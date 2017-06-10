#!/bin/sh

which git &> /dev/null
if [ $? -ne 0 ]; then
    echo "[Error] git not found in \$PATH (maybe it's not installed?)."
fi

which boot &> /dev/null
if [ $? -ne 0 ]; then
    if [ -z $1 ]; then
        echo "[Error] boot not found in \$PATH (maybe it's not installed)."
        exit 1
    else
        BOOT=$1
    fi
else
    BOOT=$(which boot)
fi

git pull | grep "Already up-to-date" > /dev/null
$(dirname $0)/clean-build.sh

$BOOT dev
