#!/bin/bash

which boot &> /dev/null
if [ $? -ne 0 ]; then
    if [ -z $1 ]; then
        echo "[Error] boot not found in \$PATH."
        echo "If it's not installed, please install it (http://boot-clj.com/). Otherwise specify the path to boot as an argument."
        exit 1
    else
        BOOT=$1
    fi
else
    BOOT=$(which boot)
fi

if [ -e $(dirname $(dirname $0))/target/camelot.jar ]; then
    echo "camelot.jar already built"
else
    $BOOT uberjar
fi
