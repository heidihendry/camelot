#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"

echo -n "Checking binaries in \$PATH... "
which gdrive &> /dev/null
which lein &> /dev/null
which git &> /dev/null
which sed &> /dev/null
echo "done"

echo -n "Checking working directory is clean... "
git diff --exit-code
git diff --cached --exit-code
echo "done"

echo -n "Cleaning... "
lein clean
echo "done"

echo -n "Checking code... "
lein check
echo "done"

echo -n "Compiling... "
lein with-profiles -dev,+production compile
lein with-profiles -dev,+uberjar cljsbuild once
echo "done"

echo -n "Running tests... "
lein with-profiles +test midje
lein with-profiles +test,+uberjar cljsbuild test
echo "done"

echo -n "Bumping to release version... "
sed -i 's/\s"\([0-9]\+\.[0-9]\+\.[0-9]\+\)-SNAPSHOT"$/ "\1"/' ${PROJECT_FILE}
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
git commit -a -m "Version bump: $released_version"
git tag -sa "v" -m "Release: $released_version"
echo "done"

echo -n "Running release build... "
lein with-profiles -dev,-user,+uberjar uberjar
echo "done"

echo -n "Uploading release... "
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
gdrive upload "target/${PROJECT_NAME}.jar" --name "$PROJECT_NAME-${released_version}.jar" -p ${CAMELOT_GDRIVE_RELEASE_PARENT}
echo "done"

echo -n "Bumping version to *-SNAPSHOT... "
patch_version=$(echo $released_version | cut -d\. -f3)
new_patch_version=$(($patch_version+1))
new_version="$(basename "${released_version}" ".${patch_version}").${new_patch_version}"
sed -i "s/\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)\"$/ \"${new_version}-SNAPSHOT\"/" ${PROJECT_FILE}
git commit -a -m "Version bump: ${new_version}-SNAPSHOT"
echo "done"

echo "RELEASE COMPLETE"
