
#ifndef CODE_PEPPER_COMMON_MPNCLASS_H_
#define CODE_PEPPER_COMMON_MPNCLASS_H_

#include "mpnops.h"

// This is a quick class that does memory management for a mpz_t or mpq_t---
// kind of like a smart pointer. It does not attempt to replicate all of the
// functionality of the mpn_class classes that GMP provides. I just want
// automatic memory management and interoperability with the GMP C bindings.
template<typename T>
class MPNClass
{

public:
  typedef typename mpn_ops<T>::SourceType SourceType;
  typedef typename mpn_ops<T>::DestType DestType;
  typedef SourceType const_pointer;
  typedef DestType pointer;
  typedef T value_type;

private:
  value_type val;

public:

  MPNClass();
  explicit MPNClass(SourceType value);
  MPNClass(const MPNClass<T>& other);

  ~MPNClass();

  MPNClass<T>& operator=(const MPNClass<T>& other);

  operator pointer       ()       { return ptr(); }
  operator const_pointer () const { return ptr(); }

  pointer       ptr()       { return val; }
  const_pointer ptr() const { return val; }

  bool operator==(const MPNClass<T>& other) const;
  bool operator!=(const MPNClass<T>& other) const;
};

typedef MPNClass<mpz_t> MPZClass;
typedef MPNClass<mpq_t> MPQClass;

#endif
