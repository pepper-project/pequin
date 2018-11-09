#include <apps/ptrchase_merkle.h>
#include <storage/ram_impl.h>
#include <storage/hasher.h>
#include <storage/configurable_block_store.h>

void ptrchase_merkle_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {

    char db_file_path[BUFLEN];
    snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/prover_%s", FOLDER_STATE, "default_shared_db");
    ConfigurableBlockStore bs(db_file_path);
    RAMImpl ram(&bs);
    HashType* hash = ram.getRootHash();

    int i = 0;
    for (HashType::HashVec::const_iterator itr = hash->GetFieldElts().begin();
         itr != hash->GetFieldElts().end(); ++itr) {
        mpz_set(mpq_numref(input_q[i]), (*itr).get_mpz_t());
        mpq_canonicalize(input_q[i]);
        i++;
    }

  for (int j = 0; j < NELMS-1; j++) {
      mpq_set_ui(input_q[i+j], j+1, 1);
  }
  mpq_set_ui(input_q[i+NELMS-1], 0, 1);

  for (int j = i+NELMS; j<num_inputs; j++) {
      mpq_set_ui(input_q[j], 0, 1);
  }
}
