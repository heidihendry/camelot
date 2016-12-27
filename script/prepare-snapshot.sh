#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"

snapshot_version="$(grep -oE "[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT" ${PROJECT_FILE} | head -n1)"
cp "target/${PROJECT_NAME}.jar" "target/$PROJECT_NAME-${snapshot_version}.jar"
