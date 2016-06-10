#include <stdint.h>
struct In {int32_t address_write; uint32_t value; int32_t address_read; };
struct Out {uint32_t value; };

/*
  Writes a value to a specified address, then reads the value at another
  specified address.

  For now, the getdb and putdb functions assume a single database.
  Eventually, they will take a compile time resolvable handle to
  a database.
*/
void compute(struct In *input, struct Out *output){
  putdb(input->address_write, input->value);
  output->value = getdb(input->address_read);
}
