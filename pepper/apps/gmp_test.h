#ifndef APPS_SFDL_GMP_TEST_H
#define APPS_SFDL_GMP_TEST_H

#include <gmp.h>
#include <stdint.h>

struct In {
  int32_t data[4];
};
struct Out {
  mpz_t b;
};

#endif //APPS_SFDL_GMP_TEST_H
