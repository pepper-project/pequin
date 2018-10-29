#!/bin/bash

# This script compiles and runs the public or verifier's setup phase
# of a verifiable computation.

# The first command invokes the C-to-constraints compiler and builds
# the verifier executable.

# The second command runs this executable in setup mode, which
# generates the proving and verification keys for the backend
# protocol.

# After running this script, the user must also provide inputs to the
# computation before running the prover. The verifier executable can
# create random inputs by running
# /bin/pepper_verifier_<computationname> gen_input. The input
# generation can be overridden by the user by customizing the
# appropriate file in input_generation.
if [ $# -ne 3 ] && [ $# -ne 4 ] 
then
    echo "usage: "$0 "<program name> <verification key file> <proving key file> [<unprocessed verification key file>]"
    echo ""
    echo "For example, "
    echo $0 "mm_pure_arith mm_pure_arith.vk mm_pure_arith.pk"
    exit 1
fi


make pepper_verifier_$1
echo ""
echo "=========================================="
echo "===== Running setup (key generation) ====="
echo "=========================================="
echo ""
bin/pepper_verifier_$1 setup $2 $3 $4

