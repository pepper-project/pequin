#include <storage/configurable_block_store.h>
#include <apps/tolling.h>


int set_tuple(mpq_t* input_q, int inp, t_tuple_t* t_tuple, char *argv[]){
  mpq_set_si(input_q[inp++], t_tuple->time, 1);
  mpq_set_ui(input_q[inp++], t_tuple->toll_booth_id, 1);
  mpq_set_ui(input_q[inp++], t_tuple->toll, 1);
  //Return the new offset in the variables list
  return inp;
}

void tolling_input_gen (mpq_t * input_q, int num_inputs) {

    char db_file_path[BUFLEN];
    snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/prover_%s", FOLDER_STATE, "default_shared_db");
    ConfigurableBlockStore bs(db_file_path);
    int i;

    hash_t hash;
    commitment_t commitment;
    struct pathdb db;
    for(i = 0; i < MAX_TUPLES; i++){
        db.path[i].time = i;
        db.path[i].toll_booth_id = (i % 5);
        db.path[i].toll = 50; //50 cents per toll always
    }

    //CK bit string acting like a salt
    commitmentCK_t CK = {{
            //Randomly generated.
            0x8a, 0xf7, 0x24, 0xa1, 0x58,
            0xc9, 0x8b, 0x89, 0x29, 0x85,
            0xce, 0xa1, 0xae, 0xc3, 0x42,
            0x6e, 0xbb, 0x86, 0x56, 0x37
        }};
    setcommitmentCK(&CK);
    hashput2(&bs, &hash, &db);
    commitmentput2(&bs, &commitment, &hash);

    //Position in input variables list
    int inp = 0;


    for(i = 0; i < NUM_CK_BITS/8; i++){
        mpq_set_ui(input_q[inp++], CK.bit[i], 1);
    }

    for(i = 0; i < NUM_COMMITMENT_CHUNKS; i++){
        mpq_set_ui(input_q[inp++], commitment.bit[i], 1);
    }

    //Fill in the rest of the input (verifier's inputs)
    for(i = 0; i < MAX_SPOTCHECKS; i++){
        //Choose the ith tuple in the db
        inp = set_tuple(input_q, inp, &(db.path[i]));
    }

    //Set the time threshold
    mpq_set_si(input_q[inp++], 2, 1);

    if (inp > num_inputs){
        std::cerr << "ERROR: Wrong num_inputs" << std::endl;
    }
}
