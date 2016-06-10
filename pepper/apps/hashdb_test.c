#include <stdint.h>
#include <db.h>

#define SIZE 16

struct In {uint32_t value; };
struct Out {uint32_t valueaft; int status;};

/*
  Convert a simple liked list node to its hash, and then reconstruct the
  node using that hash, using hashputdb and hashgetdb and library functions
  in db.h .
*/
struct llnode {uint32_t value; hash_t next;};

void compute(struct In *input, struct Out *output){
  struct llnode p;

  p.value = input->value;
  p.next = *NULL_HASH; //List of length 1.

  {
    hash_t hash_of_p;
    hashput(&hash_of_p, &p);
    p.value = -1;
    hashget(&p, &hash_of_p); //Should re-set p.value = input->value.
  }
  
  output->valueaft = p.value;
  //Check that the hash is maintained
  output->status = hasheq(&(p.next), NULL_HASH);
}
