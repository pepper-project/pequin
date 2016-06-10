# Getting started with the Pepper project #

This document gives a brief, high-level overview of the Pepper system. It
assumes you've already installed the prerequisites; if you haven't, see
README.md for a roadmap.

## Directory organization ##

This repository has two main subdirectories:

* `compiler` - This directory contains the front-end code; it turns source
  files in C or SFDL into sets of constraints suitable for execution by the
  back-end.

* `pepper` - This directory contains the back-end code, the driver scripts, and
  the source code that gets compiled by the front-end.


## Anatomy of a Pepper execution ##

(In this section, unless otherwise specified, directories are relative to `pepper`.)

The short version:

* Our example is matrix multiplication. The source code is in
  `apps/mm_pure_arith.c`.  Open this file in your favorite text editor
  and change value of`SIZE` on line 8 from 30 to 10 to make the
  execution faster. Then...

* To compile a C program to constraints, build the verifier and prover
  executables, and run the setup phase of the SNARK protocol, run the
  following in the pepper directory:


```
   ./pepper_compile_and_setup_V.sh mm_pure_arith mm_pure_arith.vkey mm_pure_arith.pkey
   ./pepper_compile_and_setup_P.sh mm_pure_arith
```

* The first command will create
  `verification_material/mm_pure_arith.vkey` and
  `proving_material/mm_pure_arith.pkey`. If the verifier and prover
  are running on separate machines, you'll need to copy the proving
  key to the proving machine before proceeding.

* Note that if the prover and verifier are running on the same
  machine, the C-to-constraints compiler will not be run twice!
  Additionally, only the verifier runs the setup (key generation)
  phase of the SNARK protocol.

* Now generate some inputs to the computation:
  ```
  bin/pepper_verifier_mm_pure_arith gen_input mm_pure_arith.inputs
  ```

  (This command will create
  `prover_verifier_shared/mm_pure_arith.inputs`, which the prover will
  read in the next step. If the prover and verifier are running on
  separate machines, you'll need to arrange for the files in this
  directory to be shared between them.)

* Run the prover:

```
  bin/pepper_prover_mm_pure_arith prove mm_pure_arith.pkey mm_pure_arith.inputs mm_pure_arith.outputs mm_pure_arith.proof
```

* Run the verifier

```
  bin/pepper_verifier_mm_pure_arith verify mm_pure_arith.vkey mm_pure_arith.inputs mm_pure_arith.outputs  mm_pure_arith.proof
 ```

## Some more details ##

The command line arguments to the setup scripts and prover/verifier
executables are the filenames which will either be written to or read
from depending on the phase of the protocol. Verifier-only files
(namely, the verification key) will be stored in
`verification_material`, prover-only files will be stored in
`proving_material`, and shared files (computation I/O, proof) will be
stored in `prover_verifier_shared`.


### `apps/mm_pure_arith.c` ###

This is the source code for the verified computation we wish to run. This
code is compiled into a set of constraints, whose satisfying assignment
is dettermined by the prover.

A few things to note here:

1. The function `compute()` is the entry point for the computation. It takes two
   structs, `struct In` and `struct Out`, for arguments.

2. The input and output struct definitions must be provided---they have no
   default definition.

3. Preprocessor directives are allowed here. The search path for the preprocessor
   is `../compiler/cstdinc`.

### `pepper_compile_and_setup_{V,P}.sh` ###

Each of these shell scripts first calls the C-to-constraints compiler,
and then builds verifier and prover executables in the bin/
directory. These exectuables are then run in `setup` mode.

For the verifier, this means running the key generation algorithm,
while for the prover, this is usually a no-op. The exception is for
computations with remote storage - specifically, computations which
make use of the Pantry `PutBlock` and `GetBlock` primitives. In this
case the prover must initialize a database at setup time.

### Input generator (`apps_handwritten/<computation>_v_inp_gen.h`) ###

`_v_inp_gen.h` contains a computation-specific funtion which generates
inputs to the computation, and is called by the verifier when running
in input-generation mode. A default implementation which creates
random inputs is generated from a template the first time the program
is run, but it may be customized for more complicated input generation.

The verifier writes the inputs returned by this function to a file
name you specify in the `prover_verifier_shared/` directory. If would
like to use inputs from another source, you can skip running the input
generation phase of the verifier and create this file yourself.

You will notice that the inputs are represented by an array, `input_q[]`,
while in `mm_pure_arith.c` they are represented by an arbitrary struct. The
array corresponds to a "flattened" view of the struct in the order dictated
by the struct's memory layout per the C standard.

### `bin/` ###

In addition to the prover and verifiable executables, this directory
contains the outputs of the C-to-constraints compiler, including
representations of the constraints as a QAP, and a prover worksheet
instructing the prover on how to solve these constraints. The format
of these files is documented in fileformats.txt.


## Creating a new computation ##

So, what is the process when we wish to create a new program? Really, it's
pretty simple:

1. Write the program, storing it in `apps/<program>.c`.

2. Run the compilation and setup scripts:
   ```
   ./pepper_compile_and_setup_V.sh <program> <verification key filename> <proving key filename>
   ./pepper_compile_and_setup_P.sh <program>
   ```
3. (optional) Customize the input generation function in
`apps_handwritten/` and recompile the verifier binary.

4. `bin/pepper_verifier_<program> gen_input <input filename>` (or create your own inputs)

5. ```bin/pepper_prover_<program> prove <proving key filename> <input filename> <output filename> <proof filename>```

6. `bin/pepper_verifier_<program> verify <verification key filename> <input filename> <output filename> <proof filename>`

## Advanced features ##

### Pantry ###

If you are using the Pantry `PutBlock` and `GetBlock` primitives, there are
a couple extra requirements for your program:

* You need to `#include <db.h>` in `program.c`.

* You need to tell the verifier to initialize the blockstore. For an
  example of how to do this, see
  `apps_handwritten/ramput_micro_inp_gen.h` or
  `apps_handwritten/ptrchase_merkle_inp_gen.h`.

### Buffet ###

If you want to run a computation with data-dependent loops, make sure
to compile the llvm libraries needed for Buffet's C-to-C compiler
first. (Run `install_buffet.sh`, or see `compiler/buffetsm/README.md`
for more details.)

After you build the libraries, the compiler will be built and invoked
automatically when compiling computations that include the
[[buffet::fsm()]] compiler directive. See `apps/rle_decode_flat.c` for
an example.)


### `exo_compute()` ###

`exo_compute()` is a function which may be used in verifiable
compuations to inject arbitrary advice into the prover. See
exo_compute.txt for more information.


* NOTE: Not all of the examples in `apps/` have been tested in this
  release.

Known working ones include:

`mm_pure_arith, pam_clustering, fannkuch, ptrchase_{benes,merkle},
mergesort_{benes,merkle}, boyer_occur_{benes,merkle}, kmpsearch,
kmpsearch_flat, rle_decode, rle_decode_flat, sparse_matvec,
sparse_matvec_flat.`

Most others should work as well, but may require porting the
verifier's input generation function from the main release.