#ifndef _DB_H
#define _DB_H

//Most of the definition of db.h can be shared with the exogenous code,
#include "db_shared.h"

#define BOOL int

hash_t __NULL_HASH__;
hash_t* NULL_HASH = &__NULL_HASH__;

#if DB_NUM_ADDRESSES < DB_THR_NUM_ADDRESSES_NAIVE 
#define USE_NAIVE_MEMORY
int memory[DB_NUM_ADDRESSES];

// reads and writes to global memory
// if addr is beyond DB_NUM_ADDRESSES, these functions do not throw an
// error
void init_memory() {
  uint32_t i;
  for (i=0; i<DB_NUM_ADDRESSES; i++) {
    memory[i] = i;
  } 
}

void ramget_naive(int *val, uint32_t addr) {
  uint32_t i;
  for (i=0; i<DB_NUM_ADDRESSES; i++) {
    if (i == addr) {
      *val = memory[i];
    }
  }
}

void ramput_naive(uint32_t addr, int *value) {
  uint32_t i;
  for (i=0; i<DB_NUM_ADDRESSES; i++) {
    if (i == addr) {
      memory[i] = *value;
    }
  }
}
#endif

void ramget_hybrid(int *val, uint32_t addr) {
  #ifdef USE_NAIVE_MEMORY
  ramget_naive(val, addr);
  #else
  ramget(val, addr);
  #endif
}

void ramput_hybrid(uint32_t addr, int *val) {
  #ifdef USE_NAIVE_MEMORY
  ramput_naive(addr, val);
  #else
  ramput(addr, val);
  #endif
}

int hasheq(hash_t* a, hash_t* b){
  int i;
  int isEq = 1;
  for(i = 0; i < (DB_HASH_NUM_BITS/64); i++){
    if (a->bit[i] != b->bit[i]){
      isEq = 0;
    }
  }
  return isEq;
}

#define strcpy(dst, src) \
   { \
       int tempI; \
       for (tempI = 0; tempI < sizeof(src); tempI++) { \
           dst[tempI] = src[tempI]; \
       } \
   }

void wordToBytes(uint8_t* dest, commitment_hash_word_t val){
  int i = 0;
  for(i = 0; i < 4; i++){
    dest[i] = (val >> (8*i)) & 0xFF ;
  }
}

//Rotate x m many positions right, in x's big endian encoding.
uint32_t RROT(uint32_t x, uint32_t m){
  return ((x >> m) | (x << (32 - m))) & 0xFFFFFFFF;
}

//Computes hash := H (m1 || m2 || m3)
//both messages are assumed to have lengths (in bits) of a multiple of 8
void commitment_hash_H(
  uint8_t* m1,
  uint32_t num_bits_m1,
  uint8_t* m2,
  uint32_t num_bits_m2,
  uint8_t* m3,
  uint32_t num_bits_m3,
  commitment_t* hash
  ){
  //Currently an implementation of SHA-256
  assert_zero(num_bits_m1 % 8);
  assert_zero(num_bits_m2 % 8);
  assert_zero(num_bits_m3 % 8);
  uint32_t num_bytes_m1 = num_bits_m1 / 8;
  uint32_t num_bytes_m2 = num_bits_m2 / 8;
  uint32_t num_bytes_m3 = num_bits_m3 / 8;

  //Initialize hash values:
  //(first 32 bits of the fractional parts of the square roots of the first
  //8 primes 2..19):
  commitment_hash_word_t
    h0 = 0x6a09e667,
    h1 = 0xbb67ae85,
    h2 = 0x3c6ef372,
    h3 = 0xa54ff53a,
    h4 = 0x510e527f,
    h5 = 0x9b05688c,
    h6 = 0x1f83d9ab,
    h7 = 0x5be0cd19;

  commitment_hash_word_t k [64] = {
0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1,
0x923f82a4, 0xab1c5ed5,
   0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74,
0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
   0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f,
0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
   0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3,
0xd5a79147, 0x06ca6351, 0x14292967,
   0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354,
0x766a0abb, 0x81c2c92e, 0x92722c85,
   0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819,
0xd6990624, 0xf40e3585, 0x106aa070,
   0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3,
0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
   0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa,
0xa4506ceb, 0xbef9a3f7, 0xc67178f2
  };

  uint32_t i,j,u,v;
  uint32_t byteptr = 0;

  //Add one byte to add 0x80 to the end of the message
  uint64_t length_bits = (uint64_t)(num_bits_m1 + num_bits_m2 +
  num_bits_m3);
  uint64_t num_blocks =
  ((length_bits + 8 + 64) + (NUM_COMMITMENT_BITS_PER_BLOCK - 1)) /
    (NUM_COMMITMENT_BITS_PER_BLOCK);

  for(i = 0; i < num_blocks; i++){
    commitment_hash_word_t w [64];
    //copy block into first words of w
    for(j = 0; j < NUM_COMMITMENT_WORDS_PER_BLOCK; j++){
      w[j] = 0;
      for(u = 0; u < 4; u++){
        //What is the byteptr'th byte of the padded message?
        uint8_t cbyte = 0;
        if (byteptr < num_bits_m1/8){
          cbyte = m1[byteptr];
        } else if (byteptr < (num_bits_m1+num_bits_m2)/8){
          cbyte = m2[byteptr - num_bits_m1/8];
        } else if (byteptr < (length_bits)/8){
          cbyte = m3[byteptr - (num_bits_m1 + num_bits_m2)/8];
        } else if (byteptr == (length_bits)/8){
          cbyte = 0x80;
        } else if (byteptr >= num_blocks * NUM_COMMITMENT_BITS_PER_BLOCK / 8 - 8) {
          int bytes_into_length = byteptr - (num_blocks *
          NUM_COMMITMENT_BITS_PER_BLOCK / 8 - 8);
          cbyte = (length_bits >> (bytes_into_length*8)) & 0xFF;
        }

        byteptr++;
        //FIXME Hacks. Without the hacks below, the compiler is not
        //producing the expected result! Please remove these hacks when
        //the compiler has been fixed!
        w[j] = (uint32_t)(w[j] | ((((uint8_t)cbyte)) << (u*8)));
        //The following should work, but doesn't (?)
        //w[j] |= cbyte << (u*8);
      }
    }
    //state machine remaining words of w
    for(j = NUM_COMMITMENT_WORDS_PER_BLOCK; j < 64; j++){
      commitment_hash_word_t s0 =
    RROT(w[j-15],7) ^ RROT(w[j-15],18) ^ (w[j-15] >> 3);

      commitment_hash_word_t s1 =
    RROT(w[j-2],17) ^ RROT(w[j-2],19) ^ (w[j-2] >> 10);

      w[j] = (w[j-16] + s0 + w[j-7] + s1) & 0xFFFFFFFF;
      //printf("RROT DEBUG %d %d %d\n", x, m, shiftr);
    }

    commitment_hash_word_t
      a = h0,
      b = h1,
      c = h2,
      d = h3,
      e = h4,
      f = h5,
      g = h6,
      h = h7;

    //Compression main loop
    for(j = 0; j < 64; j++){
      commitment_hash_word_t S1 =
        RROT(e,6) ^ RROT(e,11) ^ RROT(e,25);

      commitment_hash_word_t ch =
        (e & f) ^ ((~e) & g);

      commitment_hash_word_t temp1 =
        (h + S1 + ch + k[j] + w[j]) & 0xFFFFFFFF;

      commitment_hash_word_t S0 =
        RROT(a, 2) ^ RROT(a,13) ^ RROT(a,22);

      commitment_hash_word_t maj =
        (a & b) ^ (a & c) ^ (b & c);

      commitment_hash_word_t temp2 =
        (S0 + maj) & 0xFFFFFFFF;

      h = g;
      g = f;
      f = e;
      e = (e + temp1) & 0xFFFFFFFF;
      d = c;
      c = b;
      b = a;
      a = (temp1 + temp2) & 0xFFFFFFFF;
    }

    //Add chunk to current hash value
    h0 = (h0 + a) & 0xFFFFFFFF;
    h1 = (h1 + b) & 0xFFFFFFFF;
    h2 = (h2 + c) & 0xFFFFFFFF;
    h3 = (h3 + d) & 0xFFFFFFFF;
    h4 = (h4 + e) & 0xFFFFFFFF;
    h5 = (h5 + f) & 0xFFFFFFFF;
    h6 = (h6 + g) & 0xFFFFFFFF;
    h7 = (h7 + h) & 0xFFFFFFFF;
  }

  //Copy h0 through h7 to hash
  wordToBytes(hash->bit + 0, h0);
  wordToBytes(hash->bit + 4, h1);
  wordToBytes(hash->bit + 8, h2);
  wordToBytes(hash->bit + 12, h3);
  wordToBytes(hash->bit + 16, h4);
  wordToBytes(hash->bit + 20, h5);
  wordToBytes(hash->bit + 24, h6);
  wordToBytes(hash->bit + 28, h7);
}

//Acts like a salt
commitmentCK_t __db_commitment_CK;
void setcommitmentCK(commitmentCK_t* in){
  int i;
  for(i = 0; i < NUM_CK_BITS/8; i++){
    __db_commitment_CK.bit[i] = in->bit[i];
  }
}

void commitment_hash(
  uint8_t* message,
  int num_bits_message,
  uint8_t* key, //key is always NUM_COMMITMENT_BITS_PER_BLOCK long
  commitment_t* hash
  ){
  //compute oprod and iprod
  uint8_t o_key [NUM_COMMITMENT_BITS_PER_BLOCK/8];
  uint8_t i_key [NUM_COMMITMENT_BITS_PER_BLOCK/8];
  int i;
  for(i = 0; i < NUM_COMMITMENT_BITS_PER_BLOCK/8; i++){
    o_key[i] = 0x5c ^ key[i];
    i_key[i] = 0x36 ^ key[i];
  }

  uint8_t placeholder [1];
  //commitment_hash_H currently supports 3 bytestrings, but we don't
  //always need that many. Use placeholder as a "null string"

  //call commitment_hash_H once
  commitment_t inner_hash;
  commitment_hash_H(i_key, NUM_COMMITMENT_BITS_PER_BLOCK, 
  __db_commitment_CK.bit, NUM_CK_BITS,
  message, num_bits_message, &inner_hash);
  //call it again
  commitment_hash_H(
  o_key, NUM_COMMITMENT_BITS_PER_BLOCK,
  inner_hash.bit, NUM_COMMITMENT_BITS,
  placeholder, 0,
  hash);
}

#endif /* db.h */
