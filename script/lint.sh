#!/bin/sh

lein eastwood "{:exclude-linters [:unlimited-use] :add-linters [:unused-private-vars] :parallelism? :naive}"
