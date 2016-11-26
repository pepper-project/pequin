#!/bin/bash
set -e
cd compiler/buffetfsm/
tar xjvf ../../thirdparty/llvm_clang-r209914-patched-buffet_fsm_patch.tar.bz2
mkdir toolchain
mkdir llvm-build
cd llvm-build
$PWD/../llvm/configure --prefix=$PWD/../toolchain --enable-shared --enable-optimized
make -j$(nproc)
