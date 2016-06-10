#ifndef _DB_SHARED_H
#define _DB_SHARED_H

#include <stdint.h>

#define TRUE 1
#define FALSE 0

#define DB_SIZE_OFFSET  0
#define DB_NUM_OF_ROW_OFFSET 1
#define DB_DATA_OFFSET 2

#define NUM_HASH_CHUNKS DB_HASH_NUM_BITS/64

typedef struct _hash {
	uint64_t bit[NUM_HASH_CHUNKS];
} hash_t;

#define MAX_NUM_OF_COLUMNS 10

typedef struct db_handle {
  hash_t KEY_index_root;
  hash_t columns_index_root[MAX_NUM_OF_COLUMNS];
} db_handle_t;

#define NUM_COMMITMENT_CHUNKS 32
const int NUM_COMMITMENT_BITS = NUM_COMMITMENT_CHUNKS*8;

const int NUM_CK_BITS = 160; //TODO Is this enough bits for CK?

typedef struct _commitmentCK {
  uint8_t bit[NUM_CK_BITS/8];
} commitmentCK_t;

typedef uint32_t commitment_hash_word_t;
typedef struct _commitment {
	uint8_t bit[NUM_COMMITMENT_CHUNKS];
} commitment_t;

#define NUM_COMMITMENT_WORDS_PER_BLOCK 16
const int NUM_COMMITMENT_BITS_PER_BLOCK = NUM_COMMITMENT_WORDS_PER_BLOCK*32;

typedef struct _commitment_hash_block {
  commitment_hash_word_t words[NUM_COMMITMENT_WORDS_PER_BLOCK];
} commitment_hash_block_t;


#endif /* db_shared.h */
