#include <stdint.h>
//The number of rounds of binary search to perform
//equivalently, the number of bits in the representation of SIZE
//So, for 8 - 15, logSize is 4, for 16 - 31, logSize is 5, etc.
#define logSIZE 5
#define SIZE 16

struct In {uint16_t array[SIZE]; uint16_t search;};
struct Out {uint16_t present; uint16_t index; };

/*
  Search a sorted array for a value in logarithmic time using getdb /
  logdb. Whether or not the value is found, an index is returned where
  the value could be inserted into the list so that the list would still be
  sorted.

  For now, the getdb and putdb functions assume a single database.
  Eventually, they will take a compile time resolvable handle to
  a database.
*/
void compute(struct In *input, struct Out *output){
  uint16_t i, min, max, avg;
  uint16_t got;
  //First write the array to the databse (linear time)
  /*for(i = 0; i < SIZE; i++){*/
    /*ramput_fast(i, input->array[i]);*/
  /*}*/

  //Now search (log time)
  min = 0;
  max = SIZE-1;
  output->present = 0;
  for(i = 0; i < logSIZE; i++){
    avg = (min + max) >> 1;
    got = input->array[avg];
    if (got == input->search){
      min = avg;
      max = avg;
      output->present = 1;
    } else {
      if (input->search < got){
        max = avg;
      } else {
        min = avg;
      }
    }
  }
  
  output->index = min;
}

