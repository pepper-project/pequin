/*
  Given a database of NUM_FACES bitstrings
  each of length LENGTH_FACE, and for each face a threshold
  t_i,
  And on input x, a bitstring of length LENGTH_FACE,
  return 1 if there is some face i in the database where
    HammingDist(x, face_i) \le t_i
  and 0 otherwise.
*/

//Constants
#define NUM_FACES 128
#define LENGTH_FACE 928

typedef int32_t num_t;

struct Face {
  int32_t data[LENGTH_FACE/32];
  num_t threshold;
};

struct FaceDB {
  struct Face faces[NUM_FACES];
};

typedef struct VerifierSideIn {
  int32_t target[LENGTH_FACE/32];
} VerifierSideIn;

struct In {
  commitmentCK_t commitmentCK;
  commitment_t digest_of_db;
  VerifierSideIn verifier_in;
};

struct Out {
  int match_found;
};
