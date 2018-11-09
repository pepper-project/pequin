#include <apps/pure_hashget.h>
#include <storage/configurable_block_store.h>
#include <storage/ram_impl.h>
void pure_hashget_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {

  struct In input;
  struct Out output;
  char db_file_path[BUFLEN];
  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/prover_%s", FOLDER_STATE, "default_shared_db");
  HashBlockStore* bs = new ConfigurableBlockStore(db_file_path);
  MerkleRAM* ram = new RAMImpl(bs);
  setBlockStoreAndRAM(bs, ram);

  srand(time(NULL));
  for (int i = 0; i < NUM_OF_BLOCKS; i++) {
    for (int j = 0; j < BLOCKLEN; j++) {
      output.blocks[i].block[j] = rand();
    }
    hashput(&(input.hashes[i]), &(output.blocks[i]));
    hashget(&(output.blocks[i]), &(input.hashes[i]));
  }
  uint64_t* input_ptr = (uint64_t*)&input;
  for (int i = 0; i < num_inputs; i++) {
    mpq_set_ui(input_q[i], input_ptr[i], 1);
  }
  deleteBlockStoreAndRAM();

}
