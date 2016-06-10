# How to set up your environment #

A quick overview of the steps to get your toolchain to the point where
you can build the code in this directory.

## Prerequisites ##

You need a working C++11 compiler. GCC 4.7 or newer, or Clang 3.1 or
newer (I *think*; in the worst case, Clang 3.3 will certainly get
the job done).

## Build LLVM and Clang ##

You have two options here: use the pre-patched tarball in
`thirdparty/` or grab the code from LLVM and apply patches manually.

**NOTE**: both of these options assume that you are starting from
`code/compiler/buffetfsm`, the directory that contains this
README file.

### Step 1, Option 1: Use the provided tarball ###

    tar xjvf ../../thirdparty/llvm_clang-r209914-buffet_fsm_patch.tar.bz2
    # ** this will create a directory called llvm **

Continue to Step 2.

### Step 1, Option 2: Get LLVM and Clang, apply patches ###

**NOTE**: I have tested this against Clang/LLVM svn revision 209914. It
will probably work against later revisions from the 3.5 tree, so feel
free to elide the `-r 209914` from these subversion commands if you're
feeling adventurous.

    svn co -r 209914 http://llvm.org/svn/llvm-project/llvm/trunk llvm
    cd llvm/tools
    svn co -r 209914 http://llvm.org/svn/llvm-project/cfe/trunk clang
    cd clang/tools
    svn co -r 209914 http://llvm.org/svn/llvm-project/clang-tools-extra/trunk extra
    cd ../../../projects
    svn co -r 209914 http://llvm.org/svn/llvm-project/compiler-rt/trunk compiler-rt
    svn co -r 209914 http://llvm.org/svn/llvm-project/test-suite/trunk test-suite
    # ** now we will apply patches **
    cd ../tools/clang
    for i in ../../../llvm-patches/*; do patch -p0 < $i; done
    # ** get back to buffetfsm directory **
    cd ../../../

Continue to Step 2.

### Step 2: Configure LLVM and Clang ###

Once again, starting from `code/compiler/buffetfsm`

    mkdir toolchain
    mkdir llvm-build
    cd llvm-build
    $PWD/../llvm/configure --prefix=$PWD/../toolchain --enable-shared --enable-optimized
    make -j$(nproc)
    make check-all  # optional
    #  ** NOTE: no need to make install, we use libs directly from the build directory **

## Copyright Notice ##

LLVM is distributed under the University of Illinois/NCSA Open Source License:

    Copyright (c) 2003-2014 University of Illinois at Urbana-Champaign.
    All rights reserved.
    
    Developed by:
    
        LLVM Team
    
        University of Illinois at Urbana-Champaign
    
        http://llvm.org
    
    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal with
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
    of the Software, and to permit persons to whom the Software is furnished to do
    so, subject to the following conditions:
    
        * Redistributions of source code must retain the above copyright notice,
          this list of conditions and the following disclaimers.
    
        * Redistributions in binary form must reproduce the above copyright notice,
          this list of conditions and the following disclaimers in the
          documentation and/or other materials provided with the distribution.
    
        * Neither the names of the LLVM Team, University of Illinois at
          Urbana-Champaign, nor the names of its contributors may be used to
          endorse or promote products derived from this Software without specific
          prior written permission.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
    CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
    SOFTWARE.
