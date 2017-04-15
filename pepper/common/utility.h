#ifndef CODE_PEPPER_COMMON_UTILITY_H_  
#define CODE_PEPPER_COMMON_UTILITY_H_  

#include <string>
#include <dirent.h>
#include <iostream>
#include <stdlib.h>
#include <string.h>
#include <gmp.h>
#include <time.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <stdint.h>
#include <algorithm>
#include <list>
#include <map>
#include <vector>
#include <string>
#include <math.h>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <storage/exo.h>
#include <common/sha1.h>
#include <common/sha256.h>

using std::cerr;
using std::endl;


#define GEN_SIGNED_INPUTS 1

//Protocols
#define GINGER 0
#define ZAATAR 1
#define GGPR12 2
#define PINOCCHIO 3
#define PINOCCHIO_ZK 4

#define PROTOCOL PINOCCHIO_ZK

//USE_LIBSNARK is set in the flags file. The protocol is then set to PINOCCHIO_ZK.
#if defined(USE_LIBSNARK)
  #define PROTOCOL PINOCCHIO_ZK
  #define NONINTERACTIVE 1
  #define GGPR 0
  #define FAST_FOURIER_INTERPOLATION 0
  #define PUBLIC_VERIFIER 0
#elif PROTOCOL == GINGER
  #define NONINTERACTIVE 0
  #define FAST_FOURIER_INTERPOLATION 0
#elif PROTOCOL == ZAATAR
  #define NONINTERACTIVE 0
  #define FAST_FOURIER_INTERPOLATION 1
#elif PROTOCOL == GGPR12
  #define NONINTERACTIVE 1
  #define GGPR 1
  #define FAST_FOURIER_INTERPOLATION 0
#elif PROTOCOL == PINOCCHIO
  #define NONINTERACTIVE 1
  #define GGPR 0
  #define FAST_FOURIER_INTERPOLATION 0
  #define PUBLIC_VERIFIER 0 
#elif PROTOCOL == PINOCCHIO_ZK
  #define NONINTERACTIVE 1
  #define GGPR 0
  #define FAST_FOURIER_INTERPOLATION 0
  #define PUBLIC_VERIFIER 0
#endif





#define INTERFACE_MPI
#define FOLDER_STATE_SHARED 1

//#define DEBUG_MALICIOUS_PROVER

#define BUFLEN 10240
#define PAGESIZE 0x2000
#define INIT_MPZ_BITS 128 
#define NUM_THREADS 1

#define SERVICE_UPLOAD_NAME "/upload.php"
#define SERVICE_DOWNLOAD_NAME "/download.php"

#define NUM_OF(array) (sizeof(array) / sizeof(*array))

#ifdef ON_TACC
#define FOLDER_STATE SCRATCH_FOLDER "/computation_state"
#define FOLDER_PERSIST_STATE SCRATCH_FOLDER "/persist_state"
#define FOLDER_TMP SCRATCH_FOLDER "/tmp_state"
#else
#ifndef FOLDER_STATE
#define FOLDER_STATE "./prover_verifier_shared"
#endif
#endif
#define ROOT_FOLDER_BLOCK_STORES "block_stores"

extern bool MICROBENCHMARKS;

//using namespace std;
using std::cout;
using std::endl;
using std::string;
using std::vector;
using std::pair;
using std::fstream;
using std::ifstream;
using std::ofstream;
using std::stringstream;

#define DEFAULT_DIRECTORY_PERMISSION ((S_IRWXU | S_IRWXO | S_IRWXG) & ~(S_IWOTH | S_IWGRP))

// COMMON UTILITIES
void parse_args(int argc, char **argv, char *role, int *phase, int *batch_size, int
  *num_verifications, int *input_size, char *prover_url);

void parse_args(int argc, char **argv, char *role, int *phase, int *batch_size, int
  *num_verifications, int *input_size, char *prover_url, int *generate_states);

void parse_args(int argc, char **argv, char *role, int *phase, int *batch_size, int
  *num_verifications, int *input_size, char *prover_url, int *generate_states, char *shared_bstore_file_path);

void parse_http_args(char *query_string, int *phase, int *batch_size,
  int *batch_start, int *batch_end, int *num_verifications, int *input_size);

bool recursive_mkdir(const string& dir, mode_t mode = DEFAULT_DIRECTORY_PERMISSION);

bool open_file(FILE **fp, const char *vec_name, const char *permission,
  const char *folder_name);

long long int stat_size(const char* filename, const char*folder_name = NULL);

void open_file_update(fstream& fp, const char* name, const char* folder_name);

void open_file_read(ifstream& fp, const char* name, const char*folder_name);

void open_file_write(ofstream& fp, const char* name, const char*folder_name);

off_t get_file_size(const char *filename);

void convert_to_z(const int size, mpz_t *z, const mpq_t *q, const mpz_t prime);
void convert_to_z(mpz_t z, const mpq_t q, const mpz_t prime);

void load_prng_seed(int bytes_key, void *key, int bytes_iv, void *iv);

void dump_prng_seed(int bytes_key, void *key, int bytes_iv, void *iv);

void dump_vector(int size, char *arr, const char *vec_name, const char *folder_name); 
bool dump_vector(int size, const mpz_t *q, const char *vec_name, const char
  *folder_name = NULL);
void dump_vector_interleaved(int size, const mpz_t *q, const char *vec_name, const char
  *folder_name = NULL);
bool dump_vector(int size, const mpq_t *q, const char *vec_name, const char
  *folder_name = NULL);

void dump_binary_nums(int size, const mpq_t *q, const char *vec_name, const char *folder_name = NULL);

void dump_scalar(const mpz_t q, char *scalar_name, const char *folder_name = NULL);

void dump_scalar_array(int n, const mpz_t *scalars, const char *suffix, char
  *folder_name = NULL);

std::list<string> get_files_in_dir(char *dir_name);

void load_scalar_from_fp(FILE *fp, mpz_t s);
void dump_scalar_to_fp(FILE *fp, mpz_t s); 

void load_vector(int size, mpz_t *q, const char *vec_name, const char
  *folder_name = NULL);
void load_vector(int size, uint32_t *vec, const char *full_file_name);
void load_vector(int size, mpq_t *q, const char *vec_name, const char
  *folder_name = NULL);
void load_vector(int size, char *q, const char *vec_name, const char *folder_name); 

void load_scalar(mpz_t q, const char *scalar_name, const char *folder_name = NULL);

void load_scalar_array(int n, mpz_t *scalars, const char *suffix, char
  *folder_name = NULL);

void load_txt_scalar(mpz_t q, const char *scalar_name, const char *folder_name = NULL);

void clear_scalar(mpz_t);
void clear_scalar(mpq_t);
void clear_vec(int size, mpz_t *arr);
void clear_vec(int size, mpq_t *arr);

void alloc_init_vec(mpz_t **arr, uint32_t size);
void alloc_init_vec(mpq_t **arr, uint32_t size);
void alloc_init_vec_array(mpz_t ***array, const uint32_t n, const uint32_t vecSize);

void alloc_init_scalar(mpz_t s);
void alloc_init_scalar(mpq_t s);

bool modIfNeeded(mpz_t val, const mpz_t prime, int scale = 2);
bool modIfNeeded(mpq_t val, const mpz_t prime, int scale = 2);

void toTrueNumber(mpz_t a, const mpz_t halfPrime, const mpz_t prime);
void toTrueNumber(mpq_t a, const mpz_t halfPrime, const mpz_t prime);

void clear_del_vec(mpz_t* vec, const uint32_t n);
void clear_del_vec_array(mpz_t **array, const uint32_t n, const uint32_t vecSize);
void clear_del_vec(mpq_t* vec, const uint32_t n);

void print_matrix(mpz_t *matrix, uint32_t num_rows, uint32_t num_cols,
  string name = "");

void print_sq_matrix(mpz_t *matrix, uint32_t size, string name = "");

void* aligned_malloc(size_t size);

bool verify_conversion_to_z(size_t size, mpz_t *z, mpq_t *q, mpz_t prime);

//void safe_element_set(element_t a, element_t b);

// attempt at a fast realloc2()
// mostly just does what the gmp code does
// assumes number not larger than gmp can handl
inline void fast_mpz_realloc2(mpz_t m, int bits)
{
  bits -= (bits != 0);		/* Round down, except if 0 */
  mp_size_t new_alloc = 1 + bits / GMP_NUMB_BITS;

  // Call realloc
  mp_limb_t *ret = (mp_limb_t*) realloc (m->_mp_d, new_alloc*sizeof(mp_limb_t));

  // Something screwed up
  if (ret == 0)
  {
    fprintf (stderr, 
      "GNU MP: Cannot reallocate memory (old_size=%lu new_size=%lu)\n",
      (long) m->_mp_alloc*sizeof(mp_limb_t), 
      (long) new_alloc*sizeof(mp_limb_t));
    abort ();
  }

  m->_mp_d = ret;
  m->_mp_alloc = new_alloc;

  /* Don't create an invalid number; if the current value doesn't fit
   * after reallocation, clear it to 0.  */
  if (m->_mp_size > new_alloc || m->_mp_size < -new_alloc)
    m->_mp_size = 0;
}


// A faster version of mpz_import for our use on x86_64 bit platforms
// Will not work if gmp uses nails
inline void fast_mpz_import(mpz_t m, uint8_t * raw, int bytes)
{
  // Commented since this does not compile on Lonestar.

  #if 0
    (__x86_64 == 1) && (GMP_NAIL_BITS == 0)
    int bits = bytes<<3;
  
    // find sizes
    //int bytes_per_limb = mp_bits_per_limb>>3;
    int size = ceil((double)bits/mp_bits_per_limb);

    // alloc number of limbs
    // only do if needed (speed)
    if (MICROBENCHMARKS)
      fast_mpz_realloc2(m, bits); 
    else if (size > m->_mp_alloc)
      fast_mpz_realloc2(m, bits);    

    int l = 0;
    char *x = (char *)m->_mp_d;
    mpn_zero(m->_mp_d, size);
    for (int i = bytes - 1; i>=0; i -= 8)
    {
      // a loop is unrolled for performance
      // memcpy used for speed
      memcpy((void*)(x),(void *)(raw+i), 1); 

      if (i-1 < 0) break;
      memcpy((void*)(x+1),(void *)(raw+i-1), 1);
    
      if (i-2 < 0) break;
      memcpy((void*)(x+2),(void *)(raw+i-2), 1);
    
      if (i-3 < 0) break;
      memcpy((void*)(x+3),(void *)(raw+i-3), 1);
      if (i-4 < 0) break;
      memcpy((void*)(x+4),(void *)(raw+i-4), 1);
      if (i-5 < 0) break;
      memcpy((void*)(x+5),(void *)(raw+i-5), 1);
      if (i-6 < 0) break;
      memcpy((void*)(x+6),(void *)(raw+i-6), 1);
      if (i-7 < 0) break;
      memcpy((void*)(x+7),(void *)(raw+i-7), 1);
    
      l++;
      x+=8;
    }

    // The size should be set so that _mp_d[size-1] != 0.
    // This is not necessarily the size of of the input
    // array.
    while(size-- > 0)
    {
      if (m->_mp_d[size] != 0)
        break;
    }

    // update size
    m->_mp_size = size + 1;
  #else
    mpz_import(m, bytes, 1, sizeof(char), 0, 0, raw);
  #endif
}

uint32_t zreverse (uint32_t v); 
void print_stats(const char *operation, vector<double> s);
string uint64_to_str(int size, uint64_t *num);
string uint8_to_str(int size, uint8_t *num); 
void run_mapred_shuffle_phase(int num_mappers, int num_reducers, int size_input_red, char *folder_path); 
void dump_scalar_to_fp(FILE *fp, mpz_t s); 
void load_scalar_from_fp(FILE *fp, mpz_t s); 
void digest_to_mpq_vec(mpq_t* out, hash_t* digest);
void mpq_vec_to_digest(hash_t* digest, const mpq_t* vec);
void export_digests_to_input(mpq_t* input_q, hash_t* serverside_in_d, hash_t* clientside_in_d);
void import_digests_from_input(const mpq_t* input_q, hash_t* serverside_in_d, hash_t* clientside_in_d);
void import_exo_inputs_to_bs(HashBlockStore *bs, void *ptr, int size, hash_t *digest); 
void sha256(int len, unsigned char *input, unsigned char *hash); 
void assert_zero(int test);
#endif  // CODE_PEPPER_COMMON_UTILITY_H_
