#!/bin/sh

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"

snapshot_version="$(grep -oE "[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT" ${PROJECT_FILE} | head -n1)"
gdrive upload "target/${PROJECT_NAME}.jar" --name "$PROJECT_NAME-${snapshot_version}-$(date '+%Y%m%d%H%M%S').jar" -p ${CAMELOT_GDRIVE_SNAPSHOT_PARENT}
