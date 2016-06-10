#!/bin/bash

if [ $# -eq 0 ]; then
  echo -e "Usage: $0 <filename.c> "
  exit 1
fi

./run/build-ccomp.sh
#Expand macros
gcc -E -c $1 -o 
java -cp bin ccomp.parser_hw/CToSFDLInterpreter $1

#Cpp



