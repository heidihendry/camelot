#!/bin/sh

set -e

echo "Running camelot and ensuring it responds... "

if [ -n "$CAMELOT_DATADIR" ]; then
    echo "Using custom datadir: ${CAMELOT_DATADIR}"
else
    echo "Using default datadir"
fi

java -jar target/camelot.jar &
PID=$!
failed=1
i=0
expected_db="$(ls -1 resources/migrations | tail -n1 | grep -ioE "^[-a-z0-9_]+")"
while [ $i -lt 20 ]; do
    sleep 5
    status="$(curl -s -X GET 'http://localhost:5341/heartbeat' || true)"
    set +e
    echo $status | grep -q "\"status\": \"OK\""
    if [ $? -eq 0 ]; then
        actual_db="$(echo -e $status | grep -oE "Database version: [-a-z0-9_]+" | cut -d\: -f2 | tr -d ' ')"
        if [ "${actual_db}"="${expected_db}" ]; then
            echo "Found expected database version: $actual_db"
            failed=0
        else
            echo "Database version is $actual_db, but $expected_db expected"
        fi
        break
    else
        i=$(($i+1))
    fi
    set -e
done

set -e

if [ $i -gt 20 ]; then
   echo "Camelot was not reachable after 2 minutes. aborting."
else
    curl -s -X POST http://localhost:5341/quit || true
fi

set +e
sleep 5
ps -p $PID > /dev/null
if [ $? -eq 0 ]; then
    echo "Process had not stopped. Sending SIGKILL."
    kill -9 $PID
fi

if [ $failed -ne 0 ]; then
    exit 1
fi
