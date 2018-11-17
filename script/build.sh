#!/bin/bash

which lein &> /dev/null
if [ $? -ne 0 ]; then
    if [ -z $1 ]; then
        echo "[Error] lein not found in \$PATH."
        echo "If it's not installed, please install it (https://leiningen.org/). Otherwise specify the path to lein as an argument."
        exit 1
    else
        LEIN=$1
    fi
else
    LEIN=$(which lein)
fi

$LEIN uberjar
