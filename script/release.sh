#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"
README_FILE="README.md"
GETTING_STARTED_DOC="doc/gettingstarted.rst"
HTML_FILE="resources/public/index.html"
BATCH_FILE="script/bin/camelot-desktop.bat"

echo "Ensuring branch is clean..."
git checkout master
git status | grep -q 'branch is up-to-date'

echo "Bumping release version... "
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)-SNAPSHOT\"$/${PROJECT_NAME} \"\1\"/" ${PROJECT_FILE}
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
sed -i "s/${PROJECT_NAME}-\([0-9]\+\.[0-9]\+\.[0-9]\+\).zip/${PROJECT_NAME}-${released_version}.zip/" ${README_FILE}
sed -i "s/\([0-9]\+\.[0-9]\+\.[0-9]\+\)\]/${released_version}\]/" ${README_FILE}
sed -i "s/${PROJECT_NAME}-\([0-9]\+\.[0-9]\+\.[0-9]\+\).zip/${PROJECT_NAME}-${released_version}.zip/" ${GETTING_STARTED_DOC}
sed -i "s/${PROJECT_NAME}-\([0-9]\+\.[0-9]\+\.[0-9]\+\).jar/${PROJECT_NAME}-${released_version}.jar/" ${BATCH_FILE}
sed -i "s/\\?v=\([0-9]\+\.[0-9]\+\.[0-9]\+\)-SNAPSHOT/?v=${released_version}/" ${HTML_FILE}
git commit -a -m "Version bump: $released_version"
git tag -sa "v$released_version" -m "Release: $released_version"

echo "Running release build... "
lein with-profiles -dev,-user,+uberjar uberjar

echo "Packaging release"
mkdir "${PROJECT_NAME}-${released_version}/"
mv "target/${PROJECT_NAME}.jar" "${PROJECT_NAME}-${released_version}/${PROJECT_NAME}-${released_version}.jar"
cp "script/bin/"* "${PROJECT_NAME}-${released_version}/"
zip -r "${PROJECT_NAME}-${released_version}.zip" "${PROJECT_NAME}-${released_version}"

echo "Uploading release... "
scp "${PROJECT_NAME}-${released_version}.zip" "${CAMELOT_UPLOAD_TARGET}/release/"

echo "Bumping version to *-SNAPSHOT... "
patch_version=$(echo $released_version | cut -d\. -f3)
new_patch_version=$(($patch_version+1))
new_version="$(basename "${released_version}" ".${patch_version}").${new_patch_version}"
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)\"$/${PROJECT_NAME} \"${new_version}-SNAPSHOT\"/" ${PROJECT_FILE}
sed -i "s/\\?v=${released_version}/?v=${new_version}-SNAPSHOT/" ${HTML_FILE}
git commit -a -m "Version bump: ${new_version}-SNAPSHOT"

echo "Cleaning up"
mkdir -p ../releases
rm -rf "${PROJECT_NAME}-${released_version}/"
mv "${PROJECT_NAME}-${released_version}.zip" ../releases

echo "RELEASE COMPLETE"
