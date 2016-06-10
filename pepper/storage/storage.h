#ifndef STORAGE_H_
#define STORAGE_H_

#include <cassert>

#include <boost/dynamic_bitset.hpp>

typedef boost::dynamic_bitset<> Bits;

inline void BitsToByteArray(void *byte_arr, uint32_t size_byte, const
Bits& bits, int offset_input) {
  assert (bits.size() >= size_byte * 8 + offset_input);
  for (uint32_t i = 0; i < size_byte; i++) {
    uint8_t* chunk = &(((uint8_t*)byte_arr)[i]);
    *chunk = 0;
    for (uint32_t j = 0; j < 8; j++) {
      uint8_t num = (uint8_t)((((uint8_t)bits[i * 8 + j + offset_input]) << j));
      *chunk = (*chunk | num);
    }
  }
}

inline void BitsToByteArray(void *byte_arr, uint32_t size_byte, const
Bits& bits) {
  BitsToByteArray(byte_arr, size_byte, bits, 0);
}

inline void BitsToByteArrayReverse(void *byte_arr, uint32_t size_byte, const Bits& bits) {
  uint32_t size_bit = size_byte * 8;
  assert (bits.size() >= size_bit);
  for (uint32_t i = 0; i < size_byte; i++) {
    uint8_t* chunk = &(((uint8_t*)byte_arr)[i]);
    *chunk = 0;
    for (uint32_t j = 0; j < 8; j++) {
      *chunk |= (bits[size_bit - (i * 8 + j) - 1] << j);
    }
  }
}

inline void ByteArrayToBits(const void *byte_arr, uint32_t size_byte, Bits& bits, int offset_output) {
  assert (bits.size() >= offset_output + size_byte * 8);
  for (uint32_t i = 0; i < size_byte; i++) {
    uint8_t* chunk = &(((uint8_t*)byte_arr)[i]);
    for (uint32_t j = 0; j < 8; j++) {
      bits[i * 8 + j + offset_output] = (((*chunk) >> j) & 1);
    }
  }
}

inline void ByteArrayToBits(const void *byte_arr, uint32_t size_byte, Bits& bits) {
  ByteArrayToBits(byte_arr,size_byte,bits,0);
}

#endif /* STORAGE_H_ */
