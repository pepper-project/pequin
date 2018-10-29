#ifndef CODE_PEPPER_LIBV_COMPUTATION_P_H_
#define CODE_PEPPER_LIBV_COMPUTATION_P_H_

#include <iterator>
#include <sstream>
#include <storage/hasher.h>
#include <storage/hash_block_store.h>
#include <boost/dynamic_bitset.hpp>




#include <libv/exogenous_checker.h>

#define BUFLEN 10240



class MerkleRAM;

class ComputationProver {
protected:
    string shared_bstore_file_name;

    uint32_t *F1_index;
    int temp_stack_size;
    int num_vars, num_cons;
    mpz_t temp, temp2;
    mpz_t prime;
    mpz_t *input_output;
    mpq_t *input_q, *output_q, *temp_qs, *F1_q;
    mpq_t temp_q, temp_q2, temp_q3;
    int size_input, size_output, size_f1_vec;
    
    // folder path where blockstore is created/stored
    char bstore_file_path[BUFLEN];


    MerkleRAM* _ram;
    HashBlockStore* _blockStore;
    ExogenousChecker* exogenous_checker;

    void init_block_store();

   
    mpq_t& voc(const char*, mpq_t& use_if_constant);
    void compute_poly(FILE* pws_file, int);
    void compute_less_than_int(FILE* pws_file);
    void compute_less_than_float(FILE* pws_file);
    void compute_split_unsignedint(FILE* pws_file);
    void compute_split_int_le(FILE* pws_file);

    void compute_db_get_bits(FILE* pws_file);
    void compute_db_put_bits(FILE* pws_file);
    void compute_db_get_sibling_hash(FILE* pws_file);

    void compute_exo_compute(FILE *pws_file);
    void compute_ext_gadget(FILE *pws_file);
    void getLL(std::vector< std::vector<std::string> > &inLL, FILE *pws_file, char *buf);
    void getL (std::vector<std::string> &inL, FILE *pws_file, char *buf);

    void compute_fast_ramget(FILE* pws_file);
    void compute_fast_ramput(FILE* pws_file);

    void parse_hash(FILE* pws_file, HashBlockStore::Key& outKey, int numHashBits);
    void compute_matrix_vec_mul(FILE* pws_file);
    void compute_benes_network(FILE* pws_file);
    void compute_waksman_network(FILE* pws_file);
    void compute_get_block_by_hash(FILE* pws_file);
    void compute_put_block_by_hash(FILE* pws_file);
    void compute_free_block_by_hash(FILE* pws_file);

    void compute_genericget(FILE* pws_file);
    void compute_printf(FILE* pws_file);

  public:

    ComputationProver(int _num_vars, int _num_cons, int _size_input, int _size_output,
                      mpz_t _prime, const char *_shared_bstore_file_name, string input_file, bool only_setup);

    ~ComputationProver();
    void compute_from_pws(const char* pws_filename);

    mpz_t *input, *output, *F1;
    mpq_t *input_output_q;
};

#endif  // CODE_PEPPER_LIBV_COMPUTATION_P_H_
