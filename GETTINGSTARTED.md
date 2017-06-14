# Getting started with the Pepper project #

This document gives an overview of the Pepper system. It
assumes you've already installed the prerequisites; if you haven't, see
README.md for a roadmap.

## Directory organization ##

This repository has two main subdirectories:

* `compiler` - This directory contains the front-end code; it turns source
  files in C or SFDL into sets of constraints suitable for execution by the
  back-end.

* `pepper` - This directory contains the back-end code, the driver scripts, and
  the source code that gets compiled by the front-end.

## Summary ##

1. Write your program, storing it in `apps/<program>.c`.

2. Follow the steps in the tutorial below, running the compilation and setup
   scripts for the prover and verifier, followed by input generation,
   proving, and verification.

3. (optional) Customize the input generation function in
   `input_generation/` and recompile the verifier binary, using the
   provided compilation script.

## Tutorial and anatomy ##

(In this section, unless otherwise specified, directories are relative to `pepper`.)

* Our example is matrix multiplication, which is in 
  `apps/mm_pure_arith.c`.  Open this file in your favorite text editor
  and change value of`SIZE` on line 8 from 30 to 10 to make the
  execution faster. Then...

* Next, we want to compile a C program to constraints, build the
  verifier and prover
  executables, and run the setup phase of the probabilistic proof
  protocol (which in this case is a SNARK, a kind of non-interactive
  argument). To this end, run the following in the pepper directory:


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

Below, we detail each of the steps above.

### `apps/mm_pure_arith.c` ###

This is the source code for the verified computation we wish to run. This
code is compiled into a set of constraints, whose satisfying assignment
is determined by the prover.

A few things to note here:

1. The function `compute()` is the entry point for the computation. It takes two
   structs, `struct In` and `struct Out`, for arguments.

2. The input and output struct definitions must be provided---they have no
   default definition.

3. Preprocessor directives are allowed here. The search path for the preprocessor
   is `../compiler/cstdinc`.

### `pepper_compile_and_setup_{V,P}.sh` ###

* Each of these shell scripts first calls the C-to-constraints compiler,
and then builds verifier and prover executables in the `bin/`
directory. These exectuables are then run in `setup` mode.

* For the verifier, this means running the key generation
algorithm. Thus, the verifier setup script takes two arguments:
filenames for the verification and proving keys, which are written to
the `verification_material` and `proving_material` directories,
respectively.

* For the prover, setup is usually a no-op. The exception is for
computations with remote storage - specifically, computations which
make use of the Pantry `PutBlock` and `GetBlock` primitives. In this
case the prover must initialize a database at setup time.

### Input generation (`input_generation/<computation>_v_inp_gen.h`) ###

* Before running the prover, you must create inputs to the computation
of interest. This can be done either by running the verifier in "input
generation" mode, or by creating an inputs file yourself. To run the
verifier in input generation mode, type:

```
  bin/pepper_verifier_<program> gen_input <input filename>
```

* Note that `<computation>_v_inp_gen.h` contains a computation-specific
funtion which generates inputs to the computation, and is called by
the verifier when running in input-generation mode. A default
implementation which creates random inputs is generated from a
template the first time the program is run, but it may be customized
for more complicated input generation.

* The verifier writes the inputs returned by this function to a file
name you specify in the `prover_verifier_shared/` directory. If you
would like to use inputs from another source, you can skip running the
input generation phase of the verifier and create this file yourself.

* You will notice that the inputs are represented by an array, `input_q[]`,
while in `mm_pure_arith.c` they are represented by an arbitrary struct. The
array corresponds to a "flattened" view of the struct in the order dictated
by the struct's memory layout per the C standard.

### Running the prover ###

* To run a prover, type:

```
bin/pepper_prover_<program> prove <proving key filename> <input filename> <output filename> <proof filename>
```

 * The prover will look for (or create, in the case of outputs and
 proofs) the filenames you specify in `proving_material` (for the
 proving key), and `prover_verifier_shared` (for inputs, outputs and
 proof).

### Running the verifier ###

 * To verify the results of a computation, type:

```
bin/pepper_verifier_<program> verify <verification key filename> <input filename> <output filename> <proof filename>
```

 * The verifier will look for the filenames you specify in
`verification_material` (for the verification key), and
`prover_verifier_shared` (for inputs, outputs, and proof).

### `bin/` ###

 * In addition to the prover and verifiable executables, this
directory contains the outputs of the C-to-constraints compiler,
including a low-level representation of the constraints as a
*quadratic arithmetic program* (QAP), and a prover worksheet
instructing the prover on how to solve these constraints.

* The format of these files is documented in fileformats.txt.

## Advanced features ##

### Private prover input ###

Pepper allows the prover to take inputs which are hidden from the
verifier.

There are actually a couple of ways to do this. The most general is
using the exo_compute function in your verifiable computation. You
call it like any other C function, but the compiler treats it
specially: when the prover is executing the program and solving the
constraints, it will execute a user-provided program as a child
process. This program can accept any input from the prover and return
any output.

As a simple example, if you just want to have the prover take a fixed
private key of some kind, the user-provided program can be a shell
script that takes no input and echoes the data you want the prover to
have.

See `exo_compute.txt` for more information.

Another option is using the commitment primitives (`hashget()` and
`commitmentget()`), which allow the prover to provide the verifier
with a commitment to some private state. This allows for more
fine-grained control over what the prover's private inputs are allowed
to be, but it does come with additional
costs. https://eprint.iacr.org/2013/356.pdf, Section 6 provides the
theoretical details. See `apps/genome_snp_freq.c` or `apps/tolling.c`
for code examples.

### Pantry ###

If you are using the Pantry `PutBlock` and `GetBlock` primitives, there are
a couple extra requirements for your program:

* You need to `#include <db.h>` in `program.c`.

* You need to tell the verifier to initialize the blockstore. For an
  example of how to do this, see
  `input_generation/ramput_micro_inp_gen.h` or
  `input_generation/ptrchase_merkle_inp_gen.h`.

### Buffet ###

 * If you want to run a computation with data-dependent loops, make sure
to compile the llvm libraries needed for Buffet's C-to-C compiler
first. (Run `install_buffet.sh`, or see `compiler/buffetsm/README.md`
for more details.)

 * After you build the libraries, the compiler will be built and invoked
automatically when compiling computations that include the
`[[buffet::fsm()]]` compiler directive. See `apps/rle_decode_flat.c` for
an example.)

## Examples ##

* Note, not all of the examples in `apps/` have been tested in this
  release.

Known working ones include:

`mm_pure_arith, pam_clustering, fannkuch, ptrchase_{benes,merkle},
mergesort_{benes,merkle}, boyer_occur_{benes,merkle}, kmpsearch,
kmpsearch_flat, rle_decode, rle_decode_flat, sparse_matvec,
sparse_matvec_flat.`

Most others should work as well, but may require porting the
verifier's input generation function from the main release.
