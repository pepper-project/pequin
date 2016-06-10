#include <stdint.h>
#include <db.h>
#include <avl_tree.h>

#include "cql_ro_range.h"

int compute(struct In *input, struct Out *output) {
  Student_handle_t handle;
  tree_t Age_index;

  hashget(&handle, &(input->db_handle));
  Age_index.root = handle.Age_index;

  CQL("SELECT KEY, FName, LName, Age, Major, State, PhoneNum, Class, Credits, Average, Honored FROM Student WHERE Age > 20 AND Age < 27 LIMIT 5", output->result);
  return 0;
}
