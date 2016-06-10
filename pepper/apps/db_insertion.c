#include <stdint.h>

typedef struct data {
	uint32_t data[8];
} data_t;

struct In {
	uint32_t address;
	data_t d_in;
};

struct Out {
	data_t d_out;
};

/*
  Increments the value at address input->address of the database
  using getdb and putdb (special language features)

  For now, the getdb and putdb functions assume a single database.
  Eventually, they will take a compile time resolvable handle to
  a database.
*/
void compute(struct In *input, struct Out *output) {
	ramput(input->address, &(input->d_in));
	ramget(&(output->d_out), input->address);
}
