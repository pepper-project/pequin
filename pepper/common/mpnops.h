#ifndef CODE_PEPPER_COMMON_MPNOPS_H_
#define CODE_PEPPER_COMMON_MPNOPS_H_

#include <stdint.h>
#include <gmp.h>

template <typename T> struct MPNTypes;

template <>
struct MPNTypes<mpz_t>
{
  typedef mpz_ptr pointer;
  typedef mpz_srcptr const_pointer;
  typedef __mpz_struct c_type;
};

template <>
struct MPNTypes<mpq_t>
{
  typedef mpq_ptr pointer;
  typedef mpq_srcptr const_pointer;
  typedef __mpq_struct c_type;
};

// Singleton of function pointers to GMP functions. This simulates a vtable
// to fake inheritance for templated types.
template<typename T>
class mpn_ops
{
  private:
    // Workaround. For the type void (*binary_fn)(T, const T, const T), the
    // compiler will for some reason ignore the const modifiers when
    // compiling.
    struct make_const { typedef const T type; };

    // Private constructor. Should not initialize.
    mpn_ops();

  public:
    typedef typename MPNTypes<T>::const_pointer SourceType;
    typedef typename MPNTypes<T>::pointer DestType;

#if 1
    static void add(DestType, SourceType, SourceType);
    static void sub(DestType, SourceType, SourceType);
    static void mul(DestType, SourceType, SourceType);
    static void div(DestType, SourceType, SourceType);

    static void init(DestType);
    static void clear(DestType);

    static void init_set(DestType, SourceType);
    static void set(DestType, SourceType);
    static void set_ui(DestType, uint64_t);
    static void set_si(DestType, int64_t);

    static int cmp(SourceType, SourceType);

#else
    typedef void (*binary_fn)(DestType, SourceType, SourceType);
    static binary_fn add;
    static binary_fn sub;
    static binary_fn mul;
    static binary_fn div;

    typedef void (*zeroary_fn)(DestType);
    static zeroary_fn alloc_init;
    static zeroary_fn init;
    static zeroary_fn clear;

    typedef void (*unary_fn)(DestType, SourceType);
    static unary_fn set;

    typedef void (*unary_ui_fn)(DestType, uint64_t);
    static unary_ui_fn set_ui;

    typedef void (*unary_si_fn)(DestType, int64_t);
    static unary_si_fn set_si;

    typedef int (*cmp_fn)(SourceType, SourceType);
    static cmp_fn cmp;
#endif
};

typedef mpn_ops<mpz_t> mpz_ops;
typedef mpn_ops<mpq_t> mpq_ops;
#endif  // CODE_PEPPER_COMMON_MPNOPS_H_
