#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"

echo "Running tests... "
lein with-profiles +test midje
lein doo phantom test once
