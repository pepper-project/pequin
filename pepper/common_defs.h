
#include <iostream>
#include <fstream>
#include <string>

constexpr auto v_dir = "./verification_material/";
constexpr auto p_dir = "./proving_material/";
constexpr auto shared_dir = "./prover_verifier_shared/";

typedef libff::Fr<libsnark::default_r1cs_ppzksnark_pp> FieldT;

struct comp_params {
    int n_constraints;
    int n_inputs;
    int n_outputs;
    int n_vars;
};

comp_params parse_params(string paramFilename) {
    std::ifstream paramFile(paramFilename);
    if (!paramFile.is_open()) {
        std::cerr << "ERROR: " << paramFilename << " not found. (Try running `make ` " << std::string(NAME) << ".params)" << std::endl;
        exit(1);
    }
    int num_constraints, num_inputs, num_outputs, num_vars;
    std::string comment;
    paramFile >> num_constraints >> comment >> num_inputs >> comment >> num_outputs >> comment >> num_vars;
    paramFile.close();

    return comp_params{num_constraints, num_inputs, num_outputs, num_vars};
}
