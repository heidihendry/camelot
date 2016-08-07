#!/bin/sh

cd "$(dirname $0)"
CAMELOT=$(ls -1 ./camelot-*.jar | head -1)
java -jar $CAMELOT --browser
