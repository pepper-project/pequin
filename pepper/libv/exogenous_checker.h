#ifndef CODE_PEPPER_LIBV_EXOGENOUS_CHECKER_H_
#define CODE_PEPPER_LIBV_EXOGENOUS_CHECKER_H_

#include <common/utility.h>
#include <storage/hash_block_store.h>
#include <storage/merkle_ram.h>

class ExogenousChecker {
  protected:
    HashBlockStore *_bs;
    MerkleRAM *_ram;
  public:
    ExogenousChecker();
    virtual ~ExogenousChecker() {}
    void set_block_store(HashBlockStore* block_store, MerkleRAM* ram);
    virtual bool exogenous_check(const mpz_t* input, const mpq_t* input_q,
        int num_inputs, const mpz_t* output, const mpq_t* output_q,
        int num_outputs, mpz_t prime) = 0;
    virtual void baseline(const mpq_t* input_q, int num_inputs,
        mpq_t* output_recomputed, int num_outputs) = 0;
    virtual void init_exo_inputs(const mpq_t *, int, char *, HashBlockStore *);
    virtual void export_exo_inputs(const mpq_t *, int, char *, HashBlockStore *);
    virtual void run_shuffle_phase(char*);

    //Minimal baseline computation using native types. 
    size_t baseline_minimal_input_size, baseline_minimal_output_size;
    virtual void baseline_minimal(void* input, void* output);
};
#endif  // CODE_PEPPER_LIBV_EXOGENOUS_CHECKER_H_
