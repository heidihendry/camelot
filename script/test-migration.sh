#!/bin/sh

git clone git@gitlab.com:cshclm/camelot-testdata.git
cd camelot-testdata
tar -xvf Database-0.4.5.tar.gz
cd ../
CAMELOT_DATADIR="camelot-testdata" java -jar target/camelot.jar
