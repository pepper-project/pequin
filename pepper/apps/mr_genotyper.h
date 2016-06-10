#ifndef _MR_GENOTYPER_H
#define _MR_GENOTYPER_H

#include <stdint.h>
#include <fix_t.h>

//Scale this up.
#define NUM_LOCI_PER_MAPPER 100
//60x coverage is considered "gold standard"
//but this is often unrealistic - experimenters usually don't have the
//resources to collect 60x coverage. Something < 8 is realistic.
#define MAX_SIZE_PILEUP 8

//Scale this up
#define NUM_MAPPERS 10 

//Must == 1
#define NUM_REDUCERS 1

//Describes a Phred quality score.
//The probability of erroneous reading is 10^(-q/10)
//where q is the phred score.
typedef uint8_t phred_score_t;

//One of the four nucleotides, listed below.
typedef uint8_t nucleotide_base_t;

const nucleotide_base_t A = 0, T = A + 1, G = T + 1, C = G + 1;

//Describes a single nucleotide read - the base and Phred quality
//are given
typedef struct _nucleotide_read{
  phred_score_t quality;
  nucleotide_base_t base;
} nucleotide_read;
//
//Describes a "pileup", a vector of different reads at the
//same loci across multiple sequencing runs
typedef struct _sequence_pileup {
  nucleotide_read pileup[MAX_SIZE_PILEUP];
} sequence_pileup;

const int NUM_GENOTYPES = 10;
const nucleotide_base_t GENOTYPES[NUM_GENOTYPES][2] = {
  {A, A},
  {A, C},
  {A, G},
  {A, T},
  {C, C},
  {C, G},
  {C, T},
  {G, G},
  {G, T},
  {T, T}
};

typedef uint8_t single_locus_genotype_t;

//Describes a call that the genotyper makes for a particular locus.
typedef struct _single_locus_genotype_call{
  //Which of the 10 diploid single-locus genotypes the genotyper calls
  //for this locus
  single_locus_genotype_t call;
  //The quality (log10 of confidence) the genotyper has in its call
  fix_t quality;
} single_locus_genotype_call;

const fix_t LODthresh = 3 * FIX_SCALE;

// actual input to the mapper
typedef struct _MapperIn {
  nucleotide_base_t refseq [NUM_LOCI_PER_MAPPER];
  sequence_pileup data[NUM_LOCI_PER_MAPPER];
} MapperIn;

// actual output of the mapper
typedef struct _MapperChunkOut {
  single_locus_genotype_call calls[NUM_LOCI_PER_MAPPER];
} MapperChunkOut;

typedef struct _MapperOut {
  MapperChunkOut output[NUM_REDUCERS];
} MapperOut;

typedef struct _ReducerChunkIn {
  single_locus_genotype_call calls[NUM_LOCI_PER_MAPPER];
} ReducerChunkIn;

typedef struct _ReducerIn {
  ReducerChunkIn input[NUM_MAPPERS];
} ReducerIn;

typedef struct _ReducerOut {
  //Simply concatenate the calls
  single_locus_genotype_call calls[NUM_LOCI_PER_MAPPER * NUM_MAPPERS];
} ReducerOut;

#endif
