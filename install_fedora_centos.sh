#!/bin/bash
set -e
sudo yum groupinstall 'Development Tools'
sudo yum install python python-devel java-1.8.0-openjdk-devel golang gcc   \
        gcc-c++ glibc-devel libstdc++ glibc-devel.i686 libstdc++.i686 ant \
        make time boost boost-devel gmp gmp-devel zlib zlib-devel \
        openssl-devel cmake

cd thirdparty
./install_pepper_deps.sh
 
