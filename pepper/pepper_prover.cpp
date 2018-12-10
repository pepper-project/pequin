//prover takes proving key, pws, computation inputs as input.
//generates outputs, proofs.


#include <libsnark/relations/constraint_satisfaction_problems/r1cs/r1cs.hpp>
#include <libsnark/common/default_types/r1cs_ppzksnark_pp.hpp>
#include <libsnark/zk_proof_systems/ppzksnark/r1cs_ppzksnark/r1cs_ppzksnark.hpp>

#include <iostream>
#include <fstream>

#include <gmp.h>
#include "libv/computation_p.h"

#include "common_defs.h"

#ifndef NAME
#error "Must define NAME as name of computation."
#endif

enum Mode {proof, only_setup, witness_export };

void print_usage(char* argv[]) {
    std::cout << "usage: " << std::endl <<
        "(1)" << argv[0] << " setup" << std::endl <<
        "(2) " << argv[0] << " prove <proving key file> <inputs file> <outputs file> <proof file>" << std::endl <<
        "(3) " << argv[0] << " witness <inputs file> <witness file>" << std::endl;
}

void export_witness(const char* filename, libsnark::r1cs_ppzksnark_primary_input<libsnark::default_r1cs_ppzksnark_pp> values) {
    mpz_t r; mpz_init(r);
    FILE *file = fopen(filename, "w");
    for (auto& value : values) {
        value.as_bigint().to_mpz(r);
        gmp_fprintf(file, "%Zd\n", r);
    }
    fclose(file);
}

int main (int argc, char* argv[]) {
    
    if (argc != 2 && argc != 6 && argc != 4) {
        print_usage(argv);
        exit(1);
    }

    Mode mode = Mode::proof;
    if (strcmp(argv[1], "setup") == 0) {
        mode = Mode::only_setup;
    } else if(strcmp(argv[1], "witness") == 0) {
        mode = Mode::witness_export;
    }

    struct comp_params p = parse_params("./bin/" + string(NAME) + ".params");
    std::cout << "NUMBER OF CONSTRAINTS:  " << p.n_constraints << std::endl;

    mpz_t prime;
    mpz_init_set_str(prime, "21888242871839275222246405745257275088548364400416034343698204186575808495617", 10);

    if (mode == Mode::only_setup) {
        ComputationProver prover(p.n_vars, p.n_constraints, p.n_inputs, p.n_outputs, prime, "default_shared_db", "", true);
        return 0;
    }

    std::string input_fn = string(shared_dir) + (mode == Mode::proof ? argv[3] : argv[2]);

    ComputationProver prover(p.n_vars, p.n_constraints, p.n_inputs, p.n_outputs, prime, "default_shared_db", input_fn, false);
    prover.compute_from_pws(("./bin/" + std::string(NAME) + ".pws").c_str());
    libsnark::default_r1cs_ppzksnark_pp::init_public_params();

    libsnark::r1cs_ppzksnark_primary_input<libsnark::default_r1cs_ppzksnark_pp> primary_input;
    libsnark::r1cs_ppzksnark_auxiliary_input<libsnark::default_r1cs_ppzksnark_pp> aux_input;

    for (int i = 0; i < p.n_inputs; i++) {
        FieldT currentVar(prover.input[i]);
        primary_input.push_back(currentVar);
    }

    for (int i = 0; i < p.n_outputs; i++) {
        FieldT currentVar(prover.output[i]);
        primary_input.push_back(currentVar);
    }

    for (int i = 0; i < p.n_vars; i++) {
        FieldT currentVar(prover.F1[i]);
        aux_input.push_back(currentVar);
    }

    if (mode == Mode::witness_export) {
        export_witness((string(p_dir) + argv[3] + ".primary").c_str(), primary_input);
        export_witness((string(p_dir) + argv[3] + ".aux").c_str(), aux_input);
        return 0;
    }

    std::string pk_filename = string(p_dir) + argv[2];
    std::string output_fn = string(shared_dir) + argv[4];
    std::string proof_fn = string(shared_dir) + argv[5];

    std::ifstream pkey(pk_filename);
    if (!pkey.is_open()) {
        std::cerr << "ERROR: " << pk_filename << " not found. Try running ./verifier_setup <computation> first." << std::endl;
    }

    libsnark::r1cs_ppzksnark_keypair<libsnark::default_r1cs_ppzksnark_pp> keypair;
    std::cout << "reading proving key from file..." << std::endl;
    pkey >> keypair.pk;

    libff::start_profiling();
    libsnark::r1cs_ppzksnark_proof<libsnark::default_r1cs_ppzksnark_pp> proof = libsnark::r1cs_ppzksnark_prover<libsnark::default_r1cs_ppzksnark_pp>(keypair.pk, primary_input, aux_input);

    std::ofstream proof_file(proof_fn);
    proof_file << proof; 
    proof_file.close();

    std::ofstream output_file(output_fn);
    for (int i = 0; i < p.n_outputs; i++) {
        output_file << prover.input_output_q[p.n_inputs + i] << std::endl;
    }
    output_file.close();
}
