#include <stdint.h>
#include <db.h>
#include <avl_tree.h>

#include "cql_ro_threshold.h"

int compute(struct In *input, struct Out *output) {
  tree_t Average_index;

  Average_index.root = input->handle.Average_index;

  CQL("SELECT KEY, FName, LName, Age, Major, State, PhoneNum, Class, Credits, Average, Honored FROM Student WHERE Average >= 90 LIMIT 5", output->result);
  return 0;
}
