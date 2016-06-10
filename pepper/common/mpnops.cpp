#include "utility.h"
#include "mpnops.h"

#if 1

#define TWO_ARITY_RET_FN(cls, ret, type1, type2, fun, proxy)  \
  template <> ret cls::                                       \
  fun(type1 op1, type2 op2)                                   \
  { return proxy(op1, op2); }

#define TWO_ARITY_FN(cls, type1, type2, fun, proxy) \
  template <> void cls::                            \
  fun(type1 op1, type2 op2)                         \
  { proxy(op1, op2); }

#define BIN_FN(cls, fun, proxy)                     \
  template <> void cls::                            \
  fun(DestType rop, SourceType op1, SourceType op2) \
  { proxy(rop, op1, op2); }

#define UNARY_FN(cls, fun, proxy) TWO_ARITY_FN(cls, DestType, SourceType, fun, proxy)

#define SELF_FN(cls, fun, proxy)  \
  template <> void cls::          \
  fun(DestType rop)               \
  { proxy(rop); }


#define MPZ_BIN_FN(fun, proxy) BIN_FN(mpz_ops, fun, proxy)
#define MPZ_UNARY_FN(fun, proxy) UNARY_FN(mpz_ops, fun, proxy)
#define MPZ_SELF_FN(fun, proxy) SELF_FN(mpz_ops, fun, proxy)

MPZ_BIN_FN(add, mpz_add);
MPZ_BIN_FN(sub, mpz_sub);
MPZ_BIN_FN(mul, mpz_mul);
MPZ_BIN_FN(div, mpz_div);

MPZ_SELF_FN(init, mpz_init);
MPZ_SELF_FN(clear, mpz_clear);

MPZ_UNARY_FN(set, mpz_set);
MPZ_UNARY_FN(init_set, mpz_init_set);

TWO_ARITY_FN(mpz_ops, DestType, uint64_t, set_ui, mpz_set_ui);
TWO_ARITY_FN(mpz_ops, DestType, int64_t,  set_si, mpz_set_si);
TWO_ARITY_RET_FN(mpz_ops, int, SourceType, SourceType, cmp, mpz_cmp);


#define MPQ_BIN_FN(fun, proxy) BIN_FN(mpq_ops, fun, proxy)
#define MPQ_UNARY_FN(fun, proxy) UNARY_FN(mpq_ops, fun, proxy)
#define MPQ_SELF_FN(fun, proxy) SELF_FN(mpq_ops, fun, proxy)

MPQ_BIN_FN(add, mpq_add);
MPQ_BIN_FN(sub, mpq_sub);
MPQ_BIN_FN(mul, mpq_mul);
MPQ_BIN_FN(div, mpq_div);

MPQ_SELF_FN(init, mpq_init);
MPQ_SELF_FN(clear, mpq_clear);

MPQ_UNARY_FN(set, mpq_set);

template <> void mpq_ops::
init_set(DestType n, SourceType val)
{ mpq_init(n); mpq_set(n, val); }

TWO_ARITY_RET_FN(mpq_ops, int, SourceType, SourceType, cmp, mpq_cmp);

template <> void mpq_ops::
set_ui(DestType n, uint64_t val)
{ mpq_set_ui(n, val, 1); }

template <> void mpq_ops::
set_si(DestType n, int64_t val)
{ mpq_set_si(n, val, 1); }

template class mpn_ops<mpz_t>;
template class mpn_ops<mpq_t>;
#else
// Define table for mpz_t
void mpz_t_init(mpz_t n) { mpz_set_ui(n, 0); }
void mpz_alloc_init(mpz_t n) { mpz_init_set_ui(n, 0); }

template <> mpz_ops::zeroary_fn mpz_ops::alloc_init = mpz_alloc_init;//alloc_init_scalar;
template <> mpz_ops::zeroary_fn mpz_ops::init = mpz_t_init;
template <> mpz_ops::zeroary_fn mpz_ops::clear = mpz_clear;

template <> mpz_ops::unary_fn mpz_ops::set     = mpz_set;
template <> mpz_ops::unary_ui_fn mpz_ops::set_ui  = mpz_set_ui;
template <> mpz_ops::unary_si_fn mpz_ops::set_si  = mpz_set_si;

template <> mpz_ops::binary_fn mpz_ops::add = mpz_add;
template <> mpz_ops::binary_fn mpz_ops::sub = mpz_sub;
template <> mpz_ops::binary_fn mpz_ops::mul = mpz_mul;
template <> mpz_ops::binary_fn mpz_ops::div = mpz_div;

template <> mpz_ops::cmp_fn mpz_ops::cmp = mpz_cmp;

// Define table for mpq_t
void mpq_t_init(mpq_t n) { mpq_set_ui(n, 0, 1); }
void mpq_alloc_init(mpq_t n) { mpq_init(n); mpq_t_init(n); }

void mpq_set_int_ui(mpq_t n, uint64_t val) { mpq_set_ui(n, val, 1); }
void mpq_set_int_si(mpq_t n, int64_t val) { mpq_set_si(n, val, 1); }

template <> mpq_ops::zeroary_fn mpq_ops::alloc_init = mpq_alloc_init;
template <> mpq_ops::zeroary_fn mpq_ops::init = mpq_t_init;
template <> mpq_ops::zeroary_fn mpq_ops::clear = mpq_clear;

template <> mpq_ops::unary_fn mpq_ops::set        = mpq_set;
template <> mpq_ops::unary_ui_fn mpq_ops::set_ui  = mpq_set_int_ui;
template <> mpq_ops::unary_si_fn mpq_ops::set_si  = mpq_set_int_si;

template <> mpq_ops::binary_fn mpq_ops::add = mpq_add;
template <> mpq_ops::binary_fn mpq_ops::sub = mpq_sub;
template <> mpq_ops::binary_fn mpq_ops::mul = mpq_mul;
template <> mpq_ops::binary_fn mpq_ops::div = mpq_div;

template <> mpq_ops::cmp_fn mpq_ops::cmp = mpq_cmp;
#endif

