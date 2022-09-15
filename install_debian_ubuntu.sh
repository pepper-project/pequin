#!/bin/bash

set -e

PACKAGES="python2.7 libpython2.7 libpython2.7-dev golang \
          build-essential gcc g++ gcc-multilib g++-multilib ant \
          ant-optional make time libboost-all-dev libgmp10 libgmp-dev \
          zlib1g zlib1g-dev libssl-dev cmake pkg-config"

if [[ $(apt-cache search openjdk-8-jdk) ]]; then
    PACKAGES+=" openjdk-8-jdk"

else
    PACKAGES+=" openjdk-7-jdk"
fi

sudo apt-get install $PACKAGES

cd thirdparty
./install_pepper_deps.sh
 
