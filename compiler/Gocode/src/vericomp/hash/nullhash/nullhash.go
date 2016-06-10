package null

import (
	. "vericomp"
	. "vericomp/hash"
)

const _NUM_ELTS = 2

type nullHash struct {
	cb *ConstraintBuilder
}

func NewNullHash(cb *ConstraintBuilder) *nullHash {
	n := new(nullHash)
	n.cb = cb
	return n
}

func (n *nullHash) GetNumHashBits() int {
	return _NUM_ELTS*HASH_FIELD_BITS
}

func (n *nullHash) CreateHash(createVar func(index int) string) *HashType {
	return NewHashType(createVar, _NUM_ELTS)
}

func (n *nullHash) HashValue(val string) []string {
	return n.hashBits(n.cb.Split(val, HASH_FIELD_BITS), false)
}

func (n *nullHash) HashValueBits(valBits []string) []string {
	return n.hashBits(valBits, true)
}

func (n *nullHash) hashBits(valBits []string, rev bool) []string {
	hashBits := make([]string, n.GetNumHashBits())
	start := 0
	if len(valBits) < len(hashBits) {
		start = len(hashBits) - len(valBits)
	}

	if rev {
		valBits = Rev(valBits)
	}
	
	copy(hashBits[start:], valBits)
	for i := 0; i < start; i++ {
		hashBits[i] = n.cb.Zero
	}
	
	return hashBits
}

func (n *nullHash) HashChildren(left, right []string) []string {
	return n.cb.Xor(left, right)
}

func (n *nullHash) Final(hashBits []string) *HashType {
	return NewHashTypeFinal(hashBits, _NUM_ELTS, n.cb)
}

func (n *nullHash) GetNumElts() int {
	return _NUM_ELTS
}