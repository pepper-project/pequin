#include <cassert>
#include <storage/ggh_hash.h>
#include <storage/gghA.h>

// Parameters used in the submitted version
//static const int N = 64;
//static const uint32_t Q = 4096; // Q = N^2
//static const int Q_BITS = 12;
////static const int M = 1536; // 2Nlog(Q) -- 2:1 compression ratio
//static const int M = 5376; // 7Nlog(Q) -- 7:1 compression ratio

// Parameters used in the camera-ready version in response to Chris Peikert's suggestions
static const int N = 64;
static const uint32_t Q = 524288;
static const int Q_BITS = 19;
static const int M = 7296; // 6Nlog(Q) -- 6:1 compression ratio

static const int OUTPUT_BITS = N*Q_BITS;
static const int NUM_ELTS = OUTPUT_BITS / HashType::FIELD_SIZE;


static Bits concatBits(const Bits& b1, const Bits& b2) {
  Bits b(b1.size() + b2.size());
  int index = 0;

  for (uint32_t i = 0; i < b1.size(); i++) {
    b[index++] = b1[i];
  }
  for (uint32_t i = 0; i < b2.size(); i++) {
    b[index++] = b2[i];
  }

  return b;
}

static void copyIVBits(Bits& ivBits) {
  for (int i = 0; i < OUTPUT_BITS/8; i++) {
    uint8_t ivByte = GGHIV[i];
    for (int j = 0; j < 8; j++) {
      ivBits[i*8 + j] = (ivByte >> j) & 1;
    }
  }
}

static void copyLengthBits(Bits& b, int start, uint64_t length) {
  for (int i = 0; i < 64; i++) {
    b[start + i] = (length >> i) & 1;
  }
}

static void copyBits(Bits& dst, int dstStart, const Bits& src, int srcStart, int numBits) {
  for (int i = 0; i < numBits; i++) {
    dst[dstStart + i] = src[srcStart + i];
  }
}

static void hashBlock(const Bits& input, int inputSize, Bits& result) {
  assert(inputSize <= M);

  int rIndex = 0;
  const uint32_t* row = AMat;

  for (int i = 0; i < N; i++, row += M) {
    uint32_t rowSum = 0;

    // after testing different ways to perform this taks including:
    // 1. change the branch to conditional move (CMOV assembly instruction)
    // 2. change %= to mask
    // 3. use OpenMP to parallelize this
    // We decide to keep this implementation because
    // 1. it seems that the branch predictor is doing a good job, the
    // additional cost of CMOV will not outperform the cost of flushing the pipeline
    // caused by branch misprediction.
    // 2. %= Q will be optimized to &= 0x0fff automatically.
    // 3. We can use OpenMP at a higher level to achieve parallelism at a
    // larger granularity to better mask the overhead of multithreading.
    for (int j = 0; j < inputSize; j++) {
      if (input[j]) {
        rowSum += row[j];
      }
    }

    // Instead of doing a modular reduction everytime, a final reduction is
    // enough because 7296 * 524288 is less than the maximum of a uint32_t
    // which means that it will never overflow during the accumulation.
    rowSum %= Q;

    for (int j = 0; j < Q_BITS; j++) {
      result[rIndex++] = (rowSum >> j) & 1;
    }
  }
}

// Hash the blocks according to using a prefix-free construction from
// "Merkle-Damgard Revisited: how to Construct a Hash Function" by Coron et al.
//
// Prepend the 64-bit length, add a 1 bit at the end, and implictly pad the rest
// of the last block with 0s. Prepending the length is actually more secure than
// the traditional Merkle-Damgard construction because it yields a prefix-free
// encoding. But, it's usually undesirable because the length must be known in
// advance, making it unsuitable for streaming applications. But since we need
// to know everything at compile-time anyway, it works for us.
static Bits hashBits(const Bits& input) {
  Bits blockBits(M);
  Bits resultBits(OUTPUT_BITS);
  copyIVBits(resultBits);
  bool moreBlocks = true;
  bool firstBlock = true;
  int inputCopied = 0;

  while (moreBlocks) {
    uint32_t blockCopied = 0;

    copyBits(blockBits, blockCopied, resultBits, 0, OUTPUT_BITS);
    blockCopied += OUTPUT_BITS;

    // prepend the length
    if (firstBlock) {
      copyLengthBits(blockBits, blockCopied, input.size());
      blockCopied += 64;
      firstBlock = false;
    }

    int numInputBits = input.size() - inputCopied;
    int spaceLeft = blockBits.size() - blockCopied;
    if (numInputBits > spaceLeft) {
      numInputBits = spaceLeft;
    }

    if (numInputBits > 0) {
      copyBits(blockBits, blockCopied, input, inputCopied, numInputBits);
      inputCopied += numInputBits;
      blockCopied += numInputBits;
    }

    // add a 1 bit to the end (the rest of the block is implicitly padded with 0s)
    if (blockCopied < blockBits.size()) {
      blockBits.set(blockCopied);
      blockCopied++;
      moreBlocks = false;
    }

    hashBlock(blockBits, blockCopied, resultBits);
  }

  return resultBits;
}

GGHHash::GGHHash() {
}

GGHHash::~GGHHash() {
}

int GGHHash::getNumHashBits() {
  return OUTPUT_BITS;
}

HashType* GGHHash::createHash(const Bits& hashBits) {
  return new HashType(hashBits, NUM_ELTS);
}

Bits GGHHash::hash(const Bits& v) {
  return hashBits(v);
}

Bits GGHHash::hash(const Bits& left, const Bits& right) {
  return hashBits(concatBits(left, right));
}
