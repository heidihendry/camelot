#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"
DOWNLOADS_URL="https://api.bitbucket.org/2.0/repositories/cshclm/camelot/downloads/"

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
# Ensure process is still running
ps -p $PID
# Check the compiled JS is served
curl -s http://localhost:8080/js/compiled/camelot.js > /dev/null
# Stop process
curl -s -X POST http://localhost:8080/quit || true
# Ensure process is stopped
set +e
ps -p $PID > /dev/null
if [ $? -eq 0 ]; then
    set -e
    echo "camelot seems to not be listening to requests. aborting."
    kill $PID
    false
else
    echo "camelot is responsive."
    set -e
fi

echo "Bumping release version... "
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)-SNAPSHOT\"$/${PROJECT_NAME} \"\1\"/" ${PROJECT_FILE}
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
git commit -a -m "Version bump: $released_version"
git tag -sa "v$released_version" -m "Release: $released_version"

echo "Running release build... "
lein with-profiles -dev,-user,+uberjar uberjar

echo "Uploading release... "
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
echo "To Google Drive... "
gdrive upload "target/${PROJECT_NAME}.jar" --name "$PROJECT_NAME-${released_version}.jar" -p ${CAMELOT_GDRIVE_RELEASE_PARENT}
echo "To Bitbucket... "
mv "target/${PROJECT_NAME}.jar" "target/$PROJECT_NAME-${released_version}.jar"
curl -v -u ${BITBUCKET_CREDENTIALS} -X POST ${DOWNLOADS_URL} -F "files=@target/$PROJECT_NAME-${released_version}.jar"

echo "Bumping version to *-SNAPSHOT... "
patch_version=$(echo $released_version | cut -d\. -f3)
new_patch_version=$(($patch_version+1))
new_version="$(basename "${released_version}" ".${patch_version}").${new_patch_version}"
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)\"$/${PROJECT_NAME} \"${new_version}-SNAPSHOT\"/" ${PROJECT_FILE}
git commit -a -m "Version bump: ${new_version}-SNAPSHOT"

echo "RELEASE COMPLETE"
