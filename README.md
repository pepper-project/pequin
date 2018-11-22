# Pequin: An end-to-end toolchain for verifiable computation, SNARKs, and probabilistic proofs #

Pequin is a toolchain to *verifiably* execute programs expressed in (a
large subset of) the C programming language. There are two parties: a
*prover* and *verifier*. Using Pequin, the prover can convince the
verifier that it executed a given computation according to the program
expressed by the verifier. More generally, using Pequin, the prover can
convince the verifier of the truth of some assertion, without the
verifier having to manually check every line of the proof of that
assertion.

Pequin consists of a *front-end* and a *back-end*. The front-end takes C
programs and transforms them to a set of *arithmetic constraints*, in
such a way that the constraints are satisfiable if and only if the
purported output (which will be produced by the prover, and which is
represented by variables in the constraints) is what the original
program would have produced. The back-end is a probabilistic proof
protocol by which the prover convinces the verifier that the constraints
are indeed satisfiable (if they are); if the constraints are not
satisfiable, the verifier is not fooled, except with vanishingly low
probability.

The specific back-end in Pequin is a *zk-SNARK* (zero-knowledge succinct
non-interactive argument of knowledge). Pequin uses [SCIPR Lab's
`libsnark`](https://github.com/scipr-lab/libsnark), which is an
optimized implementation of the back-end of
[Pinocchio](http://research.microsoft.com/apps/pubs/default.aspx?id=180286),
itself a refinement and implementation of
[GGPR](http://eprint.iacr.org/2012/215).

Pequin is a result of several years of research in the [Pepper
project](http://www.pepper-project.org/), done at NYU and UT Austin.
Pepper has brought to bear powerful techniques from
complexity theory and cryptography: probabilistically checkable proofs
(PCPs), efficient arguments, interactive proofs (IPs) etc.
Pepper's research results include
reducing the computational costs of a PCP-based efficient argument by
over 20 orders of magnitude (in base 10!), and extending verifiability to
representative uses of cloud computing (MapReduce jobs, simple
database queries, computations involving private databases, etc.).
These results and others are published in peer-reviewed scientific
[papers](http://www.pepper-project.org/#publications).

Pepper itself has [code](https://github.com/pepper-project/pepper);
the goal of Pequin in particular is to be easier to use, and more composable. Development is ongoing. Please open issues or
submit pull requests!  (This source code is released under a BSD-style
license. See LICENSE for more details.)

## What's coming next? ##

* Incorporating other back-ends
* Improving documentation and usability of this codebase is ongoing.

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
computations! Please see [GETTINGSTARTED.md](GETTINGSTARTED.md) for a quick overview of the process.

## Docker ##
A dockerfile is provided to build a base docker image to use with projects. Just run:

```bash
./build_docker.sh
```
Pequin will be in `/opt/pequin/`, and can be addressed in your scripts using `$PEQUIN` environment variable. Pepper is at `/opt/pequin/pepper`.
## Contact ##

Please contact pepper@pepper-project.org for any questions and comments. We are happy to work with you to adapt this technology into your application.
