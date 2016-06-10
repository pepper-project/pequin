package hash

import (
	. "vericomp"
)

const HASH_FIELD_BITS = 64

type HashType struct {
	FieldElts []string
}

func NewHashType(createVar func(index int) string, numElts int) *HashType {
	ht := new(HashType)
	
	ht.FieldElts = make([]string, numElts)
	for i,_ := range ht.FieldElts {
		ht.FieldElts[i] = createVar(i)
	}

	return ht
}

func NewHashTypeFinal(hashBits []string, numElts int, cb *ConstraintBuilder) *HashType {
	ht := new(HashType)
	
	ht.FieldElts = make([]string, numElts)
	for i,_ := range ht.FieldElts {
		start := (numElts-i-1) * HASH_FIELD_BITS
		ht.FieldElts[i] = cb.Combine(hashBits[start : start+HASH_FIELD_BITS])
	}
	
	return ht
}

func (ht1 *HashType) CompareTo(ht2 *HashType, cb *ConstraintBuilder) {
	v1 := ht1.FieldElts
	v2 := ht2.FieldElts
	
	for i,_ := range v1 {
		cb.Assignment(v1[i], v2[i])
	}
}


type Hasher interface {
	GetNumHashBits() int
	CreateHash(createVar func(index int) string) *HashType
	HashValue(string) []string
	HashValueBits([]string) []string
	HashChildren([]string, []string) []string
	Final([]string) *HashType
	GetNumElts() int
}

func Rev(v []string) []string {
	r := make([]string, len(v))
	
	for i,_ := range v {
		r[len(v)-i-1] = v[i]
	}
	
	return r
}