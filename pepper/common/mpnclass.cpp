#include "mpnclass.h"

template <typename T> MPNClass<T>::
MPNClass()
{
  mpn_ops<T>::init(ptr());
}

template <typename T> MPNClass<T>::
MPNClass(SourceType value)
{
  mpn_ops<T>::init_set(ptr(), value);
}

template <typename T> MPNClass<T>::
MPNClass(const MPNClass<T>& other)
{
  mpn_ops<T>::init_set(ptr(), other.ptr());
}

template <typename T> MPNClass<T>::
~MPNClass()
{
  mpn_ops<T>::clear(ptr());
}

template <typename T> MPNClass<T>& MPNClass<T>::
operator=(const MPNClass<T>& other)
{
  if (this != &other)
  {
    mpn_ops<T>::set(ptr(), other.ptr());
  }
  return *this;
}

template <typename T> bool MPNClass<T>::
operator==(const MPNClass<T>& other) const
{
  return mpn_ops<T>::cmp(ptr(), other.ptr()) == 0;
}

template <typename T> bool MPNClass<T>::
operator!=(const MPNClass<T>& other) const
{
  return !operator==(other);
}

template class MPNClass<mpz_t>;
template class MPNClass<mpq_t>;

