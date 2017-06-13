//prover takes proving key, pws, computation inputs as input.
//generates outputs, proofs.


#include <libsnark/relations/constraint_satisfaction_problems/r1cs/r1cs.hpp>
#include <libsnark/common/default_types/ec_pp.hpp>
#include <libsnark/zk_proof_systems/ppzksnark/r1cs_ppzksnark/r1cs_ppzksnark.hpp>

#include <libsnark/algebra/curves/public_params.hpp>
#include <libsnark/algebra/curves/bn128/bn128_pp.hpp>
#include <libsnark/common/profiling.hpp>

#include <iostream>
#include <fstream>

#include <gmp.h>
#include "libv/computation_p.h"

#include "common_defs.h"

#ifndef NAME
#error "Must define NAME as name of computation."
#endif

void print_usage(char* argv[]) {
    std::cout << "usage: " << std::endl <<
        "(1)" << argv[0] << " setup" << std::endl <<
        "(2) " << argv[0] << " prove <proving key file> <inputs file> <outputs file> <proof file>" << std::endl;
}

int main (int argc, char* argv[]) {
    
    if (argc != 2 && argc != 6) {
        print_usage(argv);
        exit(1);
    }

    bool only_setup = false;
    if (!strcmp(argv[1], "setup")) {
        only_setup = true;
    }

    struct comp_params p = parse_params("./bin/" + string(NAME) + ".params");
    std::cout << "NUMBER OF CONSTRAINTS:  " << p.n_constraints << std::endl;

    mpz_t prime;
    mpz_init_set_str(prime, "21888242871839275222246405745257275088548364400416034343698204186575808495617", 10);

    if (only_setup) {
        ComputationProver prover(p.n_vars, p.n_constraints, p.n_inputs, p.n_outputs, prime, "default_shared_db", "", only_setup);
        return 0;
    }

    std::string pk_filename = string(p_dir) + argv[2];
    std::string input_fn = string(shared_dir) + argv[3];
    std::string output_fn = string(shared_dir) + argv[4];
    std::string proof_fn = string(shared_dir) + argv[5];

    ComputationProver prover(p.n_vars, p.n_constraints, p.n_inputs, p.n_outputs, prime, "default_shared_db", input_fn, only_setup);
    prover.compute_from_pws(("./bin/" + std::string(NAME) + ".pws").c_str());
    
    libsnark::bn128_pp::init_public_params();
    std::ifstream pkey(pk_filename);
    if (!pkey.is_open()) {
        std::cerr << "ERROR: " << pk_filename << " not found. Try running ./verifier_setup <computation> first." << std::endl;
    }

    libsnark::r1cs_ppzksnark_keypair<libsnark::bn128_pp> keypair;
    std::cout << "reading proving key from file..." << std::endl;
    pkey >> keypair.pk;
    libsnark::r1cs_ppzksnark_primary_input<libsnark::bn128_pp> primary_input;
    libsnark::r1cs_ppzksnark_auxiliary_input<libsnark::bn128_pp> aux_input;

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

    libsnark::start_profiling();
    libsnark::r1cs_ppzksnark_proof<libsnark::bn128_pp> proof = libsnark::r1cs_ppzksnark_prover<libsnark::bn128_pp>(keypair.pk, primary_input, aux_input);

    std::ofstream proof_file(proof_fn);
    proof_file << proof; 
    proof_file.close();

    std::ofstream output_file(output_fn);
    for (int i = 0; i < p.n_outputs; i++) {
        output_file << prover.input_output_q[p.n_inputs + i] << std::endl;
    }
    output_file.close();

}
