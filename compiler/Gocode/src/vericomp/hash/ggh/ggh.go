package ggh

import (
	. "vericomp"
	. "vericomp/hash"
	"math"
)

// Parameters used in the submitted version
//const N = 64
//const Q = 4096 // Q = N^2 
//const Q_BITS = 12
////const M = 1536 // 2Nlog(Q) -- 2:1 compression ratio
//const M = 5376 // 7Nlog(Q) -- 7:1 compression ratio

// Parameters used in the camera-ready version in response to Chris Peikert's suggestions
const N = 64
const Q = 524288 
const Q_BITS = 19
const M = 7296 // 6Nlog(Q) -- 6:1 compression ratio

const _OUTPUT_BITS = N*Q_BITS
const _NUM_ELTS = _OUTPUT_BITS / HASH_FIELD_BITS

type ggh struct {
	cb *ConstraintBuilder
	A [][]string
	ivBits []string
}

func NewGGH(cb *ConstraintBuilder) *ggh {
	g := new(ggh)
	g.cb = cb
	g.ivBits = g.makeIVBits()
	return g
}

func (g *ggh) GetNumHashBits() int {
	return _NUM_ELTS*HASH_FIELD_BITS
}

func (g *ggh) CreateHash(createVar func(index int) string) *HashType {
	return NewHashType(createVar, _NUM_ELTS)
}

func (g *ggh) HashValue(v string) []string {
	return g.hashBits(Rev(g.cb.Split(v, HASH_FIELD_BITS)))
}

func (g *ggh) HashValueBits(valBits []string) []string {
	return g.hashBits(valBits)
}

func (g *ggh) HashChildren(left, right []string) []string {
	return g.hashBits(Rev(append(right, left...)))
}

func (g *ggh) Final(hashBits []string) *HashType {
	return NewHashTypeFinal(hashBits, _NUM_ELTS, g.cb)
}

func (g *ggh) bitToVar(v uint64, i int) string {
	bit := (v >> uint(i)) & 1
	rv := g.cb.Zero
	if bit == 1 {
		rv = g.cb.One
	}
	return rv
}

func (g *ggh) makeIVBits() []string {
	ivBits := make([]string, _OUTPUT_BITS)
	index := 0
	for _,v := range GGHIV {
		for i := 0; i < 8; i++ {
			ivBits[index] = g.bitToVar(uint64(v), i)
			index++
		}
	}
	return ivBits
}

func (g *ggh) copyLengthBits(b []string, length uint64) {
	for i := 0; i < 64; i++ {
		b[i] = g.bitToVar(length, i)
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
func (g *ggh) hashBits(input []string) []string {
    blockBits := make([]string, M)
    resultBits := g.ivBits
    moreBlocks := true
    firstBlock := true
    
    for moreBlocks {
	bb := blockBits
	spaceLeft := M

	if !firstBlock {
		resultBits = Rev(resultBits)
	}
	copy(bb, resultBits)
	bb = bb[_OUTPUT_BITS:]
	spaceLeft -= _OUTPUT_BITS

	// prepend the length
	if firstBlock {
		g.copyLengthBits(bb, uint64(len(input)))
		bb = bb[64:]
		spaceLeft -= 64
		firstBlock = false
	}

	numInputBits := len(input)
	if numInputBits > spaceLeft {
		numInputBits = spaceLeft
	}

	if numInputBits > 0 {
		copy(bb, input[:numInputBits])
		bb = bb[numInputBits:]
		input = input[numInputBits:]
		spaceLeft -= numInputBits
	}

//    	// add a 1 bit to the end (the rest of the block is implicitly padded with 0s)
	if spaceLeft > 0 {
		bb[0] = g.cb.One
		spaceLeft--
		moreBlocks = false
	}

	resultBits = g.hashBlock(blockBits[:len(blockBits) - spaceLeft])
    }
    
    return resultBits
}

func (g *ggh) hashBlock(input []string) []string {
    if len(input) > M {
        panic("Trying to GGH hash a block of too many bits")
    }

    result := make([]string, _OUTPUT_BITS)
    maxRowBits := int(math.Ceil(math.Log2(float64(len(input))))) + Q_BITS

    /*row := AMat*/
    /*for i := 0; i < N; i++ {*/
        /*v := g.cb.SumOfProducts(input, row[:M])*/
        /*start := (N-i-1) * Q_BITS*/
        /*copy(result[start : start+Q_BITS], g.cb.ModBits(v, maxRowBits, Q_BITS))*/
        /*row = row[M:]*/
    /*}*/

    v := g.cb.MatrixVectorMul(AMat, input, N, M)
    for i := 0; i < N; i++ {
      start := (N-i-1) * Q_BITS
      copy(result[start : start+Q_BITS], g.cb.ModBits(v[i], maxRowBits, Q_BITS))
    }

    return result
}

func (g *ggh) GetNumElts() int {
	return _NUM_ELTS
}
