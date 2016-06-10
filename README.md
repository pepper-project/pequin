# Pequin: tools for verifiable computation #

This is a simplified release of (a subset of) the original [Pepper
codebase](https://github.com/pepper-project/pepper).

It includes a C compiler targeting *arithmetic constraints*, and an
interface for running verifiable outsourced computations using
*SNARKs* - succinct non-interactive arguments of knowledge, a
cryptographic protocol which allows one to produce a proof that a
computation was executed correctly.

The goal of this release is to make the Pepper compiler easier to use
and compose with other work. Development is ongoing. Please feel free
to open issues or submit pull requests!

For more information on the related publications and the research
effort in general, please visit the main [Pepper
website.](http://www.pepper-project.org/)

## What's new in this release ##

The C-to-constraints compiler is relatively unchanged from the main
release, but the multitude of backend verifiable computation protocols
have been replaced with a few simple driver programs which read the
outputs of the compiler and run the proving and verification
algorithms using [SCIPR Lab's
`libsnark`](https://github.com/scipr-lab/libsnark), which is an
optimized implementation of
the backend of [Pinocchio](http://research.microsoft.com/apps/pubs/default.aspx?id=180286),
itself a refinement and implementation of [GGPR](http://eprint.iacr.org/2012/215).

Work to incorporate other protocols for verifiable computation and to
improve documentation and usability of this codebase is ongoing.

## Installation and first steps ##

1. This codebase depends on several external libraries. Installation
   scripts are provided for a few common linux distros. For example,
   on Ubuntu 14.04 or Debian Jessie,
   ```
   ./install_debian_ubuntu.sh
   ```
   will install some packages from apt repositories and compile our
   snapshots of some third-party dependencies. (You will be
   prompted to enter your password to install the apt packages as
   sudo.)
   
2. `./install_buffet.sh` will build the patched Clang/LLVM libraries for
   Buffet's C-to-C compiler, needed for running computations with
   data-dependent loops and control flow. 

For more information on setting up dependencies (for example, for
other distros), see [INSTALLING.md](INSTALLING.md) and [compiler/buffetfsm/README.md](compiler/buffetfsm/README.md).

Once everything is set up, you're ready to run some verifiable
computations! Please have a look at [GETTINGSTARTED.md](GETTINGSTARTED.md) for a quick
overview of the process.

This source code is released under a BSD-style license. See LICENSE
for more details.

## Contact ##

Please contact pepper@nyu.systems for any questions and comments.

