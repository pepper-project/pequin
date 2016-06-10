#!/bin/bash

sudo apt-get install python2.7 libpython2.7 libpython2.7-dev openjdk-7-jdk \
            golang build-essential gcc g++ gcc-multilib g++-multilib ant  \
            ant-optional make time libboost-all-dev libgmp10 libgmp-dev   \
            zlib1g zlib1g-dev

cd thirdparty
./install_pepper_deps.sh
 
