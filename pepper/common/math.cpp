#include <cstdlib>

#include "math.h"

void
addmul_si(mpz_t rop, const mpz_t op1, const long op2)
{
  if (op2 < 0)
    mpz_submul_ui(rop, op1, -op2);
  else
    mpz_addmul_ui(rop, op1, op2);
}

void
modmult(mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime)
{
    mpz_mul(rop, op1, op2);
    mpz_mod(rop, rop, prime);
}

void
modmult_si(mpz_t rop, const mpz_t op1, const long op2, const mpz_t prime)
{
    mpz_mul_si(rop, op1, op2);
    mpz_mod(rop, rop, prime);
}

void
addmodmult(mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime)
{
    mpz_addmul(rop, op1, op2);
    mpz_mod(rop, rop, prime);
}

void
addmodmult_ui(mpz_t rop, const mpz_t op1, const unsigned long op2, const mpz_t prime)
{
    mpz_addmul_ui(rop, op1, op2);
    mpz_mod(rop, rop, prime);
}

void
addmodmult_si(mpz_t rop, const mpz_t op1, const long op2, const mpz_t prime)
{
    addmul_si(rop, op1, op2);
    mpz_mod(rop, rop, prime);
}

void
modadd(mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime)
{
    mpz_add(rop, op1, op2);
    mpz_mod(rop, rop, prime);
}

void
modsub(mpz_t rop, const mpz_t op1, const mpz_t op2, const mpz_t prime)
{
    mpz_sub(rop, op1, op2);
    mpz_mod(rop, rop, prime);
}


void
one_sub(mpz_t rop, const mpz_t op)
{
  // Be careful in case op and rop are the same number.
  mpz_neg(rop, op);
  mpz_add_ui(rop, rop, 1);
}

void
mpqMod(mpq_t val, const mpz_t prime)
{
  mpz_mod(mpq_numref(val), mpq_numref(val), prime);
  mpz_mod(mpq_denref(val), mpq_denref(val), prime);
  mpq_canonicalize(val);
}

// Computes the univariate mle of the function
//   f(0) = val0
//   f(1) = val1
// at the point pt.
void
mle_si(mpz_t rop, const long pt, const mpz_t val0, const mpz_t val1, const mpz_t prime)
{
  mpz_t tmp;
  mpz_init_set_ui(tmp, 1);

  mulmle_si(tmp, pt, val0, val1, prime);
  mpz_set(rop, tmp);

  mpz_clear(tmp);
}

void
mulmle_si(mpz_t rop, const long pt, const mpz_t val0, const mpz_t val1, const mpz_t prime)
{
  switch (pt)
  {
    case 0:
      modmult(rop, rop, val0, prime);
      break;
    case 1:
      modmult(rop, rop, val1, prime);
      break;
    default:
      mpz_t tmp;
      mpz_init(tmp);

      mpz_mul_si(tmp, val0, 1 - pt);
      addmodmult_si(tmp, val1, pt, prime);

      modmult(rop, rop, tmp, prime);

      mpz_clear(tmp);
      break;
  }
}

// Computes the univariate mle of the function
//   f(0) = val0
//   f(1) = val1
// at the point pt.
void
mle(mpz_t rop, const mpz_t pt, const mpz_t val0, const mpz_t val1, const mpz_t prime)
{
  mpz_t tmp;
  mpz_init_set_ui(tmp, 1);

  mulmle(tmp, pt, val0, val1, prime);
  mpz_set(rop, tmp);

  mpz_clear(tmp);
}

void
mulmle(mpz_t rop, const mpz_t pt, const mpz_t val0, const mpz_t val1, const mpz_t prime)
{
  mpz_t tmp;
  mpz_init(tmp);

  // Compute (1 - pt) * val0 + pt * val1
  one_sub(tmp, pt);
  mpz_mul(tmp, tmp, val0);
  mpz_addmul(tmp, pt, val1);

  modmult(rop, rop, tmp, prime);

  mpz_clear(tmp);
}

