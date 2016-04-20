#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"

echo "Checking binaries in \$PATH... "
which gdrive &> /dev/null
which lein &> /dev/null
which git &> /dev/null
which sed &> /dev/null

echo "Checking working directory is clean... "
git diff --exit-code
git diff --cached --exit-code

echo "Cleaning... "
lein clean

echo "Checking code... "
lein check

echo "Compiling... "
#lein with-profiles -dev,+production compile
#lein with-profiles -dev,+uberjar cljsbuild once
# Using uberjar over compile due to bug in Lein 2.6.1
# https://github.com/technomancy/leiningen/issues/2096
lein with-profiles -dev,-user,+uberjar uberjar

echo "Running tests... "
lein with-profiles +test midje
lein doo phantom test once

echo "Running camelot and ensuring it responds... "
java -jar target/camelot.jar &
PID=$!
sleep 10
set +e
# Ensure process is still running
ps -p $PID || exit 1
# Check the compiled JS is served
curl -s http://localhost:8080/js/compiled/camelot.js > /dev/null || exit 1
# Stop process
curl -s -X POST http://localhost:8080/quit || exit 0
sleep 1;
# Ensure process is stopped
ps -p $PID > /dev/null
if [ $? -eq 0 ]; then
    echo "camelot seems to not be listening to requests. aborting."
    kill $PID
    exit 1
fi
set -e

echo "Bumping release version... "
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)-SNAPSHOT\"$/${PROJECT_NAME} \"\1\"/" ${PROJECT_FILE}
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
git commit -a -m "Version bump: $released_version"
git tag -sa "v$released_version" -m "Release: $released_version"

echo "Running release build... "
lein with-profiles -dev,-user,+uberjar uberjar

echo "Uploading release... "
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
gdrive upload "target/${PROJECT_NAME}.jar" --name "$PROJECT_NAME-${released_version}.jar" -p ${CAMELOT_GDRIVE_RELEASE_PARENT}

echo "Bumping version to *-SNAPSHOT... "
patch_version=$(echo $released_version | cut -d\. -f3)
new_patch_version=$(($patch_version+1))
new_version="$(basename "${released_version}" ".${patch_version}").${new_patch_version}"
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)\"$/${PROJECT_NAME} \"${new_version}-SNAPSHOT\"/" ${PROJECT_FILE}
git commit -a -m "Version bump: ${new_version}-SNAPSHOT"

echo "RELEASE COMPLETE"
