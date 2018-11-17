#!/bin/sh

set -e

if [ "$1" = "clj" ]; then
    lein with-profiles +test,-dev test
elif [ "$1" = "cljs" ]; then
    lein with-profiles +test,-dev run -m figwheel.main -m camelot.test-runner
else
    lein with-profiles +test,-dev test
    lein with-profiles +test,-dev run -m figwheel.main -m camelot.test-runner
fi
