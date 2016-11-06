#!/bin/sh

set -e

PROJECT_NAME="camelot"
PROJECT_FILE="project.clj"

echo "Checking binaries in \$PATH... "
which gdrive &> /dev/null
which lein &> /dev/null
which git &> /dev/null
which sed &> /dev/null

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
lein with-profiles +test test
lein doo phantom test once

echo "Running camelot and ensuring it responds... "
java -jar target/camelot.jar &
PID=$!
sleep 20
# Ensure process is still running
ps -p $PID
# Check the compiled JS is served
curl -s http://localhost:5341/js/compiled/camelot.js > /dev/null
# Stop process
curl -s -X POST http://localhost:5341/quit || true
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

echo "TEST COMPLETE"
