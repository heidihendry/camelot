#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"
README_FILE="README.md"

echo "Bumping release version... "
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)-SNAPSHOT\"$/${PROJECT_NAME} \"\1\"/" ${PROJECT_FILE}
released_version="$(grep -oE [0-9]+\.[0-9]+\.[0-9]+ ${PROJECT_FILE} | head -n1)"
sed -i "s/${PROJECT_NAME}-\([0-9]\+\.[0-9]\+\.[0-9]\+\).jar/${PROJECT_NAME}-${released_version}.jar/" ${README_FILE}
git commit -a -m "Version bump: $released_version"
git tag -sa "v$released_version" -m "Release: $released_version"

echo "Running release build... "
lein with-profiles -dev,-user,+uberjar uberjar

echo "Packaging release"
mkdir "${PROJECT_NAME}-${released_version}/"
mv "target/${PROJECT_NAME}.jar" "${PROJECT_NAME}-${released_version}/"
cp "script/bin/"* "${PROJECT_NAME}-${released_version}/"
zip -r "${PROJECT_NAME}-${released_version}.zip" "${PROJECT_NAME}-${released_version}"

echo "Uploading release... "
scp "${PROJECT_NAME}-${released_version}.zip" "${CAMELOT_UPLOAD_TARGET}/release/"

echo "Bumping version to *-SNAPSHOT... "
patch_version=$(echo $released_version | cut -d\. -f3)
new_patch_version=$(($patch_version+1))
new_version="$(basename "${released_version}" ".${patch_version}").${new_patch_version}"
sed -i "s/${PROJECT_NAME}\s\"\([0-9]\+\.[0-9]\+\.[0-9]\+\)\"$/${PROJECT_NAME} \"${new_version}-SNAPSHOT\"/" ${PROJECT_FILE}
git commit -a -m "Version bump: ${new_version}-SNAPSHOT"

echo "Cleaning up"
mkdir -p ../releases
rm -rf "${PROJECT_NAME}-${released_version}/"
mv "${PROJECT_NAME}-${released_version}.zip" ../releases

echo "RELEASE COMPLETE"
