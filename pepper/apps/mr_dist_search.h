#ifndef _MR_DIST_SEARCH_H
#define _MR_DIST_SEARCH_H

#include <stdint.h>

#define NUM_MAPPERS 10
#define NUM_REDUCERS 1

#define SIZE_HAYSTACK 20000
#define SIZE_NEEDLE 10

// actual input to the mapper
typedef struct _MapperIn {
  uint32_t haystack[SIZE_HAYSTACK];
  uint32_t needle[SIZE_NEEDLE];
} MapperIn;

// actual output of the mapper
typedef struct _MapperChunkOut {
  int output[SIZE_HAYSTACK/SIZE_NEEDLE];
} MapperChunkOut;

typedef struct _MapperOut {
  MapperChunkOut output[NUM_REDUCERS];
} MapperOut;

typedef struct _ReducerChunkIn { 
  int input[SIZE_HAYSTACK/SIZE_NEEDLE]; 
} ReducerChunkIn;

typedef struct _ReducerIn {
  ReducerChunkIn input[NUM_MAPPERS];
} ReducerIn;

typedef struct _ReducerOut {
  int output;
} ReducerOut;



#endif
