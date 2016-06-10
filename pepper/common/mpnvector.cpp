#include "mpnvector.h"
#include "mpnops.h"
#include "utility.h"

#include <iostream>
#include <cassert>
using namespace std;

template<typename T>
MPNVector<T>::
MPNVector(size_t s) : len(s), vec(NULL) {
  if (len > 0)
    alloc_init_vec(&vec, len);
}

template<typename T>
MPNVector<T>::
MPNVector(const MPNVector<T>& other) {
  len = other.size();
  alloc_init_vec(&vec, len);

  for (size_t i = 0; i < len; i++)
    mpn_ops<T>::set(vec[i], other[i]);
}

template<typename T>
MPNVector<T>::
~MPNVector() {
  clear_del_vec(vec, len);
}

template<typename T>
void MPNVector<T>::
set(int index, SourceType val) {
  mpn_ops<T>::set(vec[index], val);
}

template<typename T>
void MPNVector<T>::
fillSi(int64_t val) {
  for (size_t i = 0; i < len; i++)
    mpn_ops<T>::set_si(vec[i], val);
}

template<typename T>
void MPNVector<T>::
fill(SourceType val) {
  for (size_t i = 0; i < len; i++)
    mpn_ops<T>::set(vec[i], val);
}

template<typename T>
void MPNVector<T>::
copy(const MPNVector<T>& other) {
  copy(other, 0, other.size(), 0);
}

template<typename T>
void MPNVector<T>::
copy(const MPNVector<T>& other, size_t startOther, size_t len, size_t startThis) {
  len = min(len, min(size() - startThis, other.size() - startOther));
  for (unsigned i = 0; i < len; i++)
    mpn_ops<T>::set(vec[startThis + i], other[startOther + i]);
}

template<typename T>
MPNVector<T>& MPNVector<T>::
operator*=(SourceType factor) {
  for (size_t i = 0; i < size(); i++)
    mpn_ops<T>::mul(vec[i], vec[i], factor);
  return *this;
}

template<typename T> MPNVector<T>& MPNVector<T>::
operator=(const MPNVector<T>& other) {
  if (this != &other) {
    clear_del_vec(vec, len);
    alloc_init_vec(&vec, other.len);
    len = other.len;

    for (size_t i = 0; i < size(); i++)
      mpn_ops<T>::set(vec[i], other[i]);
  }
  return *this;
}

template<typename T> bool MPNVector<T>::
operator==(const MPNVector<T>& other)
{
  if (size() != other.size())
    return false;

  for (size_t i = 0; i < size(); i++)
  {
    if (mpn_ops<T>::cmp(vec[i], other[i]) != 0)
      return false;
  }
  return true;
}

template<typename T> bool MPNVector<T>::
operator!=(const MPNVector<T>& other)
{
  return !operator==(other);
}

template<typename T>
void MPNVector<T>::
resize(size_t s) {
  if (size() == s)
    return;

  T* newVec;
  alloc_init_vec(&newVec, s);

  const size_t numCopies = min(size(), s);
  for (size_t i = 0; i < numCopies; i++)
    mpn_ops<T>::set(newVec[i], vec[i]);

  clear_del_vec(vec, len);
  vec = newVec;
  len = s;
}

template<typename T>
void MPNVector<T>::
reserve(size_t s) {
  if (s <= len)
    return;

  size_t newSize = ceil(max<double>(len * 1.5, s));
  resize(newSize);
}

template class MPNVector<mpz_t>;
template class MPNVector<mpq_t>;

