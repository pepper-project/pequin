#include<libv/exogenous_checker.h>

ExogenousChecker::ExogenousChecker() {
  baseline_minimal_input_size = 0;
  baseline_minimal_output_size = 0;
}

void ExogenousChecker::set_block_store(HashBlockStore* block_store, MerkleRAM* ram) {
  _bs = block_store;
  _ram = ram;
  setBlockStoreAndRAM(block_store, ram);
}

void ExogenousChecker::init_exo_inputs(const mpq_t *, int, char *, HashBlockStore *) {
  // empty implementation, but virtual; so computations that need
  // exogeneous inputs in their block store, should implement this
  // function
}

void ExogenousChecker::export_exo_inputs(const mpq_t *, int, char *, HashBlockStore *) {
  // another empty implementation, and again virtual
}

void ExogenousChecker::run_shuffle_phase(char *) {
  //Empty implementation, and again virtual
}

void ExogenousChecker::baseline_minimal(void* input, void* output){
  //Empty implementation, and again virtual
}
