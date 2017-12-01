#!/bin/bash
set -e
cd compiler/buffetfsm/
tar xjvf ../../thirdparty/llvm_clang-r319532-patched-buffet_fsm_patch.tar.bz2
mkdir toolchain
mkdir llvm-build
cd llvm-build
cmake -DCMAKE_INSTALL_PREFIX=$PWD/../toolchain -DBUILD_SHARED_LIBS=True -DLLVM_OPTIMIZED_TABLEGEN=True $PWD/../llvm/
make -j$(nproc)
