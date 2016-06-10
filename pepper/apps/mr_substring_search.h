#ifndef _MR_SUBSTRING_SEARCH_H
#define _MR_SUBSTRING_SEARCH_H

#include <stdint.h>

#define BOOLEAN_IMPL 0
#define STD_IMPL 1
#define WORD_ALIGNED_IMPL 2

#define CURRENT_IMPL STD_IMPL

//The sharing is that all mappers get the same needles, but different
//haystacks (so there is one "super haystack" that all mappers take a
//chunk from)
#define NUM_MAPPERS 10
#define NUM_REDUCERS 1

#define NUM_NEEDLES 1

//These are both in nucleotides
#define SIZE_NEEDLE 4
#define SIZE_HAYSTACK 600000

//Packing factor - 32 nucleotides in a 64bit int
#define NUCS_PER_INT 32
#define SIZE_HAYSTACK_INTS ((SIZE_HAYSTACK + NUCS_PER_INT - 1) / NUCS_PER_INT)
#define SIZE_NEEDLE_INTS ((SIZE_NEEDLE + NUCS_PER_INT - 1) / NUCS_PER_INT)

//The haystack is stored serverside, the needles are clientside.
//(But the client has a hash of the stored serverside data.)
#define HAS_SERVERSIDE_INPUT

#if CURRENT_IMPL == BOOLEAN_IMPL
typedef struct _ServersideIn {
  bool haystack[SIZE_HAYSTACK*2];
} ServersideIn;

typedef struct _ClientsideIn {
  bool needle[NUM_NEEDLES][SIZE_NEEDLE*2];
} ClientsideIn;
#else
typedef struct _ServersideIn {
  uint64_t haystack[SIZE_HAYSTACK_INTS];
} ServersideIn;

typedef struct _ClientsideIn {
  uint64_t needle[NUM_NEEDLES][SIZE_NEEDLE_INTS];
} ClientsideIn;
#endif

typedef struct _MapperIn {
  ServersideIn serverside_in; //always serverside first.
  ClientsideIn clientside_in;
} MapperIn;

// actual output of the mapper
typedef struct _MapperChunkOut {
  int32_t position[NUM_NEEDLES];
} MapperChunkOut;

typedef struct _MapperOut {
  MapperChunkOut output[NUM_REDUCERS];
} MapperOut;

typedef struct _ReducerChunkIn {
  //Position is in nucleotides
  int32_t position[NUM_NEEDLES];
} ReducerChunkIn;

typedef struct _ReducerIn {
  ReducerChunkIn input[NUM_MAPPERS];
} ReducerIn;

typedef struct _ReducerOut {
  //Position is in nucleotides
  int32_t position[NUM_NEEDLES];
} ReducerOut;

#endif
