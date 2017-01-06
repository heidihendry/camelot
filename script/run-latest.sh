#!/bin/sh

which git &> /dev/null
if [ $? -ne 0 ]; then
    echo "[Error] git not found in \$PATH (maybe it's not installed?)."
fi

which lein &> /dev/null
if [ $? -ne 0 ]; then
    if [ -z $1 ]; then
        echo "[Error] lein not found in \$PATH (maybe it's not installed)."
        exit 1
    else
        LEIN=$1
    fi
else
    LEIN=$(which lein)
fi

git pull | grep "Already up-to-date" > /dev/null
if [ $? -ne 0 ]; then
   $LEIN clean
fi
$(dirname $0)/build.sh

$LEIN run
