#include <include/db.h>
#include <storage/configurable_block_store.h>
#include <storage/gghA.h>
#include <storage/ram_impl.h>

#include <common/waksman_router.h>

// rsw added for exo_compute
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <vector>
#include <sstream>
#include <iterator>
#include <unistd.h>
#include <math.h>

#include <string>
#include <boost/unordered_map.hpp>

#include <gmp.h>

#include "computation_p.h"
#include <common/utility.h>


#include <iostream>

ComputationProver::
ComputationProver(int _num_vars, int _num_cons, int _size_input, int _size_output, 
                  mpz_t _prime, const char *_shared_bstore_file_name, string inputFilename, bool only_setup):
size_input(_size_input), size_output(_size_output), num_vars(_num_vars), num_cons(_num_cons), 
    shared_bstore_file_name(string(_shared_bstore_file_name))
{

    init_block_store();
    if (only_setup) {
        return;
    }

    size_f1_vec = num_vars;
    mpz_init_set(prime, _prime);
    alloc_init_vec(&F1, size_f1_vec);
    alloc_init_vec(&F1_q, size_f1_vec);
        
    F1_index = new uint32_t[size_f1_vec];
    for (int i = 0; i < size_f1_vec; i++)
        F1_index[i] = i;

    alloc_init_vec(&input_output_q, size_input+size_output);
    input_q = &input_output_q[0];
    output_q = &input_output_q[size_input];

    alloc_init_vec(&input_output, size_input+size_output);
    input = &input_output[0];
    output = &input_output[size_input];
    temp_stack_size = 16;
    alloc_init_vec(&temp_qs, temp_stack_size);

    alloc_init_scalar(temp);
    alloc_init_scalar(temp2);
    alloc_init_scalar(temp_q);
    alloc_init_scalar(temp_q2);

    ifstream inputFile(inputFilename);
    if (!inputFile.is_open()) {
        cerr << "ERROR: " << inputFilename << " not found. Did you run the verifier first?" << endl;
        cerr << "Aborting." << endl;
        exit(1);
    }

    for (int i = 0; i < size_input; i++) {
        inputFile >> input_q[i];
    }

}


ComputationProver::~ComputationProver() {
    if (_ram != NULL)
     delete _ram;
   if (_blockStore != NULL)
     delete _blockStore;
}

static void zcomp_assert(const char* a, const char* b,
    const char* error) {
  if (strcmp(a, b) != 0) {
    gmp_printf("%s, Expected : %s Actual: %s\n", error, b, a);
  }
}

static int try_next_token(FILE* fp, char* token) {
  return fscanf(fp, "%s", token);
}

static void next_token_or_error(FILE* fp, char* token) {
  int ret = fscanf(fp, "%s", token);
  if (ret != 1) {
    gmp_printf("Error reading from PWS file.\n");
  }
}

static void expect_next_token(FILE* fp, const char* expected, const char* error) {
  char buf[BUFLEN];
  next_token_or_error(fp, buf);
  zcomp_assert(buf, expected, error);
}

static std::vector<std::string> execute_command(char *cmd, const char *arg, std::string& procIn) {
  // get ready to launch child process
  int stdinFD[2];
  int stdoutFD[2];
  const int PIPE_RD = 0;  // much easier to read this way
  const int PIPE_WR = 1;

  if (pipe(stdinFD) < 0) {
      gmp_printf("ERROR: exo_compute couldn't open stdin pipes: %d\n", errno);
      exit(1);
  }
  if (pipe(stdoutFD) < 0) {
      gmp_printf("ERROR: exo_compute couldn't open stdout pipes: %d\n", errno);
      exit(1);
  }

  int chld = fork();
  if (chld == 0) {    // this is the child process
      //make sure the executable exists
      std::ifstream f(cmd);
      if (f.good()) {
          f.close();
      }
      else {
          gmp_printf("ERROR: %s not found. Aborting.\n");
          exit(1);
      }

      // "a careful programmer will not use dup2() without closing newfd first."
      close(STDIN_FILENO);
      close(STDOUT_FILENO);

      // replace stdin and stdout with the pipes the parent gave us
      if (dup2(stdinFD[PIPE_RD], STDIN_FILENO) < 0) {
          gmp_printf("ERROR: execute_command (child) could not replace stdin: %d\n", errno);
          exit(-1);
      }
      if (dup2(stdoutFD[PIPE_WR], STDOUT_FILENO) < 0) {
          gmp_printf("ERROR: execute_command (child) could not replace stdout: %d\n", errno);
          exit(-1);
      }

      // don't need these any more. Note that we are closing after duping, which
      // is OK because the other copy is still open
      close(stdinFD[0]); close(stdinFD[1]); close(stdoutFD[0]); close(stdoutFD[1]);

      // now exec it
      // "exo0 100 5" or whatever
      execlp(cmd,cmd,arg,(char *)NULL);
      //execlp("cat","cat",(char *)NULL);

      // if we get here, there was an error!
      gmp_printf("ERROR: execute_command (child) failed to exec: %d\n", errno);
      exit(-1);
  } else if (chld < 0) {  // failed to create child process
      gmp_printf("ERROR: execute_command failed to fork: %d\n", errno);
      exit(1);
  }

  // don't need the remote endpoints of these two pipes
  close(stdinFD[PIPE_RD]); close(stdoutFD[PIPE_WR]);

  // write the input to the process
  if (write(stdinFD[PIPE_WR], procIn.c_str(), procIn.length()) < (int) procIn.length()) {
      gmp_printf("ERROR: execute_command failed to write full input string to child process: %d\n", errno);
      exit(1);
  }
  // close the pipe to send EOF
  close(stdinFD[PIPE_WR]);

  // now grab its output back
  std::stringstream procOut;
  int rdNum = read(stdoutFD[PIPE_RD], cmd, BUFLEN-1);
  while (rdNum > 0) {
      cmd[rdNum] = '\0'; // add null termination so we can send it to the stream
      procOut << cmd;
      rdNum = read(stdoutFD[PIPE_RD], cmd, BUFLEN-1);
  }
  close(stdoutFD[PIPE_RD]);
  // make sure we didn't error out of the above loop
  if (rdNum < 0) {
      gmp_printf("ERROR: execute_command failed reading from child process: %d\n", errno);
      exit(1);
  }

  // now tokenize the input we just got
  std::istream_iterator<std::string> pOutIter(procOut);
  std::istream_iterator<std::string> eof;
  std::vector<std::string> pOutTok(pOutIter,eof);
  return pOutTok;
}

void ComputationProver::init_block_store() {
  snprintf(bstore_file_path, BUFLEN - 1, "%s/block_stores", FOLDER_STATE);
  mkdir(bstore_file_path, S_IRWXU);

  // the name of the block store is shared between the verifier and the prover.
  char bstore_file_path_priv[BUFLEN];
  snprintf(bstore_file_path_priv, BUFLEN - 1, "%s/prover_%s", bstore_file_path, shared_bstore_file_name.c_str());
  string bstore_file_path_priv_str(bstore_file_path_priv);
  _blockStore = new ConfigurableBlockStore(bstore_file_path_priv_str);
  _ram = new RAMImpl(_blockStore);
  // exogenous check unimplemented for now.
  //  exogenous_checker->set_block_store(_blockStore, _ram);
}

void ComputationProver::compute_poly(FILE* pws_file, int tempNum) {

  mpq_t& polyTarget = temp_qs[tempNum];
  mpq_t& termTarget = temp_qs[tempNum+1];
  mpq_set_ui(polyTarget, 0, 1);
  if (tempNum >= temp_stack_size-1) {
    gmp_printf("ERROR IN PROVER - Polynomial required more than %d recursive calls \n", temp_stack_size);
    return;
  }

  bool hasTerms = false;
  bool hasFactors = false;
  bool isEmpty = true;
  bool negate = false;

  char tok[BUFLEN], cmds[BUFLEN];
  while(try_next_token(pws_file, tok) != EOF) {
    //Emit last term, if necessary:
    if (strcmp(tok, "+") == 0 || strcmp(tok, "-") == 0) {
      if (hasFactors) {
        if (negate) {
          mpq_neg(termTarget, termTarget);
        }
        if (!hasTerms) {
          mpq_set(polyTarget, termTarget);
        } else {
          mpq_add(polyTarget, polyTarget, termTarget);
        }
        hasTerms = true;
        isEmpty = false;
        hasFactors = false;
        negate = false;
      }
    }

    if (strcmp(tok, "(") == 0) {
      //Recurse
      compute_poly(pws_file, tempNum + 2);
      mpq_t& subresult = temp_qs[tempNum + 2];
      if (!hasFactors) {
        mpq_set(termTarget, subresult);
      } else {
        mpq_mul(termTarget, termTarget, subresult);
      }
      hasFactors = true;
    } else if (strcmp(tok, ")") == 0) {
      break;
    } else if (strcmp(tok, "E") == 0) {
      break;
    } else if (strcmp(tok, "+") == 0 || strcmp(tok, "*") == 0) {
      //handled below
    } else if (strcmp(tok, "-") == 0) {
      negate = !negate;
      //remaining handled below
    } else {
      //Factor. (either constant or variable)
      mpq_t& factor = voc(tok, temp_q);
      if (!hasFactors) {
        mpq_set(termTarget, factor);
      } else {
        mpq_mul(termTarget, termTarget, factor);
      }
      hasFactors = true;
    }
  }

  //Emit last term, if necessary:
  if (hasFactors) {
    if (negate) {
      mpq_neg(termTarget, termTarget);
    }
    if (!hasTerms) {
      mpq_set(polyTarget, termTarget);
    } else {
      mpq_add(polyTarget, polyTarget, termTarget);
    }
    hasTerms = true;
    isEmpty = false;
    hasFactors = false;
    negate = false;
  }

  //Set to zero if the polynomial is empty
  if (isEmpty) {
    mpq_set_ui(polyTarget, 0, 1);
  }
}


// Expected format SI INPUT into LENGTH bits at FIRST_OUTPUT
void ComputationProver::compute_split_unsignedint(FILE* pws_file) {
  mpq_t* in = NULL;
  char cmds[BUFLEN];

  //cout << *cmds << endl;
  next_token_or_error(pws_file, cmds);

  if (cmds[0] == 'V') {
    in = &F1_q[F1_index[atoi(cmds + 1)]];
  } else if (cmds[0] == 'I') {
    in = &input_q[atoi(cmds + 1)];
  }
  expect_next_token(pws_file, "into", "Invalid SI");
  next_token_or_error(pws_file, cmds);
  int length = atoi(cmds);
  expect_next_token(pws_file, "bits", "Invalid SI");
  expect_next_token(pws_file, "at", "Invalid SI");
  ////cout << *cmds << endl;
  next_token_or_error(pws_file, cmds);
  int output_start = atoi(cmds + 1);

  //Fill in the Ni with the bits of in.
  //Each bit is either 0 or 1
  //gmp_printf("%Zd\n", in);
  for(int i = 0; i < length; i++) {
    mpq_t& Ni = F1_q[F1_index[output_start + i]];
    int bit = mpz_tstbit(mpq_numref(*in), length - i - 1);
    //cout << bit << endl;
    mpq_set_ui(Ni, bit, 1);
    //gmp_printf("%Zd\n", Ni);
  }
}

// Expected format SIL (uint | int) bits <length> X <input> Y0 <first bit of output>
void ComputationProver::compute_split_int_le(FILE* pws_file) {
  char cmds[BUFLEN];

  next_token_or_error(pws_file, cmds);
  bool isSigned = cmds[0] != 'u';
  expect_next_token(pws_file, "bits", "Invalid SIL");
  next_token_or_error(pws_file, cmds);
  int N = atoi(cmds);
  expect_next_token(pws_file, "X", "Invalid SIL");
  next_token_or_error(pws_file, cmds);
  mpq_t& in = voc(cmds, temp_q);
  expect_next_token(pws_file, "Y0", "Invalid SIL");
  next_token_or_error(pws_file, cmds);
  if (cmds[0] != 'V'){
    gmp_printf("Assertion Error: Cannot output split gate bits to %s, a V# was required.\n", cmds);
  }
  int output_start = atoi(cmds + 1);

  //Fill in the Ni with the bits of in 
  //Each bit is either 0 or 1
  //gmp_printf("%Zd\n", in);

  mpz_set(temp, mpq_numref(in));
  bool inIsNegative = mpz_sgn(temp) < 0;
  if (!isSigned && inIsNegative){
    gmp_printf("Assertion Error: Negative integer input to unsigned split gate\n");
  }
  if (inIsNegative){
    mpz_set_ui(temp2, 1);
    mpz_mul_2exp(temp2, temp2, N);
    mpz_add(temp, temp, temp2);
  }
  for(int i = 0; i < N; i++) {
    mpq_t& Ni = F1_q[F1_index[output_start + i]];
    if (i == N-1 && isSigned){
      mpz_set_ui(temp2, inIsNegative ? 1 : 0);
      //If the number is negative, then temp2 should be the sign bit.
      //Subtract it off.
      mpz_sub(temp, temp, temp2);
    } else {
      mpz_tdiv_r_2exp(temp2, temp, 1);
      mpz_tdiv_q_2exp(temp, temp, 1);
    }
    mpq_set_z(Ni, temp2);
  }
  if (mpz_sgn(temp)!=0){
    gmp_printf("Assertion Error: Some bits left over %Qd, in splitting %Qd to signed? %d bits %d\n", temp, in, isSigned, N);
  }
}

void ComputationProver::compute_less_than_int(FILE* pws_file) {
  char cmds[BUFLEN];

  expect_next_token(pws_file, "N_0", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  int N_start = atoi(cmds + 1);
  expect_next_token(pws_file, "N", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  int N = atoi(cmds);
  expect_next_token(pws_file, "Mlt", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Mlt = voc(cmds, temp_q);
  expect_next_token(pws_file, "Meq", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Meq = voc(cmds, temp_q);
  expect_next_token(pws_file, "Mgt", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Mgt = voc(cmds, temp_q);

  expect_next_token(pws_file, "X1", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& X1 = voc(cmds, temp_q);
  expect_next_token(pws_file, "X2", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& X2 = voc(cmds, temp_q);
  expect_next_token(pws_file, "Y", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Y = voc(cmds, temp_q);

  int compare = mpq_cmp(X1, X2);
  if (compare < 0) {
    mpq_set_ui(Mlt, 1, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_sub(temp_qs[0], X1, X2);
  } else if (compare == 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 1, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_sub(temp_qs[0], X1, X2);
  } else if (compare > 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 1, 1);
    mpq_sub(temp_qs[0], X2, X1);
  }
  mpq_set(Y, Mlt);

  mpz_set_ui(temp, 1);
  mpz_mul_2exp(temp, temp, N-1);
  mpz_add(temp, temp, mpq_numref(temp_qs[0]));

  //Fill in the Ni with the bits of the difference + 2^(N-1)
  //Each bit is either 0 or the power of two, so the difference = sum (Ni)
  for(int i = 0; i < N-1; i++) {
    mpq_t& Ni = F1_q[F1_index[N_start + i]];
    mpz_tdiv_r_2exp(temp2, temp, 1);
    mpq_set_z(Ni, temp2);
    mpq_mul_2exp(Ni, Ni, i);
    mpz_tdiv_q_2exp(temp, temp, 1);
  }
}

void ComputationProver::compute_less_than_float(FILE* pws_file) {
  char cmds[BUFLEN];
  expect_next_token(pws_file, "N_0", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  int N_start = atoi(cmds + 1);

  expect_next_token(pws_file, "Na", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  int Na = atoi(cmds);

  expect_next_token(pws_file, "N", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& N = voc(cmds, temp_q);

  expect_next_token(pws_file, "D_0", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  int D_start = atoi(cmds + 1);

  expect_next_token(pws_file, "Nb", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  int Nb = atoi(cmds);

  expect_next_token(pws_file, "D", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& D = voc(cmds, temp_q);

  expect_next_token(pws_file, "D", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& ND = voc(cmds, temp_q);

  expect_next_token(pws_file, "Mlt", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Mlt = voc(cmds, temp_q);
  expect_next_token(pws_file, "Meq", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Meq = voc(cmds, temp_q);
  expect_next_token(pws_file, "Mgt", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Mgt = voc(cmds, temp_q);

  expect_next_token(pws_file, "X1", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& X1 = voc(cmds, temp_q);
  expect_next_token(pws_file, "X2", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& X2 = voc(cmds, temp_q);
  expect_next_token(pws_file, "Y", "Invalid <I");
  next_token_or_error(pws_file, cmds);
  mpq_t& Y = voc(cmds, temp_q);

  int compare = mpq_cmp(X1, X2);
  if (compare < 0) {
    mpq_set_ui(Mlt, 1, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_sub(temp_q, X1, X2);
    mpq_set_z(N, mpq_numref(temp_q));
    mpq_set_z(D, mpq_denref(temp_q)); //should be positive
  } else if (compare == 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 1, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_set_si(N, -1, 1);
    mpq_set_ui(D, 1, 1);
  } else if (compare > 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 1, 1);
    mpq_sub(temp_q, X2, X1);
    mpq_set_z(N, mpq_numref(temp_q));
    mpq_set_z(D, mpq_denref(temp_q)); //should be positive
  }
  mpq_set(Y, Mlt);

  mpz_set_ui(temp, 1);
  mpz_mul_2exp(temp, temp, Na);
  mpz_add(temp, temp, mpq_numref(N)); //temp = 2^Na + (numerator of difference)

  //Fill in the Ni with the bits of the numerator difference + 2^Na
  //Each bit is either 0 or the power of two, so N = sum (Ni)
  for(int i = 0; i < Na; i++) {
    mpq_t& Ni = F1_q[F1_index[N_start + i]];
    mpz_tdiv_r_2exp(temp2, temp, 1);
    mpq_set_z(Ni, temp2);
    mpq_mul_2exp(Ni, Ni, i);
    mpz_tdiv_q_2exp(temp, temp, 1);
  }

  mpz_set(temp, mpq_numref(D));

  //Fill in the Di with whether the denominator is a particular power of
  //two.
  for(int i = 0; i < Nb + 1; i++) {
    mpq_t& Di = F1_q[F1_index[D_start + i]];
    mpz_tdiv_r_2exp(temp2, temp, 1);
    mpq_set_z(Di, temp2);
    mpz_tdiv_q_2exp(temp, temp, 1);
  }

  //Invert D.
  mpq_inv(D, D);
  //Compute N D
  mpq_mul(ND, N, D);
}

void ComputationProver::compute_db_get_bits(FILE* pws_file) {
  char cmds[BUFLEN];
  next_token_or_error(pws_file, cmds);
  mpq_t& index = voc(cmds, temp_q);
  uint32_t idx = mpz_get_ui(mpq_numref(index));
  next_token_or_error(pws_file, cmds);
  mpq_t& nb = voc(cmds, temp_q);
  uint32_t numBits = mpz_get_ui(mpq_numref(nb));

  Bits data = _ram->get(idx);
    if (numBits != data.size()) {
      gmp_printf("ERROR: compute_db_get_bits: wrong number of bits requested: index=%d, requested=%d, stored=%d\n",
          idx, numBits, data.size());
      exit(1);
    }

  //cout << "prover: get bits at " << idx << endl;
  for (uint32_t i = 0; i < numBits; i++) {
    //cout << data[i];
    next_token_or_error(pws_file, cmds);
    mpq_t& val = voc(cmds, temp_q);
    mpz_set_ui(mpq_numref(val), static_cast<uint32_t>(data[i]));
    mpq_canonicalize(val);
  }
  //cout << endl;
}

void ComputationProver::compute_db_put_bits(FILE* pws_file) {
  char cmds[BUFLEN];
  next_token_or_error(pws_file, cmds);
  mpq_t& index = voc(cmds, temp_q);
  uint32_t idx = mpz_get_ui(mpq_numref(index));
  next_token_or_error(pws_file, cmds);
  mpq_t& nb = voc(cmds, temp_q);
  uint32_t numBits = mpz_get_ui(mpq_numref(nb));

  Bits data(numBits);

  for (uint32_t i = 0; i < numBits; i++) {
    next_token_or_error(pws_file, cmds);
    mpq_t& val = voc(cmds, temp_q);
    uint32_t bit = mpz_get_ui(mpq_numref(val));

    if (bit != 0 && bit != 1) {
      gmp_printf("ERROR: compute_db_put_bits: input is not a bit: index=%d, input=%d, bit_index=%d\n",
          idx, bit, i);
      exit(1);
    }

    data[i] = static_cast<bool>(bit);
  }

  _ram->put(idx, data);
}

void ComputationProver::compute_exo_compute(FILE *pws_file) {
    // read in the EXO_COMPUTE line
    char cmds[BUFLEN];

    // grab exoid
    expect_next_token(pws_file, "EXOID", "Invalid EXO_COMPUTE");
    next_token_or_error(pws_file,cmds);
    int exoId = atoi(cmds);

    expect_next_token(pws_file, "INPUTS", "Invalid EXO_COMPUTE");
    expect_next_token(pws_file, "[", "Invalid EXO_COMPUTE");
    std::vector< std::vector<std::string> > inVarsStr;
    // get the input args
    getLL(inVarsStr, pws_file, cmds);

    expect_next_token(pws_file, "OUTPUTS", "Invalid EXO_COMPUTE");
    expect_next_token(pws_file, "[", "Invalid EXO_COMPUTE");
    std::vector<std::string> outVarsStr;
    // get the output args
    getL(outVarsStr, pws_file, cmds);

    // now prepare the string we will send to stdin of the process
    std::stringstream procIn;
    for (std::vector< std::vector<std::string> >::iterator it = inVarsStr.begin(); it != inVarsStr.end(); it++) {
        procIn << "[ ";
        for (std::vector<std::string>::iterator jt = (*it).begin(); jt != (*it).end(); jt++) {
            mpq_t &vval = voc((*jt).c_str(), temp_q);
            procIn << mpz_get_str(cmds,10,mpq_numref(vval)) << '%';
            procIn << mpz_get_str(cmds,10,mpq_denref(vval)) << ' ';
        }
        procIn << "] ";
    }
    std::string procInStr = procIn.str();

    // build up the arguments to the command and run it
    sprintf(cmds, "./bin/exo%d",exoId);
    
    // buffer is totally big enough that this is acceptable. tee hee
    char *outLenStr = cmds + strlen(cmds) + 2;
    sprintf(outLenStr,"%ld",outVarsStr.size());
    std::vector<std::string> pOutTok = execute_command(cmds, outLenStr, procInStr);

    int pTok = 0;

    // walk through the tokens from the child process and the outVars list, assigning the latter to the former
    for ( std::vector<std::string>::iterator it = outVarsStr.begin() ;
          (it != outVarsStr.end()) && (pTok < (int) pOutTok.size()) ; 
          it++ , pTok++ ) {
        mpq_t &vval = voc((*it).c_str(),temp_q);
        if (&vval == &temp_q) {
            gmp_printf("ERROR: exo_compute trying to write output to a const value, %s.\n", (*it).c_str());
        } else if (mpq_set_str(vval, pOutTok[pTok].c_str(), 0) < 0) {
            gmp_printf("ERROR: exo_compute failed to convert child process output to mpq_t: %s\n", pOutTok[pTok].c_str());
        }
    }

    // and we're done!
    return;
}

void ComputationProver::compute_ext_gadget(FILE *pws_file) {
    // read in the EXO_COMPUTE line
    char cmds[BUFLEN];

    // grab exoid
    expect_next_token(pws_file, "GADGETID", "Invalid EXT_GADGET");
    next_token_or_error(pws_file,cmds);
    int gadgetId = atoi(cmds);

    expect_next_token(pws_file, "INPUTS", "Invalid EXT_GADGET");
    expect_next_token(pws_file, "[", "Invalid EXT_GADGET");
    std::vector<std::string> inVarsStr;
    // get the input args
    getL(inVarsStr, pws_file, cmds);

    expect_next_token(pws_file, "OUTPUTS", "Invalid EXT_GADGET");
    expect_next_token(pws_file, "[", "Invalid EXT_GADGET");
    std::vector<std::string> outVarsStr;
    // get the output args
    getL(outVarsStr, pws_file, cmds);

    expect_next_token(pws_file, "INTERMEDIATE", "Invalid EXT_GADGET");
    expect_next_token(pws_file, "[", "Invalid EXT_GADGET");
    std::vector<std::string> intermediateVarsStr;
    // get the output args
    getL(intermediateVarsStr, pws_file, cmds);

    // now prepare the string we will send to stdin of the process
    std::stringstream procIn;
    for (std::vector<std::string>::iterator it = inVarsStr.begin(); it != inVarsStr.end(); it++) {
      mpq_t &vval = voc((*it).c_str(), temp_q);
      procIn << mpq_get_str(cmds,10,vval) << " ";
    }
    std::string procInStr = procIn.str();

    // build up the arguments to the command and run it
    sprintf(cmds, "./bin/gadget%d", gadgetId);
    std::vector<std::string> pOutTok = execute_command(cmds, "witness", procInStr);

    // walk through the tokens from the child process and the variable lists, assigning the latter to the former
    // First inputs, they should be the same as what we passed in
    std::vector<std::string>::const_iterator outTokIt = pOutTok.cbegin();
    for (std::vector<std::string>::const_iterator it = inVarsStr.cbegin(); it != inVarsStr.cend(); it++) {
      mpq_t &vval = voc((*it).c_str(),temp_q);
      if (&vval == &temp_q) {
          gmp_printf("ERROR: ext_gadget trying to write output to a const value, %s.\n", (*it).c_str());
      }
      char value[BUFLEN];
      mpq_get_str(value, 10, vval);
      assert(strcmp(value, outTokIt->c_str()) == 0);
      outTokIt++;
    }

    // Then assing outputs
    for (std::vector<std::string>::const_iterator it = outVarsStr.cbegin(); it != outVarsStr.cend(); it++) {
      mpq_t &vval = voc((*it).c_str(),temp_q);
      assert(outTokIt != pOutTok.cend());
      if (&vval == &temp_q) {
          gmp_printf("ERROR: ext_gadget trying to write output to a const value, %s.\n", (*it).c_str());
      } else if (mpq_set_str(vval, outTokIt->c_str(), 0) < 0) {
          gmp_printf("ERROR: ext_gadget failed to convert child process output to mpq_t: %s\n", outTokIt->c_str());
      }
      outTokIt++;
    }

    // Last but not least assing intermediate
    for (std::vector<std::string>::const_iterator it = intermediateVarsStr.cbegin(); it != intermediateVarsStr.cend(); it++) {
      mpq_t &vval = voc((*it).c_str(),temp_q);
      assert(outTokIt != pOutTok.cend());
      if (&vval == &temp_q) {
          gmp_printf("ERROR: ext_gadget trying to write output to a const value, %s.\n", (*it).c_str());
      } else if (mpq_set_str(vval, outTokIt->c_str(), 0) < 0) {
          gmp_printf("ERROR: ext_gadget failed to convert child process output to mpq_t: %s\n", outTokIt->c_str());
      }
      outTokIt++;
    }
    // Make sure there are no more tokens left in the output
    assert(outTokIt == pOutTok.cend());

    // and we're done!
    return;
}

void ComputationProver::getLL(std::vector< std::vector<std::string> > &inLL, FILE *pws_file, char *buf) {
    // pull until we get ] ]
    while(1) {
        // if we have ] then we're done
        next_token_or_error(pws_file, buf);
        if (strcmp(buf,"]") == 0) { break; }

        // otherwise it had better have been [
        zcomp_assert(buf,"[","Invalid EXO_COMPUTE");

        // now pull in the whole list
        std::vector<std::string> tmpList;
        getL(tmpList,pws_file,buf);
        inLL.push_back(tmpList);
    }
}

void ComputationProver::getL(std::vector<std::string> &inL, FILE *pws_file, char *buf) {
    while(1) {
        next_token_or_error(pws_file, buf);
        if(strcmp(buf,"]") == 0) { break; }
        // convert char* so string, add to vector
        string sbuf(buf);
        inL.push_back(sbuf);
    }
}

#define MAX_RAM_SIZE (1 << (FAST_RAM_ADDRESS_WIDTH))

static boost::unordered_map<size_t, mpq_t*> true_ram;

void ComputationProver::compute_fast_ramget(FILE* pws_file) {
  char cmds[BUFLEN];

  mpq_t addr;
  mpq_init(addr);

  expect_next_token(pws_file, "ADDR", "Invalid RAMGET_FAST");
  next_token_or_error(pws_file, cmds);
  mpq_set(addr, voc(cmds, temp_q));

  expect_next_token(pws_file, "VALUE", "Invalid RAMGET_FAST");
  next_token_or_error(pws_file, cmds);
  mpq_t& value = voc(cmds, temp_q);

  // we don't need to worry about ramget in a branch, do we?
  // No, we don't
  size_t addr_idx = mpz_get_ui(mpq_numref(addr));
  if (true_ram.find(addr_idx) != true_ram.end()) {
    mpq_set(value, true_ram[addr_idx][0]);
  } else {
    mpq_set_si(value, 0, 1);
  }
  //gmp_printf("ramget addr: %d value: %ld\n", addr_idx, true_ram[addr_idx]);

  mpq_clear(addr);
}

void ComputationProver::compute_fast_ramput(FILE* pws_file) {
  char cmds[BUFLEN];

  mpq_t addr, value, condition;
  mpq_inits(addr, value, condition, NULL);

  expect_next_token(pws_file, "ADDR", "Invalid RAMPUT_FAST");
  next_token_or_error(pws_file, cmds);
  mpq_set(addr, voc(cmds, temp_q));

  expect_next_token(pws_file, "VALUE", "Invalid RAMPUT_FAST");
  next_token_or_error(pws_file, cmds);
  mpq_set(value, voc(cmds, temp_q));

  expect_next_token(pws_file, "CONDITION", "Invalid RAMPUT_FAST");
  next_token_or_error(pws_file, cmds);
  mpq_set(condition, voc(cmds, temp_q));
  next_token_or_error(pws_file, cmds);

  int branch = 0;
  if (strcmp(cmds, "true") == 0) {
    branch = 1;
  }

  next_token_or_error(pws_file, cmds);
  mpq_t& target = voc(cmds, temp_q);

  // if the conditional bit is non-zero, actually execute the ramput
  size_t addr_idx = mpz_get_ui(mpq_numref(addr));
  if (mpq_cmp_ui(condition, 0, 1) != 0) {
    if (branch) {
      if (true_ram.find(addr_idx) != true_ram.end()) {
        mpq_set(true_ram[addr_idx][0], value);
      } else {
        true_ram[addr_idx] = new mpq_t[1];
        mpq_init(true_ram[addr_idx][0]);
        mpq_set(true_ram[addr_idx][0], value);
      }
      mpq_set(target, value);
      //int64_t v = mpz_get_si(mpq_numref(value));
      //true_ram[addr_idx] = v;
      //gmp_printf("ramput addr: %d value %ld\n", addr_idx, true_ram[addr_idx]);
    } else {
      if (true_ram.find(addr_idx) != true_ram.end()) {
        mpq_set(target, true_ram[addr_idx][0]);
      } else {
        mpq_set_si(target, 0, 1);
      }
    }
  } else {
    if (!branch) {
      if (true_ram.find(addr_idx) != true_ram.end()) {
        mpq_set(true_ram[addr_idx][0], value);
      } else {
        true_ram[addr_idx] = new mpq_t[1];
        mpq_init(true_ram[addr_idx][0]);
        mpq_set(true_ram[addr_idx][0], value);
      }
      //int64_t v = mpz_get_si(mpq_numref(value));
      //true_ram[addr_idx] = v;
      //gmp_printf("ramput addr: %d value %ld\n", addr_idx, true_ram[addr_idx]);
    } else {
      if (true_ram.find(addr_idx) != true_ram.end()) {
        mpq_set(target, true_ram[addr_idx][0]);
      } else {
        mpq_set_si(target, 0, 1);
      }
    }
  }

  mpq_clears(addr, value, condition, NULL);
}


void ComputationProver::compute_db_get_sibling_hash(FILE* pws_file) {
  char cmds[BUFLEN];
  next_token_or_error(pws_file, cmds);
  mpq_t& index = voc(cmds, temp_q);
  uint32_t idx = mpz_get_ui(mpq_numref(index));
  next_token_or_error(pws_file, cmds);
  mpq_t& level = voc(cmds, temp_q);
  uint32_t lvl = mpz_get_ui(mpq_numref(level));
  next_token_or_error(pws_file, cmds);
  int hashVarStart = atoi(cmds + 1);

  Bits hash;
  bool found = _ram->getSiblingHash(idx, lvl, hash);
  if (!found) {
    gmp_printf("ERROR: Sibling hash not found: index=%d, level=%d\n", idx, lvl);
    exit(1);
  }

  int numBits = _ram->getNumHashBits();
  for (int i = 0; i < numBits; i++) {
    mpq_t& hashVar = F1_q[F1_index[hashVarStart + i]];
    // Output bits in big endian for compatibility with other functions (e.g. SI)
    mpz_set_ui(mpq_numref(hashVar), static_cast<uint32_t>(hash[numBits - i - 1]));
    mpq_canonicalize(hashVar);
  }
}

void ComputationProver::parse_hash(FILE* pws_file, HashBlockStore::Key& outKey, int numHashBits) {
  char cmds[BUFLEN];

  outKey.resize(numHashBits);
  for (int i = 0; i < numHashBits; i++) {
    next_token_or_error(pws_file, cmds);
    if (strcmp(cmds, "NUM_X") == 0 || strcmp(cmds, "NUM_Y") == 0) {
      gmp_printf("ERROR: parse_hash: wrong number of hash vars: expected=%d, actual=%d\n", numHashBits, i);
      exit(1);
    }

    mpq_t& val = voc(cmds, temp_q);
    uint32_t bit = mpz_get_ui(mpq_numref(val));
    outKey[i] = static_cast<bool>(bit);
  }
}

void ComputationProver::compute_get_block_by_hash(FILE* pws_file) {
  char cmds[BUFLEN];

  HashBlockStore::Key key;
  parse_hash(pws_file, key, _ram->getNumHashBits());

  HashBlockStore::Value block;
  uint32_t blockSize = 0;

  // Special case: if the key == 0, don't fail. Just return zeroes.
  if (key.any()) {
    bool found = _blockStore->get(key, block);
    if (!found) {
      int numHashBits = _ram->getNumHashBits();

      std::string s;
      boost::to_string(key, s);
      gmp_printf("ERROR: compute_get_block_by_hash: block not found: hash=\n");
      for (int i = 0; i < numHashBits; i++) {
        cout<<key[numHashBits-i];
      }
      cout<<endl;
      exit(1);
    }

    blockSize = block.size();
  }

  expect_next_token(pws_file, "NUM_Y", "compute_get_block_by_hash: expected NUM_Y");

  next_token_or_error(pws_file, cmds);
  mpq_t& nb = voc(cmds, temp_q);
  uint32_t numBits = mpz_get_ui(mpq_numref(nb));

  expect_next_token(pws_file, "Y", "compute_get_block_by_hash: expected Y");

  for (uint32_t i = 0; i < numBits; i++) {
    next_token_or_error(pws_file, cmds);
    mpq_t& val = voc(cmds, temp_q);

    // Pad with zeroes
    uint32_t bit = 0;
    if (i < blockSize) {
      bit = static_cast<int>(block[i]);
    }
    mpz_set_ui(mpq_numref(val), bit);
    mpq_canonicalize(val);
  }
}

void ComputationProver::compute_put_block_by_hash(FILE* pws_file) {
  char cmds[BUFLEN];

  HashBlockStore::Key key;
  parse_hash(pws_file, key, _ram->getNumHashBits());
  HashBlockStore::Value block;

  expect_next_token(pws_file, "NUM_X", "compute_put_block_by_hash: expected NUM_X");

  next_token_or_error(pws_file, cmds);
  mpq_t& nb = voc(cmds, temp_q);
  uint32_t numBits = mpz_get_ui(mpq_numref(nb));

  expect_next_token(pws_file, "X", "compute_put_block_by_hash: expected X");

  block.resize(numBits);

  //	gmp_printf("compute_put_block_by_hash: value=");
  for (uint32_t i = 0; i < numBits; i++) {
    next_token_or_error(pws_file, cmds);
    mpq_t& val = voc(cmds, temp_q);
    uint32_t bit = mpz_get_ui(mpq_numref(val));

    if (bit != 0 && bit != 1) {
      gmp_printf("ERROR: compute_put_block_by_hash: input is not a bit: input=%d, bit_index=%d\n", bit, i);
      exit(1);
    }

    //		gmp_printf("%d", bit);

    block[i] = static_cast<bool>(bit);
  }
  //	gmp_printf("\n");

  // Special case: if the key == 0, dont't store the block.
  if (key.any()) {
    _blockStore->put(key, block);
  }
}

void ComputationProver::compute_free_block_by_hash(FILE* pws_file) {
  HashBlockStore::Key key;
  parse_hash(pws_file, key, _ram->getNumHashBits());
  _blockStore->free(key);
}

void ComputationProver::compute_printf(FILE* pws_file) {
  char cmds[BUFLEN];

  std::string format;
  while(true){
    next_token_or_error(pws_file, cmds);
    if (strcmp(cmds, "NUM_X") == 0) {
      break;
    }
    format += cmds;
    format += " ";
  }
  next_token_or_error(pws_file, cmds);
  int num_args = atoi(cmds);

  expect_next_token(pws_file, "X", "Format error in printf");

  mpz_t args [10];
  if (num_args > 10){
    cout << "ERROR: Cannot have more than 10 arguments to prover's printf!" << endl;
    num_args = 10;
  }

  for(int i = 0; i < num_args; i++){
    next_token_or_error(pws_file, cmds);
    mpq_t& arg = voc(cmds, temp_q);
    mpz_init(args[i]);
    mpz_set_q(args[i], arg);
  }

  gmp_printf("PRINTF in computation_p %d:\n", num_args);
  gmp_printf(format.c_str(), args[0], args[1], args[2], args[3],
  args[4], args[5], args[6], args[7], args[8], args[9]);
  gmp_printf("\n");
}

void ComputationProver::compute_genericget(FILE* pws_file) {
  char cmds[BUFLEN];

  expect_next_token(pws_file, "COMMITMENT", "For now, only COMMITMENT is allowed in genericGET");

  expect_next_token(pws_file, "NUM_HASH_BITS", "Format error in genericget 1");

  next_token_or_error(pws_file, cmds);

  expect_next_token(pws_file, "HASH_IN", "Format error in genericget 2");

  HashBlockStore::Key key;
  parse_hash(pws_file, key, NUM_COMMITMENT_BITS);

  HashBlockStore::Value block;
  uint32_t blockSize = 0;

  // Special case: if the key == 0, don't fail. Just return zeroes.
  if (key.any()) {
    bool found = _blockStore->get(key, block);
    if (!found) {
      int numHashBits = NUM_COMMITMENT_BITS;

      std::string s;
      boost::to_string(key, s);
      gmp_printf("ERROR: compute_genericget: block not found: hash=\n");
      for (int i = 0; i < numHashBits; i++) {
        cout<<key[i];
      }
      cout<<endl;
      exit(1);
    }

    blockSize = block.size();
  }

  expect_next_token(pws_file, "NUM_Y", "compute_genericget: expected NUM_Y");

  next_token_or_error(pws_file, cmds);
  mpq_t& nb = voc(cmds, temp_q);
  uint32_t numBits = mpz_get_ui(mpq_numref(nb));

  expect_next_token(pws_file, "Y", "compute_genericget: expected Y");

  for (uint32_t i = 0; i < numBits; i++) {
    next_token_or_error(pws_file, cmds);
    mpq_t& val = voc(cmds, temp_q);

    // Pad with zeroes
    uint32_t bit = 0;
    if (i < blockSize) {
      bit = static_cast<int>(block[i]);
    }
    mpz_set_ui(mpq_numref(val), bit);
    mpq_canonicalize(val);
  }
}

/**
 * If str is the name of a variable, return a reference to that variable.
 * Otherwise, set use_if_constant to the constant variable held by the
 * string, and return it.
 *
 * The method name stands for "variable or constant"
 **/
//mpq_t& ComputationProver::voc(const std::string& str, mpq_t& use_if_constant) {
mpq_t& ComputationProver::voc(const char* str, mpq_t& use_if_constant) {
  int index;
  const char* name = str;
  if (name[0] == 'V') {
    index = atoi(name + 1);
    if (index < 0 || index >= num_vars) {
      gmp_printf("PARSE ERROR - variable %s\n",str);
      return use_if_constant;
    }
    return F1_q[F1_index[index]];
  } else if (name[0] == 'I') {
    index = atoi(name + 1);
    if (index < 0 || index >= size_input) {
      gmp_printf("PARSE ERROR - variable %s\n",str);
      return use_if_constant;
    }
    return input_output_q[index];
  } else if (name[0] == 'O') {
    index = atoi(name + 1);
    if (index < size_input || index >= size_input + size_output) {
      gmp_printf("PARSE ERROR - variable %s\n",str);
      return use_if_constant;
    }
    return input_output_q[index];
  }
  //Parse the rational constant
  mpq_set_str(use_if_constant, str, 10);
  return use_if_constant;
}

//void ComputationProver::compute_matrix_vec_mul(std::istream_iterator<std::string>& cmds) {
void ComputationProver::compute_matrix_vec_mul(FILE* pws_file) {
  char cmds[BUFLEN];

  expect_next_token(pws_file, "NUM_ROWS", "Invalid MATRIX_VEC_MUL, NUM_ROWS exected.");
  next_token_or_error(pws_file, cmds);
  int number_of_rows = atoi(cmds);

  expect_next_token(pws_file, "NUM_COLUMNS", "Invalid MATRIX_VEC_MUL, NUM_COLUMNS expected.");
  next_token_or_error(pws_file, cmds);
  int number_of_columns = atoi(cmds);

  expect_next_token(pws_file, "ACTUAL_NUM_COLUMNS", "Invalid MATRIX_VEC_MUL, ACTUAL_NUM_COLUMNS expected.");
  next_token_or_error(pws_file, cmds);
  int actual_number_of_columns = atoi(cmds);

  // read input bits from F1
  expect_next_token(pws_file, "IN_VEC", "Invalid MATRIX_VEC_MUL, IN_VEC expected.");

  mpq_t *input, *result;
  alloc_init_vec(&input, actual_number_of_columns);
  alloc_init_vec(&result, number_of_rows);

  for (int i = 0; i < actual_number_of_columns; i++) {
    next_token_or_error(pws_file, cmds);
    mpq_set(input[i], voc(cmds, temp_q));
  }

  // compute ggh hash and get output
  const uint32_t* row = AMat;
  mpq_t factor;
  mpq_t term;
  mpq_inits(factor, term, NULL);
  for (int i = 0; i < number_of_rows; i++, row += number_of_columns) {
    mpq_set_ui(result[i], 0, 1);
    for (int j = 0; j < actual_number_of_columns; j++) {
      mpq_set_ui(factor, row[j], 1);
      mpq_mul(term, factor, input[j]);
      mpq_add(result[i], result[i], term);
    }
  }
  mpq_clears(factor, term, NULL);

  // assign output to output variables
  expect_next_token(pws_file, "OUT_VEC", "Invalid MATRIX_VEC_MUL, IN_VEC expected.");

  for (int i = 0; i < number_of_rows; i++) {
    //mpq_t& output = voc(*cmds,temp_q); cmds++;
    next_token_or_error(pws_file, cmds);
    mpq_t& output = voc(cmds, temp_q);
    mpq_set(output, result[i]);
  }

  clear_del_vec(input, actual_number_of_columns);
  clear_del_vec(result, number_of_rows);
}
void ComputationProver::compute_waksman_network(FILE* pws_file) {
  char cmds[BUFLEN];

  expect_next_token(pws_file, "WIDTH", "Invalid WAKSMAN_NETWORK, WIDTH expected.");
  next_token_or_error(pws_file, cmds);
  int width = atoi(cmds);
  
  expect_next_token(pws_file, "INPUT", "WAKSMAN_NETWORK, INPUT expected.");
  int num_switches = 0;
  for (int i = 1; i <= width; i++) {
    num_switches += ceil(log2(i));
   }
  int num_intermediate = num_switches * 2 - width;
  //cout << num_switches << endl;
  data_t* input = new data_t[width];
  data_t* intermediate = new data_t[num_intermediate];
  data_t* output =  new data_t[width];
  switch_t* switches = new switch_t[num_switches];

  const int num_elements = 4;

  for (int i = 0; i < width; i++) {
    next_token_or_error(pws_file, cmds);
    // constant numbers are fine for voc.
    input[i].addr = mpz_get_si(mpq_numref(voc(cmds, temp_q)));
    next_token_or_error(pws_file, cmds);
    input[i].timestamp = mpz_get_si(mpq_numref(voc(cmds, temp_q)));
    next_token_or_error(pws_file, cmds);
    input[i].type = mpz_get_si(mpq_numref(voc(cmds, temp_q)));
    next_token_or_error(pws_file, cmds);
    input[i].value = mpz_get_si(mpq_numref(voc(cmds, temp_q)));
    //    gmp_printf("%d ", input[i].addr);
    //gmp_printf("%d ", input[i].timestamp);
    //gmp_printf("%d ", input[i].type);
    //gmp_printf("%d\n", input[i].value);
  }
  
  wak_route(input, intermediate, output, switches, width, num_switches);

  expect_next_token(pws_file, "INTERMEDIATE", "Invalid WAKSMAN_NETWORK, INTERMEDIATE expected.");
  next_token_or_error(pws_file, cmds);
  if (strcmp(cmds, "NULL") != 0) {
    int intermediate_offset = atoi(cmds+1);
    
    for (int i = 0; i < num_intermediate; i++) {
      mpq_set_si(F1_q[F1_index[intermediate_offset + i * num_elements + 0]], intermediate[i].addr, 1);
      mpq_set_si(F1_q[F1_index[intermediate_offset + i * num_elements + 1]], intermediate[i].timestamp, 1);
      mpq_set_si(F1_q[F1_index[intermediate_offset + i * num_elements + 2]], intermediate[i].type, 1);
      mpq_set_si(F1_q[F1_index[intermediate_offset + i * num_elements + 3]], intermediate[i].value, 1);
      
      //  gmp_printf("%d ", intermediate[i].addr);
      // gmp_printf("%d ", intermediate[i].timestamp);
      // gmp_printf("%d ", intermediate[i].type);
      // gmp_printf("%d\n", intermediate[i].value);
    }
  }

  expect_next_token(pws_file, "OUTPUT", "Invalid WAKSMAN_NETWORK, OUTPUT expected.");
  next_token_or_error(pws_file, cmds);
  int output_offset = atoi(cmds+1);

  for (int i = 0; i < width; i++) {
    mpq_set_si(F1_q[F1_index[output_offset + i * num_elements + 0]], output[i].addr, 1);
    mpq_set_si(F1_q[F1_index[output_offset + i * num_elements + 1]], output[i].timestamp, 1);
    mpq_set_si(F1_q[F1_index[output_offset + i * num_elements + 2]], output[i].type, 1);
    mpq_set_si(F1_q[F1_index[output_offset + i * num_elements + 3]], output[i].value, 1);
    
    // gmp_printf("%d ", output[i].addr);
    // gmp_printf("%d ", output[i].timestamp);
    // gmp_printf("%d ", output[i].type);
    // gmp_printf("%d\n", output[i].value);
  }

  expect_next_token(pws_file, "SWITCH", "Invalid BENES_NETWORK, SWITCH expected.");
  next_token_or_error(pws_file, cmds);

  int switches_offset = atoi(cmds+1);
  for (int i = 0; i < num_switches; i++) {
    if (switches[i].swap) {
      mpq_set_si(F1_q[F1_index[switches_offset + i]], 1, 1);
    } else {
      mpq_set_si(F1_q[F1_index[switches_offset + i]], 0, 1);
    }
    
    //    gmp_printf("%d\n", switches[i].swap);
  }  
}


/**
  The computation may elect to simply execute a PWS file (prover work sheet).
  This routine parses a PWS file (filename in a C-string) and parses it.
  */
void ComputationProver::compute_from_pws(const char* pws_filename) {
  std::ifstream cmdfile (pws_filename);
  FILE* pws_file = fopen(pws_filename, "r");

  if (pws_file == NULL) {
    gmp_printf("Couldn't open prover worksheet file.\n");
    return;
  }

  // do not read the whole pws file into memory.
  char tok[BUFLEN], cmds[BUFLEN];
  while(try_next_token(pws_file, tok) != EOF) {
    if (strcmp(tok, "P") == 0) {
      next_token_or_error(pws_file, cmds);
      mpq_t& Y = voc(cmds, temp_q);
      expect_next_token(pws_file, "=", "Invalid POLY");
      compute_poly(pws_file, 0);
      mpq_set(Y, temp_qs[0]);
    } else if (strcmp(tok, "<I") == 0) {
      compute_less_than_int(pws_file);
    } else if (strcmp(tok, "<F") == 0) {
      compute_less_than_float(pws_file);
    } else if (strcmp(tok, "!=") == 0) {
      //Not equals computation
      expect_next_token(pws_file, "M", "Invalid !=");
      next_token_or_error(pws_file, cmds);
      mpq_t& M = voc(cmds, temp_q);
      expect_next_token(pws_file, "X1", "Invalid !=");
      next_token_or_error(pws_file, cmds);
      mpq_t& X1 = voc(cmds, temp_q);
      expect_next_token(pws_file, "X2", "Invalid !=");
      next_token_or_error(pws_file, cmds);
      mpq_t& X2 = voc(cmds, temp_q2);
      expect_next_token(pws_file, "Y", "Invalid !=");
      next_token_or_error(pws_file, cmds);
      mpq_t& Y = voc(cmds, temp_q);
      if (mpq_equal(X1, X2)) {
        mpq_set_ui(M, 0, 1);
        mpq_set_ui(Y, 0, 1);
      } else {
        mpq_sub(temp_q, X1, X2);
        //f(a,b)^-1 = b*a^-1
        mpz_invert(temp, mpq_numref(temp_q), prime);
        mpz_mul(temp, temp, mpq_denref(temp_q));
        mpq_set_z(M, temp);
        mpq_set_ui(Y, 1, 1);
      }
    } else if (strcmp(tok, "/") == 0 || strcmp(tok, "/I") == 0 || strcmp(tok, "%I") == 0) {
      //Binary operation 
      next_token_or_error(pws_file, cmds);
      mpq_t& Y = voc(cmds, temp_q);
      expect_next_token(pws_file, "=", ("Invalid "+std::string(tok)).c_str());
      next_token_or_error(pws_file, cmds);
      mpq_t& X1 = voc(cmds, temp_q);
      expect_next_token(pws_file, tok, ("Invalid "+std::string(tok)).c_str());
      next_token_or_error(pws_file, cmds);
      mpq_t& X2 = voc(cmds,temp_q2);
      if (strcmp(tok, "/") == 0) {
        //Exact division
        if (mpq_sgn(X2) != 0) {
          mpq_div(Y,X1,X2);
        }
      } else if (strcmp(tok, "/I") == 0) {
        if (mpq_sgn(X2) != 0) {
          mpz_tdiv_q(mpq_numref(Y), mpq_numref(X1), mpq_numref(X2));
          mpz_set_ui(mpq_denref(Y),1);
        }
      } else if (strcmp(tok, "%I") == 0) {
        if (mpq_sgn(X2) != 0) {
          mpz_tdiv_r(mpq_numref(Y), mpq_numref(X1), mpq_numref(X2));
          mpz_set_ui(mpq_denref(Y),1);
        }
      }
    } else if (strcmp(tok, "SI") == 0) {
      //Split into bits (big endian, see implementation for format)
      compute_split_unsignedint(pws_file);
    } else if (strcmp(tok, "SIL") == 0) {
      compute_split_int_le(pws_file);
    } else if (strcmp(tok, "MATRIX_VEC_MUL") == 0) {
      compute_matrix_vec_mul(pws_file);
    } else if (strcmp(tok, "DB_GET_BITS") == 0) {
      compute_db_get_bits(pws_file);
    } else if (strcmp(tok, "DB_PUT_BITS") == 0) {
      compute_db_put_bits(pws_file);
    } else if (strcmp(tok, "DB_GET_SIBLING_HASH") == 0) {
      compute_db_get_sibling_hash(pws_file);
    } else if (strcmp(tok, "GET_BLOCK_BY_HASH") == 0) {
      compute_get_block_by_hash(pws_file);
    } else if (strcmp(tok, "PUT_BLOCK_BY_HASH") == 0) {
      compute_put_block_by_hash(pws_file);
    } else if (strcmp(tok, "FREE_BLOCK_BY_HASH") == 0) {
      compute_free_block_by_hash(pws_file);
    } else if (strcmp(tok, "GENERICGET") == 0) {
      compute_genericget(pws_file);
    } else if (strcmp(tok, "PRINTF") == 0) {
      compute_printf(pws_file);
    } else if (strcmp(tok, "ASSERT_ZERO") == 0) {
      next_token_or_error(pws_file, cmds);
      mpq_t& Y = voc(cmds, temp_q);
      mpz_t Z; mpz_init(Z);
      convert_to_z(Z, Y, prime);
      std::string var(cmds);
      if (mpz_sgn(Z) != 0){
        cout << "ASSERT_ZERO FAILED: " << var << " is " << mpz_get_str(NULL, 10, Z) << endl;
      }
    } else if (strcmp(tok, "RAMGET_FAST") == 0) {
      compute_fast_ramget(pws_file);
    } else if (strcmp(tok, "RAMPUT_FAST") == 0) {
      compute_fast_ramput(pws_file);
    } else if (strcmp(tok, "BENES_NETWORK") == 0) {
      //compute_benes_network(pws_file);
    } else if (strcmp(tok, "WAKSMAN_NETWORK") == 0) {
      compute_waksman_network(pws_file);
    } else if (strcmp(tok, "EXO_COMPUTE") == 0) {
      compute_exo_compute(pws_file);
    } else if (strcmp(tok, "EXT_GADGET") == 0) {
      compute_ext_gadget(pws_file);
    } else {
      gmp_printf("Unrecognized token: %s\n", tok);
    }
  }

  // convert output_q to output
  convert_to_z(size_input, input, input_q, prime);
  convert_to_z(size_output, output, output_q, prime);

  // convert F1_q to F1
  convert_to_z(num_vars, F1, F1_q, prime);
  

}
