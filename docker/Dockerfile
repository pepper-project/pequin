FROM debian:jessie

ENV PEQUIN /opt/pequin
RUN set -ex \
    && buildDeps=" \
    python2.7 libpython2.7 libpython2.7-dev openjdk-7-jdk \
    golang build-essential gcc g++ gcc-multilib g++-multilib ant  \
    ant-optional make time libboost-all-dev libgmp10 libgmp-dev   \
    zlib1g zlib1g-dev libssl-dev cmake git pkg-config \
    " \
    && apt-get update \
    && apt-get install -y --no-install-recommends $buildDeps \
    && rm -rf /var/lib/apt/lists/*

COPY . $PEQUIN
RUN cd $PEQUIN/thirdparty && ./install_pepper_deps.sh \
    && cd $PEQUIN && ./install_buffet.sh \
    && mv $PEQUIN/thirdparty/libsnark /tmp && rm -rf $PEQUIN/thirdparty/* \
    && mv /tmp/libsnark $PEQUIN/thirdparty/ \
    && rm -rf $PEQUIN/compiler/buffetfsm/llvm $PEQUIN/compiler/buffetfsm/llvm-build