#!/bin/sh

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"
DOWNLOADS_URL="https://api.bitbucket.org/2.0/repositories/cshclm/camelot/downloads/"

snapshot_version="$(grep -oE "[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT" ${PROJECT_FILE} | head -n1)"
cp "target/${PROJECT_NAME}.jar" "target/$PROJECT_NAME-${snapshot_version}.jar"
curl -u ${BITBUCKET_CREDENTIALS} -X POST ${DOWNLOADS_URL} -F "files=@target/$PROJECT_NAME-${snapshot_version}.jar"
rm "target/$PROJECT_NAME-${snapshot_version}.jar"
