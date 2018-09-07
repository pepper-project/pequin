#!/bin/bash
set -e
sudo pacman -S python python2 jdk7-openjdk go base-devel multilib-devel    \
            libstdc++5 lib32-glibc lib32-libstdc++5 lib32-gcc-libs         \
            gcc-libs-multilib libtool-multilib gcc-multilib apache-ant     \
            make time boost boost-libs gmp zlib cmake


cd thirdparty
./install_pepper_deps.sh
 
