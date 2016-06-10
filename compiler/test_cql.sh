#! /bin/bash

path=$1
filename=$(basename "$path")
classname="${filename%.*}" #todo - something fancier?

./zcc --cql --cqltest -d . -f ${path} -t zaatar

gcc -I./cql/ ${filename}_nocql.c -o ${classname}
