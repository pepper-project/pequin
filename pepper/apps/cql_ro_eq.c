#include <stdint.h>
#include <db.h>
#include <avl_tree.h>

#include "cql_ro_eq.h"

int compute(struct In *input, struct Out *output) {
  tree_t Class_index;

  Class_index.root = input->handle.Class_index;

  CQL("SELECT KEY, FName, LName, Age, Major, State, PhoneNum, Class, Credits, Average, Honored FROM Student WHERE Class = 2009 LIMIT 5", output->result);
  return 0;
}
