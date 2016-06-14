#!/bin/bash

# This script compiles and sets up a prover executable for a
# verifiable computation.

# The first command invokes the C-to-constraints compiler and builds
# the prover executable. (Although, the C-to-constraints compiler will
# not actually be run twice if the verifier setup script is run
# first.)

# The second command runs this executable in setup mode, which for the
# prover is only used to initialize any remote storage (see, and is
# otherwise a null-op.

# After running this script, the user must also provide inputs to the
# computation before running the prover. Inputs are read from the file
# prover_verifier_shared/<computation>.inputs. The verifier executable
# can create random inputs and write them to this file by running
# /bin/pepper_verifier_<computationname> gen_input. The input
# generation can be overridden by the user by customizing the
# appropriate file in input_generation.
if [ $# -ne 1 ] 
then
    echo "usage: "$0 "<program name>"
    echo ""
    echo "For example, "
    echo $0 "mm_pure_arith"
    exit 1
fi
make pepper_prover_$1
bin/pepper_prover_$1 setup

