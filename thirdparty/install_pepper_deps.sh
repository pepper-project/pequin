#!/bin/bash

set -e

DEPS_DIR=$HOME/pepper_deps
UP=`pwd`
mkdir -p $DEPS_DIR/lib/python
ln -sf $DEPS_DIR/lib $DEPS_DIR/lib64

TAR="tar xvzf"

# papi
echo "installing PAPI"
$TAR papi-5.4.1.tar.gz
cd papi-5.4.1/src
./configure --prefix=$DEPS_DIR
make
make install
cd $UP

# libconfig
echo "installing libconfig"
$TAR libconfig-1.4.8.tar.gz
cd libconfig-1.4.8
./configure --prefix=$DEPS_DIR
make
make install
cd $UP

# Cheetah template library
echo "installing Cheetah template library"
$TAR Cheetah-2.4.4.tar.gz
cd Cheetah-2.4.4
export PYTHONPATH=${DEPS_DIR}/lib/python/:${PYTHONPATH}
python setup.py install --home $DEPS_DIR
cd $UP

#Kyoto Cabinet
echo "installing Kyoto Cabinet"
$TAR kyotocabinet-1.2.76.tar.gz
cp gcc6-workaround.patch kyotocabinet-1.2.76
cd kyotocabinet-1.2.76
patch -p1 < gcc6-workaround.patch
./configure --prefix=$DEPS_DIR
make
make install
cd $UP

#leveldb
echo "installing leveldb"
$TAR leveldb-1.10.0.tar.gz
cd leveldb-1.10.0
make
cp --preserve=links libleveldb.* $DEPS_DIR/lib
cp -r include/leveldb $DEPS_DIR/include
cd $UP

#libsnark
echo "installing libsnark"
[ ! -d libsnark ] && git clone https://github.com/scipr-lab/libsnark.git
cp libsnark_compilerflag.patch libsnark
cd libsnark
git checkout dc78fdae02b437bb6c838a82f9261c49bbd7723e
git submodule init && git submodule update
git apply libsnark_compilerflag.patch
mkdir build && cd build
cmake -DCMAKE_INSTALL_PREFIX=$DEPS_DIR -DWITH_PROCPS=OFF ..
DESTDIR=$DEPS_DIR make install
cd $UP
