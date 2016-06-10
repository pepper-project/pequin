#include "stdint.h"
#include "db.h"

#include "face_detect_hamming.h"

num_t hdist_scalar(int32_t a, int32_t b){
  num_t dist = 0;

  int i;
  a ^= b;
  for(i = 0; i < 32; i++){
    if ((a >> i)&1){
      dist++;
    }
  }

  return dist;
}

num_t hdist(int32_t* face1, int32_t* face2){
  num_t dist = 0;
  int i;
  for(i = 0; i < LENGTH_FACE/32; i++){
    dist += hdist_scalar(face1[i], face2[i]);
  }
  return dist;
}

int face_detect(struct FaceDB* db, VerifierSideIn* verifier_in, struct Out* output){
  int i;
  output->match_found = 0;
  for(i = 0; i < NUM_FACES; i++){
    num_t dist = hdist(verifier_in->target, db->faces[i].data);
    if (dist <= db->faces[i].threshold){
      output->match_found = 1;
    }
  }
}

int compute(struct In *input, struct Out *output) {
  struct FaceDB db;

  //hashget(&db, &(input->digest_of_db));
  setcommitmentCK(&input->commitmentCK);
  hash_t hash;
  commitmentget(&hash, &(input->digest_of_db));
  hashget(&db, &hash);

  return face_detect(&db, &(input->verifier_in), output);
}
