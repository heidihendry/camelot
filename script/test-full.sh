#!/bin/sh

set -e

echo "Checking binaries in \$PATH... "
which boot &> /dev/null

echo "Running tests... "
boot check

echo "Compiling... "
boot build

echo "Running camelot and ensuring it responds... "
java -jar target/camelot.jar &
PID=$!
sleep 45
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
