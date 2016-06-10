#include <dirent.h>
#include <errno.h>
#include <cassert>
#include <sys/stat.h>

#include <common/utility.h>
#include <storage/configurable_block_store.h>

// COMMON UTILITIES
void parse_args(int argc, char **argv, char *actor, int *phase, int *batch_size, int
                *rho_pcp, int *input_size, char *prover_url) {

  if (argc < 9) {
    cout<<"LOG: Fewer arguments passed; using defaults"<<endl;
    //cout<<"input format: [-a <v|p> -p <1|2> -b <num_of_batches> -r <num_verifier_repetitions> -i <input_size> -s <prover_url> -o <0|1>"<<endl;
  }

  // set sensible defaults
  *actor = 'v';
  *phase = 1;
  *batch_size = 1;
  *rho_pcp = 1;
  *input_size = 10;

  if (prover_url != NULL)
    prover_url[0] = '\0';

  for (int i=1; i<argc; i++) {
    if (strcmp(argv[i], "-a") == 0 && actor != NULL)
      *actor = argv[i+1][0];
    else if (strcmp(argv[i], "-p") == 0 && phase != NULL)
      *phase = atoi (argv[i+1]);
    else if (strcmp(argv[i], "-b") == 0)
      *batch_size = atoi (argv[i+1]);
    else if (strcmp(argv[i], "-r") == 0)
      *rho_pcp = atoi (argv[i+1]);
    else if (strcmp(argv[i], "-i") == 0)
      *input_size = atoi (argv[i+1]);
    else if (strcmp(argv[i], "-s") == 0 && prover_url != NULL) {
      strncpy(prover_url, argv[i+1], BUFLEN-1);
      prover_url[BUFLEN-1] = '\0';
    }
  }

}

void parse_args(int argc, char **argv, char *actor, int *phase, int *batch_size, int
                *rho_pcp, int *input_size, char *prover_url, int *generate_states) {

  parse_args(argc, argv, actor, phase, batch_size, rho_pcp, input_size, prover_url);

  //Set reasonable defaults
  *generate_states = 0;
  for (int i = 1; i < argc; i++) {
    if (strcmp(argv[i], "--gen-states") == 0 && generate_states != NULL)
      *generate_states = atoi(argv[i+1]);
  }
}

void parse_args(int argc, char **argv, char *actor, int *phase, int *batch_size, int
                *rho_pcp, int *input_size, char *prover_url, int *generate_states, char* shared_bstore_file_name) {
  parse_args(argc, argv, actor, phase, batch_size, rho_pcp, input_size, prover_url, generate_states);

  //Set reasonable defaults
  if (shared_bstore_file_name != NULL){
    strncpy(shared_bstore_file_name, "default_shared_db", BUFLEN - 1);
  }

  for (int i = 1; i < argc; i++) {
    if (strcmp(argv[i], "--shared-bstore-path") == 0 && shared_bstore_file_name != NULL)
      strncpy(shared_bstore_file_name, argv[i+1], BUFLEN-1);
  }
  shared_bstore_file_name[BUFLEN-1] = '\0';
}

void parse_http_args(char *query_string, int *phase, int *batch_size,
                     int *batch_start, int *batch_end, int *rho_pcp, int *input_size) {
  if (query_string == NULL) {
    return;
  }

  char *ptr = strtok(query_string, "=&");

  int key_id = -1;
  while (ptr != NULL) {
    if (strstr(ptr, "phase") != NULL)
      key_id = 0;
    else if (strstr(ptr, "batch_size") != NULL)
      key_id = 1;
    else if (strstr(ptr, "batch_start") != NULL)
      key_id = 2;
    else if (strstr(ptr, "batch_end") != NULL)
      key_id = 3;
    else if (strstr(ptr, "reps") != NULL)
      key_id = 4;
    else if (strstr(ptr, "m") != NULL)
      key_id = 5;
    else if (strstr(ptr, "opt") != NULL)
      key_id = 6;
    else {
      int arg = 0;
      if (key_id != -1)
        arg = atoi(ptr);

      switch (key_id) {
      case 0:
        *phase = arg;
        break;
      case 1:
        *batch_size = arg;
        break;
      case 2:
        *batch_start = arg;
        break;
      case 3:
        *batch_end = arg;
        break;
      case 4:
        *rho_pcp = arg;
        break;
      case 5:
        *input_size = arg;
        break;
      }
      key_id = -1;
    }
    ptr = strtok(NULL, "=&");
  }
}

std::list<string> get_files_in_dir(char *dir_name) {
  std::list<string> files;
  DIR *dir = opendir(dir_name);
  if (dir != NULL) {
    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
      string file_name(ent->d_name);
      if (!file_name.compare(".") || !file_name.compare(".."))
        continue;
      files.push_back(file_name);
    }
    closedir(dir);
  }
  files.sort();
  return files;
}

bool
recursive_mkdir(const string& dir, mode_t mode)
{
  struct stat sb;
  if (stat(dir.c_str(), &sb) == 0)
    return S_ISDIR(sb.st_mode);

  size_t sepIdx = dir.find_last_of('/');
  bool success = true;
  if (sepIdx != dir.npos)
    success = recursive_mkdir(dir.substr(0, sepIdx), mode);

  if (success)
  {
    success = mkdir(dir.c_str(), mode) == 0;
    success = success || (!success && errno != EEXIST);
  }

  return success;
}

void open_file_update(fstream& fp, const char* name, const char* folder_name) {
  char file_name[BUFLEN];

  if (folder_name == NULL)
    snprintf(file_name, BUFLEN-1, "%s/%s", FOLDER_STATE, name);
  else
    snprintf(file_name, BUFLEN-1, "%s/%s", folder_name, name);
  fp.open(file_name, ios_base::in | ios_base::out);
  if (!fp.is_open()) {
    fp.open(file_name, ios_base::out);
    if (!fp.is_open()) {
      cout <<"Warning: could not operate file "<<file_name << endl;
      exit(1);
    }
  }
}

void open_file_write(ofstream& fp, const char* name, const char*folder_name) {
  char file_name[BUFLEN];

  if (folder_name == NULL)
    snprintf(file_name, BUFLEN-1, "%s/%s", FOLDER_STATE, name);
  else
    snprintf(file_name, BUFLEN-1, "%s/%s", folder_name, name);
  fp.open(file_name);
  if (!fp.is_open()) {
    cout <<"Warning: could not operate file "<<file_name << endl;
    exit(1);
  }
}

void open_file_read(ifstream& fp, const char* name, const char*folder_name) {
  char file_name[BUFLEN];
  if (folder_name == NULL)
    snprintf(file_name, BUFLEN-1, "%s/%s", FOLDER_STATE, name);
  else
    snprintf(file_name, BUFLEN-1, "%s/%s", folder_name, name);
  fp.open(file_name);
  if (!fp.is_open()) {
    cout <<"Warning: could not operate file "<<file_name << endl;
    exit(1);
  }
}

bool open_file(FILE **fp, const char *vec_name, const char *permission, const char *folder_name) {
  char file_name[BUFLEN];

  if (folder_name == NULL)
    snprintf(file_name, BUFLEN-1, "%s/%s", FOLDER_STATE, vec_name);
  else
    snprintf(file_name, BUFLEN-1, "%s/%s", folder_name, vec_name);
  *fp = fopen(file_name, permission);
  if (*fp == NULL) {
    cout <<"Warning: could not operate file "<<file_name<<" with permision "<<permission<<endl;
  }
  return *fp == NULL;
}

long long int stat_size(const char* filename, const char*folder_name) {
  char file_name[BUFLEN];

  if (folder_name == NULL)
    snprintf(file_name, BUFLEN-1, "%s/%s", FOLDER_STATE, filename);
  else
    snprintf(file_name, BUFLEN-1, "%s/%s", folder_name, filename);
  struct stat st;
  stat(file_name, &st);
  return st.st_size;
}

void convert_to_z(const int size, mpz_t *z, const mpq_t *q, const mpz_t prime) {
  for (int i = 0; i < size; i++)
    convert_to_z(z[i], q[i], prime);
}

void convert_to_z(mpz_t z, const mpq_t q, const mpz_t prime) {
  assert(mpz_sgn(prime) != 0);
  if (mpz_cmp_ui(mpq_denref(q), 1) == 0) {
    mpz_set(z, mpq_numref(q));
  } else if (mpz_cmp_ui(mpq_denref(q), 0) == 0) {
    mpz_set_ui(z, 0);
  } else {
    mpz_invert(z, mpq_denref(q), prime);
    mpz_mul(z, z, mpq_numref(q));
  }
  mpz_mod(z, z, prime);
}

off_t get_file_size(const char *filename) {
  struct stat st;
  if (stat(filename, &st) == 0)
    return st.st_size;
  return -1;
}

void serialize_bits(int num_bits, bool *b, uint64_t n) {
  for (int i=0; i<num_bits; i++) {
    b[i] = n & 1;
    n = n>>1;
  }
}

void serialize_64bit(bool *b, uint64_t n) {
  serialize_bits(64, b, n);
}

void serialize_8bit(bool *b, uint8_t n) {
  serialize_bits(8, b, n);
}

string uint64_to_str(int size, uint64_t *num) {
  std::ostringstream o;
  bool bits[64];
  for (int i=0; i<size; i++) {
    serialize_64bit(bits, num[i]);
    for (int j=0; j<64; j++)
      o << bits[j];
  }
  string str = o.str();
  std::reverse(str.begin(), str.end());
  return str;
}

string uint8_to_str(int size, uint8_t *num) {
  std::ostringstream o;
  bool bits[8];
  for (int i=0; i<size; i++) {
    serialize_8bit(bits, num[i]);
    for (int j=0; j<8; j++)
      o << bits[j];
  }
  string str = o.str();
  std::reverse(str.begin(), str.end());
  return str;
}

void dump_vector_interleaved(int size, const mpz_t *q, const char *vec_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, vec_name, (char *)"wb", folder_name);
  if (fp == NULL) return;

  for (int i=0; i<size/2; i++)
    mpz_out_raw(fp, q[2*i]);

  for (int i=0; i<size/2; i++)
    mpz_out_raw(fp, q[2*i+1]);

  fclose(fp);
}

void dump_vector(int size, char *arr, const char *vec_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, vec_name, (char *)"wb", folder_name);
  if (fp == NULL) {
    cout<<"Cannot create "<<vec_name<<" at "<<folder_name<<endl;
    exit(1);
  }
  
  for (int i=0; i<size; i++)
    fprintf(fp, "%c", arr[i]);

  fclose(fp);
}

bool dump_vector(int size, const mpz_t *q, const char *vec_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, vec_name, (char *)"wb", folder_name);
  if (fp == NULL) return false;
//cout << "WRITING TO FILE: " << folder_name << vec_name << endl;
  for (int i=0; i<size; i++)
    mpz_out_raw(fp, q[i]);

  fclose(fp);
  return true;
}

bool dump_vector(int size, const mpq_t *q, const char *vec_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, vec_name, (char *)"wb", folder_name);

  if (!fp)
    return false;

  for (int i=0; i<size; i++) {
    mpz_out_raw(fp, mpq_numref(q[i]));
    mpz_out_raw(fp, mpq_denref(q[i]));
  }
  fclose(fp);
  return true;
}

/**
 * Dump just the numerators of a vector of rational numbers, writing them out as 32 bit ints.
 */
void dump_binary_nums(int size, const mpq_t *q, const char *vec_name, const char *folder_name) {
  FILE *fp;
  int32_t buf[1] = {0};
  open_file(&fp, vec_name, (char *)"wb", folder_name);
  for (int i=0; i<size; i++) {
    buf[0] = mpz_get_si(mpq_numref(q[i]));
    fwrite(buf, sizeof(int32_t), 1, fp);
  }
  fclose(fp);
}

void dump_scalar(const mpz_t q, char *scalar_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, scalar_name, (char *)"wb", folder_name);
  if (fp == NULL) return;
  mpz_out_raw(fp, q);
  fclose(fp);
}

/*
 * Dump an entire array of scalars. Useful when there are multiple functions in
 * the prover.
 *
 * Vectors will be stored with the following name:
 *      f<num_func>_<suffix>
 * num_func is an integer in [0, n).
 */
void dump_scalar_array(int n, const mpz_t *scalars, const char *suffix, char *folder_name) {
  char vec_name[BUFLEN];

  for (int i = 0; i < n; i++) {
    snprintf(vec_name, sizeof(vec_name), "f%d_%s", i, suffix);
    dump_scalar(scalars[i], vec_name, folder_name);
  }
}


void load_prng_seed(int bytes_key, void *key, int bytes_iv, void *iv) {
  FILE *fp;
  open_file(&fp, "seed_decommit_queries", (char *)"r", NULL);
  if (((size_t)bytes_key != fread(key, 1, bytes_key, fp)) || ((size_t)bytes_iv != fread(iv, 1, bytes_iv, fp)))
    cerr<<"Error loading the seed for the decommitment queries"<<endl;
  fclose(fp);
}

void dump_prng_seed(int bytes_key, void *key, int bytes_iv, void *iv) {
  FILE *fp;
  open_file(&fp, "seed_decommit_queries", (char *)"w", NULL);
  fwrite(key, 1, bytes_key, fp);
  fwrite(iv, 1, bytes_iv, fp);
  fclose(fp);
}

void digest_to_mpq_vec(mpq_t* out, hash_t* digest){
  for(int i = 0; i < NUM_HASH_CHUNKS; i++){
    mpq_set_ui(out[i], digest->bit[i], 1);
    //gmp_printf("Vec<-Digest %Qd %Lu\n", out[i], digest->bit[i]);
  }
}
void mpq_vec_to_digest(hash_t* digest, const mpq_t* vec) {
  for(int i = 0; i < NUM_HASH_CHUNKS; i++){
    digest->bit[i] = mpz_get_ui(mpq_numref(vec[i]));
    //gmp_printf("Vec->Digest %Qd %Lu\n", vec[i], digest->bit[i]);
  }
}

void export_digests_to_input(mpq_t* input_q, hash_t* serverside_in_d, hash_t* clientside_in_d) {
  //Assumes serverside struct comes first in MapperIn.
  digest_to_mpq_vec(input_q, serverside_in_d);
  digest_to_mpq_vec(input_q + NUM_HASH_CHUNKS, clientside_in_d);
}

void import_digests_from_input(const mpq_t* input_q, hash_t* serverside_in_d, hash_t* clientside_in_d) {
  //Assumes serverside struct comes first in MapperIn.
  mpq_vec_to_digest(serverside_in_d, input_q); 
  mpq_vec_to_digest(clientside_in_d, input_q + NUM_HASH_CHUNKS);
}

void sha256(int len, unsigned char *input, unsigned char *hash) {
  SHA256_CTX ctx;
  sha256_init(&ctx);
  sha256_update(&ctx, input, len);
  sha256_final(&ctx, hash);
}



// read a block by hash from a block store named by the hash and store it in bs.
void import_exo_inputs_to_bs(HashBlockStore *bs, void *ptr, int size, hash_t *digest) {
  string input_name = uint64_to_str(NUM_HASH_CHUNKS, digest->bit);
  unsigned char hash[20];
  char hexstring[42];
  sha1::calc(input_name.c_str(), strlen(input_name.c_str()), hash);
  sha1::toHexString(hash, hexstring);
  hexstring[41] = '\0';

  char db_file_path[BUFLEN];
  snprintf(db_file_path, BUFLEN - 1, "%s/%s/%s", FOLDER_STATE, ROOT_FOLDER_BLOCK_STORES, hexstring);
  FILE *fp = fopen(db_file_path, "rb");
  if (fread(ptr, 1, size, fp) != (size_t)size) {
    printf("error reading %d bytes from %s\n", size, db_file_path);
  }
  fclose(fp);
  
  //string db_file_path_str(db_file_path);
  //HashBlockStore* _block_store = new ConfigurableBlockStore(db_file_path_str);
  //__hashget(_block_store, ptr, digest, size);
  //delete _block_store;
  
  __hashput(bs, digest, ptr, size);
}


void load_scalar_from_fp(FILE *fp, mpz_t s) {
  mpz_inp_raw(s, fp);
}

void dump_scalar_to_fp(FILE *fp, mpz_t s) {
  mpz_out_raw(fp, s);
}

void run_mapred_shuffle_phase(int num_mappers, int num_reducers,
  int size_input_red, char *folder_path) {
  char scratch_str[BUFLEN];
  FILE *fp_m[num_mappers];
  FILE *fp_m_q[num_mappers];
  FILE *fp_r[num_reducers];
  FILE *fp_r_q[num_reducers];

  mpz_t temp;
  mpq_t temp_q;

  mpz_init(temp);
  mpq_init(temp_q);
  
  for (int i=0; i<num_mappers; i++) {
    snprintf(scratch_str, BUFLEN-1, "output_b_%d", i);
    open_file(&fp_m[i], scratch_str, "rb", folder_path);

    snprintf(scratch_str, BUFLEN-1, "output_q_b_%d", i);
    open_file(&fp_m_q[i], scratch_str, "rb", folder_path);

    // ignore the first number which is an exit status of the mapper
    load_scalar_from_fp((fp_m[i]), temp);
    load_scalar_from_fp((fp_m_q[i]), mpq_numref(temp_q));
    load_scalar_from_fp((fp_m_q[i]), mpq_denref(temp_q));
  }

  for (int i=0; i<num_reducers; i++) {
    snprintf(scratch_str, BUFLEN-1, "input_b_%d", i);
    open_file(&fp_r[i], scratch_str, "wb", folder_path);
    
    snprintf(scratch_str, BUFLEN-1, "input_q_b_%d", i);
    open_file(&fp_r_q[i], scratch_str, "wb", folder_path);
  }

  int size_input_each_mapper =  size_input_red/num_mappers;

  for (int k=0; k<num_reducers; k++) {
    for (int i=0; i<num_mappers; i++) {
      for (int j=0; j<size_input_each_mapper; j++) {
        // read in
        load_scalar_from_fp((fp_m[i]), temp);
        load_scalar_from_fp((fp_m_q[i]), mpq_numref(temp_q));
        load_scalar_from_fp((fp_m_q[i]), mpq_denref(temp_q));
        
        dump_scalar_to_fp((fp_r[k]), temp); 
        dump_scalar_to_fp((fp_r_q[k]), mpq_numref(temp_q)); 
        dump_scalar_to_fp((fp_r_q[k]), mpq_denref(temp_q)); 
      }
    }
  }

  for (int i=0; i<num_mappers; i++) {
    fclose(fp_m[i]);
    fclose(fp_m_q[i]);
  }

  for (int i=0; i<num_reducers; i++) {
    fclose(fp_r[i]);
    fclose(fp_r_q[i]);
  }  

  mpz_clear(temp);
  mpq_clear(temp_q);
}

void load_vector(int size, uint32_t *vec, const char *full_file_name) {
  FILE *fp = fopen(full_file_name, "r");
  if (fp == NULL) {
    cout<<"Cannot read "<<full_file_name<<endl;
    exit(1);
  }

  int ret;
  for (int i=0; i < size; i++) {
    ret = fscanf(fp, "%d ", &vec[i]);
    if (ret <= 0)
      break;
  }
  fclose(fp);
}

void load_vector(int size, char *q, const char *vec_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, vec_name, (char *)"rb", folder_name);
  if (fp == NULL) return;
  for (int i=0; i<size; i++)
    if (fscanf(fp, "%c", &q[i]) != 1) {
      printf("error reading from %s\n", vec_name);
    }
  fclose(fp);
}

void load_vector(int size, mpz_t *q, const char *vec_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, vec_name, (char *)"rb", folder_name);
  if (fp == NULL) return;
  for (int i=0; i<size; i++)
    mpz_inp_raw(q[i], fp);
  fclose(fp);
}

void load_vector(int size, mpq_t *q, const char *vec_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, vec_name, (char *)"rb", folder_name);
  for (int i=0; i<size; i++) {
    mpz_inp_raw(mpq_numref(q[i]), fp);
    mpz_inp_raw(mpq_denref(q[i]), fp);
  }
  fclose(fp);
}

void load_scalar(mpz_t q, const char *scalar_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, scalar_name, (char *)"rb", folder_name);
  if (fp == NULL) return;
  mpz_inp_raw(q, fp);
  fclose(fp);
}

/*
 * Load an entire array of scalars. Useful when there are multiple functions in
 * the prover.
 *
 * Vectors will be stored with the following name:
 *      f<num_func>_<suffix>
 * num_func is an integer in [0, n).
 */
void load_scalar_array(int n, mpz_t *scalars, const char *suffix, char *folder_name) {
  char vec_name[BUFLEN];

  for (int i = 0; i < n; i++) {
    snprintf(vec_name, sizeof(vec_name), "f%d_%s", i, suffix);
    load_scalar(scalars[i], vec_name, folder_name);
  }
}

void load_txt_scalar(mpz_t q, const char *scalar_name, const char *folder_name) {
  FILE *fp;
  open_file(&fp, scalar_name, (char *)"rb", folder_name);
  if (fp == NULL) return;
  mpz_inp_str(q, fp, 10);
  fclose(fp);
}

void clear_scalar(mpz_t s) {
  mpz_clear(s);
}

void clear_scalar(mpq_t s) {
  mpq_clear(s);
}

void clear_vec(int size, mpz_t *arr) {
  for (int i=0; i<size; i++)
    mpz_clear(arr[i]);
  delete[] arr; 
}

void clear_vec(int size, mpq_t *arr) {
  for (int i=0; i<size; i++)
    mpq_clear(arr[i]);
  delete[] arr; 
}

void alloc_init_vec(mpz_t **arr, uint32_t size) {
  *arr = new mpz_t[size];
  for (uint32_t i=0; i<size; i++)
    alloc_init_scalar((*arr)[i]);
}

void alloc_init_vec(mpq_t **arr, uint32_t size) {
  *arr = new mpq_t[size];
  for (uint32_t i=0; i<size; i++) {
    alloc_init_scalar((*arr)[i]);
  }
}

void alloc_init_vec_array(mpz_t ***array, const uint32_t n, const uint32_t vecSize) {
  mpz_t **newArray = new mpz_t*[n];

  for (uint32_t i = 0; i < n; i++)
    alloc_init_vec(&newArray[i], vecSize);

  *array = newArray;
}

void alloc_init_scalar(mpz_t s) {
  mpz_init2(s, INIT_MPZ_BITS);
  //mpz_init(s);
  mpz_set_ui(s, 0);
}

void alloc_init_scalar(mpq_t s) {
  mpq_init(s);
  mpq_set_ui(s, 0, 1);
}

void
clear_del_vec(mpz_t* vec, const uint32_t n) {
  for (uint32_t i = 0; i < n; i++)
    mpz_clear(vec[i]);
  delete[] vec;
}

void
clear_del_vec_array(mpz_t **array, const uint32_t n, const uint32_t vecSize) {
  for (uint32_t i = 0; i < n; i++) {
    clear_del_vec(array[i], vecSize);
  }
  delete[] array;
}

void clear_del_vec(mpq_t* vec, const uint32_t n) {
  for (uint32_t i = 0; i < n; i++)
    mpq_clear(vec[i]);
  delete[] vec;
}

bool modIfNeeded(mpz_t val, const mpz_t prime, int slack) {
  if (mpz_sizeinbase(val, 2) >= slack * mpz_sizeinbase(prime, 2) - 1)
  {
    mpz_mod(val, val, prime);
    return true;
  }
  else
  {
    return false;
  }
}

bool modIfNeeded(mpq_t val, const mpz_t prime, int slack) {
  bool changed = false;
  changed = modIfNeeded(mpq_numref(val), prime, slack) || changed;
  changed = modIfNeeded(mpq_denref(val), prime, slack) || changed;

  if (changed)
    mpq_canonicalize(val);

  return changed;
}

void
toTrueNumber(mpz_t a, const mpz_t halfPrime, const mpz_t prime)
{
  if (mpz_cmp(halfPrime, a) < 0)
  {
    mpz_sub(a, a, prime);
  }
}

void
toTrueNumber(mpq_t a, const mpz_t halfPrime, const mpz_t prime)
{
  if (mpz_cmp(halfPrime, mpq_numref(a)) < 0)
  {
    mpz_sub(mpq_numref(a), mpq_numref(a), prime);
    mpq_canonicalize(a);
  }
}

void print_matrix(mpz_t *matrix, uint32_t num_rows, uint32_t num_cols, string name) {
  cout << "\n" << name << " =" << endl;
  for (uint32_t i = 0; i < num_rows*num_cols; i++) {
    gmp_printf("%Zd ", matrix[i]);
    if (i % num_cols == num_cols - 1)
      gmp_printf("\n");
  }
  cout << endl;
}

void print_sq_matrix(mpz_t *matrix, uint32_t size, string name) {
  print_matrix(matrix, size, size, name);
}

void* aligned_malloc(size_t size) {
  void* ptr = malloc(size + PAGESIZE);

  if (ptr) {
    void* aligned = (void*)(((long)ptr + PAGESIZE) & ~(PAGESIZE - 1));
    ((void**)aligned)[-1] = ptr;
    return aligned;
  } else
    return NULL;
}

bool verify_conversion_to_z(size_t size, mpz_t *z, mpq_t *q, mpz_t prime) {
  mpz_t temp;
  mpz_init(temp);

  for (size_t i = 0; i < size; i++) {
    mpz_mul(temp, z[i], mpq_denref(q[i]));
    mpz_sub(temp, temp, mpq_numref(q[i]));
    mpz_mod(temp, temp, prime);
    if (mpz_cmp_ui(temp, 0) != 0) {
      //cout<<"ERROR: Conversion test failed."<<endl;
      return false;
    }
  }

  mpz_clear(temp);

  return true;
}

void print_stats(const char *operation, vector<double> s) {
  double min = 0, max = 0;
  double avg = 0;

  sort(s.begin(), s.begin()+s.size());

  int start_index = 0;
  int end_index = s.size();
  int count = 0;

  for (long i=start_index; i<end_index; i++) {
    if (s[i] < min || min == 0)
      min = s[i];

    if (s[i] > max)
      max = s[i];

    avg = avg + s[i];
    count = count + 1;
  }

  avg = avg/count;

  double sd = 0;
  for (long i=start_index; i<end_index; i++) {
    sd = sd + (s[i] - avg) * (s[i] - avg);
  }

  sd = sqrt(sd/count);
  cout<<"* "<<operation<<"_min "<<min<<endl;
  cout<<"* "<<operation<<"_max "<<max<<endl;
  cout<<"* "<<operation<<"_avg "<<avg<<endl;
  cout<<"* "<<operation<<"_sd "<<sd<<endl<<endl;
}

void assert_zero(int value){
  assert(value == 0);
}
