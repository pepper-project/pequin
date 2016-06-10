#include <crypto/prng.h>

Prng Prng::global(PNG_CHACHA);

Prng::Prng(int type) {
  init_chacha_png();
}

Prng::~Prng() {

  delete []random_state;
}

Prng::Prng(int type, u8 *key, u8 *iv) {
  memcpy(this->key, (char*)key, (size_t)(CHACHA_KEY_SIZE/8));
  memcpy(this->iv, (char*)iv, (size_t)(CHACHA_IV_SIZE/8));
  init_chacha_png(key, iv);
}

void Prng::get_seed(u8 *key, u8 *iv) {
  memcpy(key, (char*)(this->key), (size_t)(CHACHA_KEY_SIZE/8));
  memcpy(iv, (char*)(this->iv), (size_t)(CHACHA_IV_SIZE/8));
}

int Prng::get_type() {
  return type;
}

void Prng::get_random(mpz_t m, const mpz_t n) {
  chacha_urandom(m, n);
}

void Prng::get_randomb(mpz_t m, int nbits) {
  chacha_urandomb(m, nbits);
}

void Prng::get_randomb(char *buf, int nbits) {
  chacha_urandomb(buf, nbits);
}

void Prng::init_chacha_png() {
  u8 key[CHACHA_KEY_SIZE];
  u8 iv[CHACHA_IV_SIZE];

  ifstream rand;

  rand.open("/dev/urandom", ifstream::in);
  rand.read((char*)&key, (size_t)(CHACHA_KEY_SIZE/8));
  rand.read((char*)&iv, (size_t)(CHACHA_IV_SIZE/8));
  rand.close();

  init_chacha_png(key, iv);
  return;
}

void Prng::init_chacha_png(u8 *key, u8 *iv) {
  chacha = (ECRYPT_ctx*)aligned_malloc(sizeof(ECRYPT_ctx));
  random_state = new u8[RANDOM_STATE_SIZE];

  ECRYPT_keysetup(chacha, key, CHACHA_KEY_SIZE, CHACHA_IV_SIZE);
  ECRYPT_ivsetup(chacha, iv);

  chacha_refill_randomness();

  return;
}

// generates random number using chacha random
void Prng::chacha_urandom(mpz_t m, const mpz_t n) {
  // figure out numbers of bits in n
  int nbits = int(mpz_sizeinbase(n, 2));

  // loop until m < n
  do {
    chacha_urandomb(m, nbits);
  } while (mpz_cmp(m, n) >= 0);
}

// generates random bits using one big call to chacha and keeping state
void Prng::chacha_urandomb(mpz_t m, int nbits) {

  // determine number of bytes
  int nbytes = ceil(double(nbits)/8.0);
  int diff = (nbytes <<3) - nbits;

  // check that we have enough randomness
  if ((RANDOM_STATE_SIZE-random_index) < nbytes)
    chacha_refill_randomness();

  // convert raw to mpz_t
  fast_mpz_import(m, &random_state[random_index], nbytes);
  //mpz_import(m, nbytes, 1, sizeof(char), 0, 0, &random_state[random_index]);

  // update index
  random_index += nbytes;

  // remove extra bits if needed
  if (diff != 0)
    mpz_fdiv_q_2exp(m, m, diff);
}

// generates random bits using one big call to chacha and keeping state
void Prng::chacha_urandomb(char *buf, int nbits) {
  if (nbits == 0)
    return;

  // determine number of bytes
  int nbytes = ceil(double(nbits)/8.0);
  int diff = (nbytes <<3) - nbits;

  // check that we have enough randomness
  if ((RANDOM_STATE_SIZE-random_index) < nbytes)
    chacha_refill_randomness();

  if (diff == 0) {
    memcpy(buf, &random_state[random_index], nbytes);
  } else {
    memcpy(buf, &random_state[random_index], nbytes - 1);
    char byte = random_state[random_index + nbytes - 1];
    byte &= 0xFF >> (8 - diff);
    byte |= buf[nbytes - 1] & (0xFF << diff);
    buf[nbytes - 1] = byte;
  }

  // update index
  random_index += nbytes;
}

// generate new random state
void Prng::chacha_refill_randomness() {
  ECRYPT_keystream_bytes(chacha, random_state, RANDOM_STATE_SIZE);
  random_index = 0;
}
