#ifndef CODE_PEPPER_CRYPTO_PRNG_H_
#define CODE_PEPPER_CRYPTO_PRNG_H_
#include <iostream>
#include <stdlib.h>
#include <string.h>
#include <gmp.h>

#include <sys/time.h>
#include <sys/resource.h>
#include <sys/types.h>
#include <stdint.h>
#include <vector>
#include <algorithm>
#include <math.h>
#include <common/utility.h>
#include <fstream>

// chacha is a C library
extern "C" {
#include <ecrypt-sync.h>
}

#define PNG_CHACHA 3
#define CHACHA_KEY_SIZE 256
#define CHACHA_IV_SIZE 64

// must be factor of mp_bits_per_limb for split()
#define RANDOM_STATE_SIZE (3000*mp_bits_per_limb/8)

using std::cout;
using std::endl;
using std::string;
using std::vector;
using std::ifstream;

class Prng {
  private:
    unsigned char key[256];
    unsigned char iv[64];

    int type;
    ECRYPT_ctx *chacha;
    u8 *random_state;
    int random_index;

    void init_mt_png();
    void init_chacha_png();
    void init_chacha_png(u8 *key, u8 *iv);
    void chacha_urandom(mpz_t m, const mpz_t n);
    void chacha_urandomb(mpz_t m, int nbits);
    void chacha_urandomb(char *buf, int nbits);

  public:
    Prng(int type);
    Prng(int type, u8 *key, u8 *iv);
    ~Prng();
    int get_type();
    void get_seed(u8 *key, u8 *iv);
    void get_random(mpz_t m, const mpz_t n);
    void get_randomb(mpz_t m, int nbits);
    void get_randomb(char *buf, int nbits);
    void chacha_refill_randomness();

    // Used when some random code needs a good prng.
    static Prng global;
};
#endif  // CODE_PEPPER_CRYPTO_PRNG_H_
