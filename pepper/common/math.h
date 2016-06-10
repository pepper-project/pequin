
#ifndef CODE_PEPPER_COMMON_MATH_H_
#define CODE_PEPPER_COMMON_MATH_H_

#include <gmp.h>

template<typename IntType> int
fls(IntType t)
{
  if (t == 0)
    return 0;

  int out = 1;
  while (t >>= 1) out++;
  return out;
}

/*
 * Computes ceil(log2(t)). Returns 0 if t <= 0.
 */
template<typename IntType> IntType
log2i(IntType t)
{
  if (t <= 0)
    return 0;

  return fls(t-1);
}

template<typename IntType> IntType
pow2i(IntType t)
{
  if (t < 0)
    return 0;
  return 1 << t;
}

template<typename IntType> IntType
powi(IntType base, IntType exp)
{
  IntType ret = 1;
  for (IntType i = 0; i < exp; i++)
    ret *= base;
  return ret;
}

template<typename IntType> IntType
divRoundUp(const IntType& val, const IntType& div)
{
  return (val + div - 1) / div;
}

template<typename IntType> IntType
iRoundDown(const IntType& val, const IntType& align)
{
  return val - (val % align);
}

template<typename IntType> IntType
iRoundUp(const IntType& val, const IntType& align)
{
  if (val < 0)
    return iRoundDown(val - (align - 1), align);
  else
    return iRoundDown(val + (align - 1), align);
}


void addmul_si(mpz_t rop, const mpz_t op1, const long op2);
void modmult   (mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime);
void modmult_si(mpz_t rop, const mpz_t op1, const long op2,  const mpz_t prime);
void addmodmult(mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime);
void addmodmult_ui(mpz_t rop, const mpz_t op1, const unsigned long op2, const mpz_t prime);
void addmodmult_si(mpz_t rop, const mpz_t op1, const long op2, const mpz_t prime);
void modadd(mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime);
void modsub(mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime);
void one_sub(mpz_t rop, const mpz_t op);
void mpqMod(mpq_t val, const mpz_t prime);

void mle      (mpz_t rop, const mpz_t pt, const mpz_t val0, const mpz_t val1, const mpz_t prime);
void mle_si   (mpz_t rop, const long pt,  const mpz_t val0, const mpz_t val1, const mpz_t prime);
void mulmle   (mpz_t rop, const mpz_t pt, const mpz_t val0, const mpz_t val1, const mpz_t prime);
void mulmle_si(mpz_t rop, const long pt,  const mpz_t val0, const mpz_t val1, const mpz_t prime);

#endif
