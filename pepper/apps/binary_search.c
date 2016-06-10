#include <stdint.h>
//The number of rounds of binary search to perform
//equivalently, the number of bits in the representation of SIZE
//So, for 8 - 15, logSize is 4, for 16 - 31, logSize is 5, etc.
#define logSIZE 5
#define SIZE 16

struct In {uint32_t array[SIZE]; uint32_t search;};
struct Out {int32_t present; int32_t index; };

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
  int32_t i, min, max, avg;
  uint32_t got;
  //First write the array to the databse (linear time)
  for(i = 0; i < SIZE; i++){
    ramput(i, &(input->array[i]));
  }

  //Now search (log time)
  min = 0;
  max = SIZE-1;
  output->present = 0;
  for(i = 0; i < logSIZE; i++){
    avg = (min + max) >> 1;
    ramget(&got, avg);
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
