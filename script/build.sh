#!/bin/bash

which lein &> /dev/null
if [ $? -ne 0 ]; then
    if [ -z $1 ]; then
        echo "[Error] lein not found in \$PATH."
        echo "If it's not installed, please install it (http://leiningen.org/). Otherwise specify the path to lein as an argument."
        exit 1
    else
        LEIN=$1
    fi
else
    LEIN=$(which lein)
fi

if [ -e $(dirname $(dirname $0))/target/camelot.jar ]; then
    echo "camelot.jar already built"
else
    $LEIN with-profiles -dev,+production uberjar
fi
