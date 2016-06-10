#include <stdint.h>
#include <db.h>

struct In { commitment_t digest; };
struct Out { hash_t result; };

/*
  Microbenchmark to measure the cost of commitmentget.
*/
void compute(struct In *input, struct Out *output){
  commitmentget(output, &input->digest);
}
