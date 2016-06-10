package merkle

import (
	. "vericomp"
	. "vericomp/hash"
	. "vericomp/hash/nullhash"
	. "vericomp/hash/ggh"
	"math"
	. "strconv"
	"os"
)

type merkle struct {
	cb *ConstraintBuilder
	dbSize int // should be a power of 2
	numLevels int
	h Hasher
}

func NewMerkle(dbSize int, hashType string, specFile, pwsFile, qapAFile, qapBFile, qapCFile, 
			   f1IndexFile *os.File, outputTmpl bool) *merkle {
	m := new(merkle)
	
	m.cb = NewConstraintBuilder(specFile, pwsFile, qapAFile, qapBFile, qapCFile, f1IndexFile, outputTmpl)
	m.dbSize = dbSize
	m.numLevels = int(math.Ceil(math.Log2(float64(dbSize)))) + 1
	
	switch hashType {
	case "null":
		m.h = NewNullHash(m.cb)
	case "ggh":
		m.h = NewGGH(m.cb)
	default:
		panic("Unknown hash function: " + hashType)
	}
	
	return m
}

func (m *merkle) WriteFiles() {
	m.cb.WriteFiles()
}

func (m *merkle) GetCB() *ConstraintBuilder {
	return m.cb
}

func (m *merkle) GenGetPutConstraints() {
	var createInputVar = func(index int) string {
		return m.cb.NextInput()
	}
	var createVar = func(index int) string {
		return m.cb.NextVar()
	}
	
	// We have to create all the inputs and then all the outputs
	inRootHash := m.h.CreateHash(createInputVar)
	inVal := m.cb.NextInput()
	outVal1 := m.cb.NextOutput()
	outVal2 := m.cb.NextOutput()
	
	index := m.cb.Constant(1)
	
	m.Get(inRootHash, index, outVal1)
	
	rootHash2 := m.h.CreateHash(createVar)
	m.Put(inRootHash, index, inVal, rootHash2)
	
	val := m.cb.NextVar()
	m.Get(rootHash2, index, val)
	m.cb.Assignment(inVal, val)
	
	rootHash3 := m.h.CreateHash(createVar)	
	m.Increment(rootHash2, index, rootHash3)
	
	val2 := m.cb.NextVar()
	m.Get(rootHash3, index, val2)
	m.cb.Assignment(val2, m.cb.AddConst(val, 1))
	
	m.cb.Assignment(outVal2, val2)
}


func (m *merkle) GenSumConstraints() {
	var createInputVar = func(index int) string {
		return m.cb.NextInput()
	}
	rootHash := m.h.CreateHash(createInputVar)
	outSum := m.cb.NextOutput()
	
	m.SumValues(rootHash, outSum)
}

func (m *merkle) makeIndexedVar(prefix string, index int) string {
	return m.cb.NewExternalVar(prefix + "_" + Itoa(index))
}

func (m *merkle) genVars(numVars int, prefix string) []string {
	vars := make([]string, numVars)
	for i,_ := range vars {
		vars[i] = m.makeIndexedVar(prefix, i)
	}
	return vars
}

func (m *merkle) genHashVars(prefix string) *HashType {
	var createVar = func(index int) string {
		return m.makeIndexedVar(prefix, index)
	}
	return m.h.CreateHash(createVar)
}

func (m *merkle) genDBOp(op string, numValVars int) {
	index := m.cb.NewExternalVar("VAR_INDEX")
	
	if op == "get" || op == "put" {
		numValVars = 1
	}
	values := m.genVars(numValVars, "VAR_VALUE")
	inHash := m.genHashVars("VAR_INHASH")
	
	switch op {
	case "get":
		m.Get(inHash, index,  values[0])
	case "get_bits":
		m.GetBits(inHash, index,  values)
	case "put", "put_bits":
		outHash := m.genHashVars("VAR_OUTHASH")
		
		if op == "put" {
			m.Put(inHash, index, values[0], outHash)
		} else {
			m.PutBits(inHash, index, values, outHash)
		}
	default:
		panic("Unknown op: " + op)
	}
}

func (m *merkle) genBlockHashOp(op string, numValVars int) {
	switch op {
	case "get_block_by_hash":
		hashBits := m.genVars(m.h.GetNumHashBits(), "VAR_INHASH")
		valueBits := m.genVars(numValVars, "VAR_VALUE")
		m.GetBlockByHash(hashBits, valueBits)
	case "put_block_by_hash":
		valueBits := m.genVars(numValVars, "VAR_VALUE")
		outHashBits := m.genVars(m.h.GetNumHashBits(), "VAR_OUTHASH")
		m.PutBlockByHash(valueBits, outHashBits)
	case "free_block_by_hash":
		hashBits := m.genVars(m.h.GetNumHashBits(), "VAR_INHASH")
		m.FreeBlockByHash(hashBits)
	default:
		panic("Unknown op: " + op)
	}
}

func (m *merkle) GenOp(op string, numValVars int) {
	switch op {
	case "get", "get_bits", "put", "put_bits":
		m.genDBOp(op, numValVars)
	case "get_block_by_hash", "put_block_by_hash", "free_block_by_hash":
		m.genBlockHashOp(op, numValVars)
	default:
		panic("Unknown op: " + op)
	}
}

func (m *merkle) Get(rootHash *HashType, index, outValue string) {
	v := m.cb.NextVar()
	m.cb.DBGet(index, v)
	m.checkPath(rootHash, v, m.cb.Split(index, m.numLevels), m.getSiblingHashes(index))
	m.cb.Assignment(outValue, v)
}

func (m *merkle) GetBits(rootHash *HashType, index string, outValues []string) {
	m.cb.DBGetBits(index, outValues)
	m.checkPathBits(rootHash, outValues, m.cb.Split(index, m.numLevels), m.getSiblingHashes(index))
}

func (m *merkle) Put(rootHash *HashType, index, value string, outRootHash *HashType) {
	put := func(oldVal, newVal string) {
		m.cb.Assignment(newVal, value)
	}
	
	m.updateVal(rootHash, index, put, outRootHash)
}

func (m *merkle) PutBits(rootHash *HashType, index string, values []string, outRootHash *HashType) {
	oldLeaf := make([]string, len(values))
	for i,_ := range values {
		oldLeaf[i] = m.cb.NextVar()
	}
	
	m.cb.DBGetBits(index, oldLeaf)
	pathBits := m.cb.Split(index, m.numLevels)
	siblingHashes := m.getSiblingHashes(index)
	
	m.checkPathBits(rootHash, oldLeaf, pathBits, siblingHashes)
	
	m.cb.DBPutBits(index, values)
	newRootHash := m.genPath(m.h.HashValueBits(values), pathBits, siblingHashes)
	outRootHash.CompareTo(newRootHash, m.cb)
}

func (m *merkle) Increment(rootHash *HashType, index string, outRootHash *HashType) {
	inc := func(oldVal, newVal string) {
		m.cb.Assignment(newVal, m.cb.AddConst(oldVal, 1))
	}
	
	m.updateVal(rootHash, index, inc, outRootHash)
}

func (m *merkle) SumValues(rootHash *HashType, outSum string) {
	values := m.getDBValues()
	m.checkWholeTree(rootHash, values)
	
	sum := m.cb.Sum(values)
	m.cb.Assignment(outSum, sum)
}

func (m *merkle) MultHash(h *HashType, val string) {
	for i,_ := range h.FieldElts {
		h.FieldElts[i] = m.cb.Mult(h.FieldElts[i], val)
	}
}

func (m *merkle) GetBlockByHash(hashBits, valueBits []string) {
	//Have prover supply value bits for H^-1 hashBits.
	m.cb.GetBlockByHash(hashBits, valueBits)
	
	//Combine the hashBits
	expectedHash := m.h.Final(hashBits)

	//Before applying hashValueBits to the valueBits, we must check the
	//prover supplied bits.
	for _,v := range valueBits {
	  m.cb.AssertBit(v)
	}

	//O.K. We can aply H to the prover's bits.
	actualHash := m.h.Final(m.h.HashValueBits(valueBits))
	
	// hashSum will be zero if the expected hash == NULL_HASH
	hashSum := m.cb.Sum(expectedHash.FieldElts)
	
	// Multiply the expected and actual hashes by hashSum. If the expected hash
	// is nonzero, then hashSum will be nonzero, and multiplying both the
	// expected and actual hashes by the same nonzero value won't affect the
	// equality test. If hashSum is zero, however, then multiplying both sides
	// by zero will cause the equality test to always succeed.
	m.MultHash(actualHash, hashSum)
	m.MultHash(expectedHash, hashSum)
	expectedHash.CompareTo(actualHash, m.cb)
}

func (m *merkle) PutBlockByHash(valueBits, outHashBits []string) {
	hashBits := m.h.HashValueBits(valueBits)
	m.cb.PutBlockByHash(hashBits, valueBits)
	
	for i,_ := range hashBits {
		m.cb.Assignment(outHashBits[i], hashBits[i])
	}
}

func (m *merkle) FreeBlockByHash(hashBits []string) {
	m.cb.FreeBlockByHash(hashBits)
}



func (m *merkle) updateVal(rootHash *HashType, index string, 
						   updateFunc func(oldVal, newVal string), outRootHash *HashType) {
						   
	oldLeaf := m.cb.NextVar()
	m.cb.DBGet(index, oldLeaf)
	pathBits := m.cb.Split(index, m.numLevels)
	siblingHashes := m.getSiblingHashes(index)
	
	m.checkPath(rootHash, oldLeaf, pathBits, siblingHashes)
	
	newLeaf := m.cb.NextVar()
	updateFunc(oldLeaf, newLeaf)
	m.cb.DBPut(index, newLeaf)
	
	newRootHash := m.genPath(m.h.HashValue(newLeaf), pathBits, siblingHashes)
	outRootHash.CompareTo(newRootHash, m.cb)
}

func (m *merkle) getSiblingHashes(index string) [][]string {
	siblingHashes := make([][]string, m.numLevels - 1)
	for i, _ := range siblingHashes {
		siblingHashes[i] = m.cb.SiblingHash(index, i+1, m.h.GetNumHashBits())
	}
	return siblingHashes
}

func (m *merkle) checkPath(rootHash *HashType, leaf string, pathBits []string, siblingHashes [][]string) {
	computedRootHash := m.genPath(m.h.HashValue(leaf), pathBits, siblingHashes)
	computedRootHash.CompareTo(rootHash, m.cb)
}

func (m *merkle) checkPathBits(rootHash *HashType, leafBits []string, pathBits []string, siblingHashes [][]string) {
	computedRootHash := m.genPath(m.h.HashValueBits(leafBits), pathBits, siblingHashes)
	computedRootHash.CompareTo(rootHash, m.cb)
}


func (m *merkle) genPath(leafHash []string, pathBits []string, siblingHashes [][]string) *HashType {
	nodeHash := leafHash
	
	for i := m.numLevels - 1; i > 0; i-- {
		leftHash := m.cb.IfThenElseArray(pathBits[i], siblingHashes[i-1], nodeHash)
		rightHash := m.cb.IfThenElseArray(pathBits[i], nodeHash, siblingHashes[i-1])
		nodeHash = m.h.HashChildren(leftHash, rightHash)
	}
	
	return m.h.Final(nodeHash) // returns the root hash
}

func (m *merkle) checkWholeTree(rootHash *HashType, leaves []string) {
	computedRootHash := m.h.Final(m.hashSubtree(leaves, 0, 0))
	computedRootHash.CompareTo(rootHash, m.cb)
}

func (m *merkle) hashSubtree(leaves []string, level, index int) []string {
	var result []string
	
	if level == m.numLevels - 1 {
		result = m.h.HashValue(leaves[index])
	} else {
		leftHash := m.hashSubtree(leaves, level+1, index << 1)
		rightHash := m.hashSubtree(leaves, level+1, (index << 1) | 1)
		result = m.h.HashChildren(leftHash, rightHash)
	}
	
	return result
}

func (m *merkle) getDBValues() []string {
	values := make([]string, m.dbSize)
	for i, _ := range values {
		values[i] = m.cb.NextVar()
		m.cb.DBGetConst(i, values[i])
	}
	return values
}
