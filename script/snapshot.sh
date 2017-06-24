#!/bin/sh

set -e

PROJECT_NAME="camelot"
BUILD_FILE="build.boot"

echo "Running snapshot build... "
boot uberjar

snapshot_version="$(grep -oE "[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT" ${BUILD_FILE} | head -n1)"
cp "target/${PROJECT_NAME}.jar" "target/$PROJECT_NAME-${snapshot_version}.jar"

echo "Uploading snapshot... "
scp "target/$PROJECT_NAME-${snapshot_version}.jar" "${CAMELOT_UPLOAD_TARGET}/snapshot/"

if [ -n $CAMELOT_UPLOAD_HOST ]; then
    echo "${CAMELOT_UPLOAD_HOST}/snapshot/$PROJECT_NAME-${snapshot_version}.jar"
fi
