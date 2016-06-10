#include <stdint.h>
#include <db.h>
#include <avl_tree.h>

#include "cql_rw_update.h"

int compute(struct In *input, struct Out *output) {
  Student_handle_t handle;
  tree_t KEY_index;
  tree_t Average_index;
  struct {Student_result_t student[1];} results;
  int num_results;

  hashget(&handle, &(input->db_handle));
  KEY_index.root = handle.KEY_index;

  uint32_t path_depth = 0, encoded_tree_path = 0;
  CQL("UPDATE Student SET Honored = 1 WHERE KEY = 90", path_depth, encoded_tree_path);

  handle.KEY_index = KEY_index.root;
  hashput(&(output->db_handle), &handle);

  output->path_depth = path_depth;
  output->tree_path = encoded_tree_path;
  return 0;
}
