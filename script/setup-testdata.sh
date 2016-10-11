#!/bin/sh

set -e

git clone git@gitlab.com:camelot-project/testdata.git
cd testdata
tar -xvf Database-0.4.5.tar.gz
