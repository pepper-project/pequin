package vericomp

import (
    "fmt"
    "io"
    "bytes"
    "math/big"
    "strconv"
    "log"
    "os"
    "unicode"
    "vericomp/util"
)

const FIELD_BITS = 128



// Terms

type term struct {
    coef interface{}
    variable string
}

func t(variable string) term {
    return ti(1, variable)
}

func tn(variable string) term {
    return ti(-1, variable)
}

func ti(coeff int64, variable string) term {
    return term{coeff, variable}
}

func tb(coeff *big.Int, variable string) term {
    return term{coeff, variable}
}

func r(terms ...term) []term {
	return terms
}


 // ConstraintBuilder
 
type ConstraintBuilder struct {
    nextSubcript, nextInputOutputSub, inputNum int
    
    buildSpec, buildPWS, buildQAP, outputTmpl bool
    
    specFile, pwsFile, f1IndexFile, cmdsFile *os.File

    matFiles [3]*os.File
    matNumCons int
    numNonZero [3]int

    varNumBits map[string]int
    
    extVars []string
    extVarIndices map[string]int
    
    Zero, One string
}

func NewConstraintBuilder(specFile, pwsFile, qapAFile, qapBFile, qapCFile, 
						  f1IndexFile *os.File, outputTmpl bool) *ConstraintBuilder {
    cb := new(ConstraintBuilder)
    
    cb.buildSpec = specFile != nil
    cb.buildPWS = pwsFile != nil
    cb.buildQAP = qapAFile != nil && qapBFile != nil && qapCFile != nil
    cb.outputTmpl = outputTmpl
    
    if cb.buildSpec && (cb.buildPWS || cb.buildQAP) {
    	log.Fatal("You can only output Spec or PWS and QAP, not both")
    }
    if cb.outputTmpl {
	    if cb.buildSpec {
	    	log.Fatal("You can't build a Spec file as a template")
	    }
	    if !cb.buildPWS || !cb.buildQAP {
	    	log.Fatal("You must output PWS and QAP if you're building a template")
	    }
    }
    
    cb.cmdsFile = util.OpenTempFile("cmds")
    if cb.buildSpec {
    	cb.specFile = specFile
    }
    if cb.buildPWS {
    	cb.pwsFile = pwsFile
    }
    if cb.buildQAP {
    	cb.matFiles[0] = qapAFile
    	cb.matFiles[1] = qapBFile
    	cb.matFiles[2] = qapCFile
    }
    cb.f1IndexFile = f1IndexFile

    cb.varNumBits = make(map[string]int)
    cb.extVarIndices = make(map[string]int)
    
    cb.Zero = cb.Constant(0)
    cb.One = cb.Constant(1)
    
    return cb
}

func (b *ConstraintBuilder) pv(varName string) string {
    if b.outputTmpl && !unicode.IsDigit(rune(varName[0])) {
        return "${" + varName + "}"
    }

    return varName
}

func (b *ConstraintBuilder) addPolyCmd(resultVar, poly string) {
    if b.buildSpec {
        fmt.Fprintf(b.cmdsFile, "(  ) * (  ) + ( %v - %v )\n", poly, b.pv(resultVar))
    } else {
        fmt.Fprintf(b.cmdsFile, "P %v = %v E\n", b.pv(resultVar), poly)
    }
}

func (b *ConstraintBuilder) getIndex(variable string) int {
    if variable == "" {
        return 0
    }

    var index int = -1
    
    index, exists := b.extVarIndices[variable]
    if exists {
        index =+ b.nextSubcript + b.nextInputOutputSub
    } else if variable[0] == 'V' {
        index, _ = strconv.Atoi(variable[1:])
    } else if variable[0] == 'I' {
        index, _ = strconv.Atoi(variable[1:])
        index += b.nextSubcript
    } else if variable[0] == 'O' {
        index, _ = strconv.Atoi(variable[1:])
        index += b.nextSubcript
    } else {
        log.Fatal("getIndex: variable " + variable + " not found!")
    }
    
    return index + 1
}


func (b *ConstraintBuilder) addToMat(mat int, terms []term) {
    for _, t := range terms {
    	if t.coef == 0 || t.variable == b.Zero {
    		continue
    	}
        
        var v, c string
        if b.outputTmpl {
        	if t.variable == "" {
        		v = "0"
        	} else {
            	v = b.pv(t.variable)
            }
            c = "${CONS_" + strconv.Itoa(b.matNumCons) + "}"
        } else {
            v = strconv.Itoa(b.getIndex(t.variable))
            c = strconv.Itoa(b.matNumCons)
        }
        fmt.Fprintf(b.matFiles[mat], "%v %v %v\n", v, c, t.coef)
        b.numNonZero[mat]++
    }
}

func (b *ConstraintBuilder) addCons(aTerms []term, bTerms []term, cTerms []term) {
    b.matNumCons++
    terms := [][]term{aTerms, bTerms, cTerms}
    for i, t := range terms {
    	if t != nil {
    		b.addToMat(i, t)
    	}
    }
}

func (b *ConstraintBuilder) DBGet(index, outValue string) {
    // We don't add anything to the QAP because the value from the DB is exogenous
    fmt.Fprintf(b.cmdsFile, "DB_GET %v %v\n", b.pv(index), b.pv(outValue))
}

func (b *ConstraintBuilder) DBGetBits(index string, outValues []string) {
    // We don't add anything to the QAP because the value from the DB is exogenous
    fmt.Fprintf(b.cmdsFile, "DB_GET_BITS %v %v ", b.pv(index), len(outValues))
    for _,v := range outValues {
        fmt.Fprintf(b.cmdsFile, "%v ", b.pv(v))
    }
    fmt.Fprintln(b.cmdsFile)
}

func (b *ConstraintBuilder) DBGetConst(index int, outValue string) {
    b.DBGet(strconv.Itoa(index), outValue)
}

func (b *ConstraintBuilder) DBPut(index, value string) {
    // We don't add anything to the QAP because putting a value into the DB is exogenous
    fmt.Fprintf(b.cmdsFile, "DB_PUT %v %v\n", b.pv(index), b.pv(value))
}

func (b *ConstraintBuilder) DBPutBits(index string, values []string) {
    // We don't add anything to the QAP because the value from the DB is exogenous
    fmt.Fprintf(b.cmdsFile, "DB_PUT_BITS %v %v ", b.pv(index), len(values))
    for _,v := range values {
        fmt.Fprintf(b.cmdsFile, "%v ", b.pv(v))
    }
    fmt.Fprintln(b.cmdsFile)
}

func (b *ConstraintBuilder) DBPutConst(index int, value string) {
    b.DBPut(strconv.Itoa(index), value)
}

func (b *ConstraintBuilder) SiblingHash(index string, level, numHashBits int) []string {    
    hashBits := make([]string, numHashBits)
    for i,_ := range hashBits {
        hashBits[i] = b.NextVarBits(1)
    }
    
    //   We don't add anything to the QAP because the sibling hash is exogenous
    fmt.Fprintf(b.cmdsFile, "DB_GET_SIBLING_HASH %v %v %v\n", b.pv(index), level, b.pv(hashBits[0]))
    
    return hashBits
}

func (b *ConstraintBuilder) blockByHash(get bool, hashBits, values []string) {
    // We don't add anything to the QAP because the value from the DB is exogenous
    op := "GET_BLOCK_BY_HASH"
    numXy := "NUM_Y"
    xy := "Y"
    if !get {
        op = "PUT_BLOCK_BY_HASH"
        numXy = "NUM_X"
        xy = "X"
    }
    
    fmt.Fprintf(b.cmdsFile, "%v ", op)
    for _,v := range hashBits {
        fmt.Fprintf(b.cmdsFile, "%v ", b.pv(v))
    }
    fmt.Fprintf(b.cmdsFile, "%v %v %v ", numXy, len(values), xy)
    for _,v := range values {
        fmt.Fprintf(b.cmdsFile, "%v ", b.pv(v))
    }
    
    fmt.Fprintln(b.cmdsFile)
}

func (b *ConstraintBuilder) GetBlockByHash(hashBits, outValues []string) {  
    b.blockByHash(true, hashBits, outValues)
}

func (b *ConstraintBuilder) PutBlockByHash(hashBits, values []string) { 
    b.blockByHash(false, hashBits, values)
}

func (b *ConstraintBuilder) FreeBlockByHash(hashBits []string) {    
    // We don't add anything to the QAP because the value from the DB is exogenous
    fmt.Fprintf(b.cmdsFile, "FREE_BLOCK_BY_HASH ")
    for _,v := range hashBits {
        fmt.Fprintf(b.cmdsFile, "%v ", b.pv(v))
    }
    fmt.Fprintln(b.cmdsFile)
}

func (b *ConstraintBuilder) AssertBit(x string) {
  //Similar to ensureBit, below, but works when outputting to Spec.
  
  // This command doesn't seem to get written to the PWS file 
  if b.buildSpec {
  	v := b.pv(x)
  	fmt.Fprintf(b.cmdsFile, "ASSERT_POLY_ZERO ( %v ) * ( %v ) + ( -1 * %v )\n", v, v, v)
  }
  
  b.ensureBit(x)
}

func (b *ConstraintBuilder) ensureBit(x string) {
    if b.buildQAP {
        // x * x - x = 0
        b.addCons(r(t(x)),
         		  r(t(x)),
        		  r(tn(x)))
    }
    
    // We don't add to the PWS or Spec, because this has to be handled by a PWS or Spec builtin
}

// Big endian
func (b *ConstraintBuilder) sumOfPowers(xi []string, x string, printPoly bool) {
    numBits := len(xi)
    Ci := make([]term, numBits + 1)
    
    // (x_i * 2^i) + ... + (x_0 * 2^0) - x = 0
    for i,_ := range xi {       
        coeff := new(big.Int).Exp(big.NewInt(2), big.NewInt(int64(numBits - 1 - i)), nil)
        Ci[i] = tb(coeff, xi[i])
    }
    Ci[numBits] = tn(x)
    
    if b.buildQAP {
    	b.addCons(nil, nil, Ci)
    }
    
    if printPoly {
        poly := new(bytes.Buffer)
        for i,_ := range xi {
            fmt.Fprintf(poly, "( %v * %v )", Ci[i].coef, b.pv(xi[i]))
            if i < len(xi) - 1 {
                fmt.Fprintf(poly, " + ")
            }
        }
        b.addPolyCmd(x, poly.String())
    }
}

// Big endian
func (b *ConstraintBuilder) Split(x string, numBits int) []string {
    xi := make([]string, numBits)
    for i,_ := range xi {
        xi[i] = b.NextVarBits(1)
        b.ensureBit(xi[i])
    }
    
    fmt.Fprintf(b.cmdsFile, "SI %v into %v bits at %v\n", b.pv(x), numBits, b.pv(xi[0]))
    b.sumOfPowers(xi, x, false)
    
    return xi
}

// TODO: I'm not sure why the constraints involving the coefficients were commeneted out.

/* big endian */
func (b *ConstraintBuilder) splitBase(x string, l int, base int64) []string {
    xi := make([]string, l)
    for i := 0; i < l; i++ {
        xi[i] = b.NextVar()
        b.ensureBit(xi[i])
//        b.addCons(0, []term{term{big.NewInt(1), xi[i]}})
//        b.addCons(1, []term{term{big.NewInt(1), xi[i]}})
//        b.addCons(2, []term{term{big.NewInt(1), xi[i]}})
        //b.cmds = append(b.cmds, fmt.Sprintf("P %v = (%v) * (%v) E\n", xi[i], xi[i], xi[i]))
    }

    fmt.Fprintf(b.cmdsFile, "P %v = (", x) // AJF - uncomment
    Ai := make([]term, 0)
    for i := 0; i < l; i++ {
        coef := new(big.Int).Exp(big.NewInt(base), big.NewInt(int64(l - i - 1)), nil)
        Ai = append(Ai, term{coef, xi[i]})
        fmt.Fprintf(b.cmdsFile, "%v * %v + ", coef.String(), xi[i]) // AJF - uncomment
    }
    //Ai = append(Ai, term{new(big.Int).Neg(new(big.Int).Exp(big.NewInt(2), big.NewInt(31), nil)), ""})
//    b.addCons(0, Ai)
//    b.addCons(1, []term{term{big.NewInt(1), ""}})
    //b.c = append(b.c, []term{term{big.NewInt(1), x},term{new(big.Int).Exp(big.NewInt(2), big.NewInt(31), nil), ""}})
//    b.addCons(2, []term{term{big.NewInt(1), x}})
// AJF -    b.cmds = append(b.cmds, fmt.Sprintf("SI %v into %v bits at %v", x, l, xi[0]))
    fmt.Fprintf(b.cmdsFile, "0) * (1) E") // AJF - uncomment
    
    fmt.Fprintln(b.cmdsFile)
    return xi
}

// Big endian
func (b *ConstraintBuilder) Combine(xi []string) string {
    x := b.NextVar()
    b.sumOfPowers(xi, x, true)  
    return x
}

/* big endian */
func (b *ConstraintBuilder) combineBase(xi []string, base int64) string {
    x := b.NextVar()
    l := len(xi)

    fmt.Fprintf(b.cmdsFile, "P %v = ( ", x)
    Ai := make([]term, 0)
    for i := 0; i < l; i++ {
        coef := new(big.Int).Exp(big.NewInt(base), big.NewInt(int64(l - i - 1)), nil)
        Ai = append(Ai, term{coef, xi[i]})
        fmt.Fprintf(b.cmdsFile, "%v * %v + ", coef.String(), xi[i])
    }
    //Ai = append(Ai, term{new(big.Int).Neg(new(big.Int).Exp(big.NewInt(2), big.NewInt(31), nil)), ""})
//    b.addCons(0, Ai)
//    b.addCons(1, []term{term{big.NewInt(1), ""}})
    //b.c = append(b.c, []term{term{big.NewInt(1), x},term{new(big.Int).Exp(big.NewInt(2), big.NewInt(31), nil), ""}})
//    b.addCons(2, []term{term{big.NewInt(1), x}})

    fmt.Fprintf(b.cmdsFile, " 0 ) E")

    fmt.Fprintln(b.cmdsFile)
    return x
}

func (b *ConstraintBuilder) Base257ToBase256(input []string, output []string) string {
    size := len(input)
    
    // Reverse the input because it arrives as little endian but combineBase() is big endian 
    in := make([]string, size)
    for i, val := range input {
        in[size-i-1] = val
    }
    
    
    // TODO: There is likely a problem here. How do we know that the variables in out are in the right range?
    // out[0] is supposed to be a bit, and the rest are supposed to be < 256
    // This could be a problem for all uses of Split().
    sum := b.combineBase(in, 257)
    out := b.splitBase(sum, size+1, 256)
    
    // Reverse the output because the caller expects the result to be little endian 
    for i := 0; i < size; i++ {
        output[i] = out[size-i]
    }
    
    // The first, and largest, element of out is the "carry" bit
    return out[0]
}

func (b *ConstraintBuilder) Xor(xi, yi []string) []string {
    l := len(xi)
    zi := make([]string, l)

    for i := 0; i < l; i++ {
        zi[i] = b.NextVarBits(1)
    }

    //  -2xy + x + y - z = 0
    for i := 0; i < l; i++ {
        if b.buildQAP {
        	b.addCons(r(ti(-2, xi[i])),
        			  r(t(yi[i])),
        			  r(t(xi[i]), t(yi[i]), tn(zi[i])))
        }

        // Special case it when we're making a spec file because the compiler backend barfs 
        if b.buildSpec {
            fmt.Fprintf(b.cmdsFile, "( -2 * %v ) * ( %v ) + ( %v + %v - %v )\n",
                        b.pv(xi[i]), b.pv(yi[i]), b.pv(xi[i]), b.pv(yi[i]), b.pv(zi[i]))
        } else {
            b.addPolyCmd(zi[i], fmt.Sprintf("( -2 * %v ) * ( %v ) + %v + %v", b.pv(xi[i]), b.pv(yi[i]), b.pv(xi[i]), b.pv(yi[i])))
        }
        
    }
    return zi
}

func (b *ConstraintBuilder) And(xi,yi []string) []string {
    l := len(xi)
    zi := make([]string, l)

    for i := 0; i < l; i++ {
        zi[i] = b.NextVarBits(1)
    }

    // xy - z = 0
    for i := 0; i < l; i++ {
        if b.buildQAP {
        	b.addCons(r(t(xi[i])),
        			  r(t(yi[i])),
        			  r(tn(zi[i])))
        }
        
        b.addPolyCmd(zi[i], fmt.Sprintf("%v * %v", b.pv(xi[i]), b.pv(yi[i])))
    }
    return zi
}

func (b *ConstraintBuilder) Or(xi,yi []string) []string {
    l := len(xi)
    zi := make([]string, l)

    for i := 0; i < l; i++ {
        zi[i] = b.NextVarBits(1)
    }

     // z = x or y <=> z = 1 - (1-x)(1-y) <=> -xy + x + y - z = 0
    for i := 0; i < l; i++ {
        if b.buildQAP {
        	b.addCons(r(tn(xi[i])),
        			  r(t(yi[i])),
        			  r(t(xi[i]), t(yi[i]), tn(zi[i])))
        }
        
        b.addPolyCmd(zi[i], fmt.Sprintf("( -1 * %v ) * ( %v ) + %v + %v", b.pv(xi[i]), b.pv(yi[i]), b.pv(xi[i]), b.pv(yi[i])))
    }

    return zi
}

func (b *ConstraintBuilder) Not(xi []string) []string {
    l := len(xi)
    yi := make([]string, l)

    for i := 0; i < l; i++ {
        yi[i] = b.NextVarBits(1)
    }

    // 1 - x - y = 0
    for i := 0; i < l; i++ {
        if b.buildQAP {
        b.addCons(nil,
        		  nil,
        		  r(t(""), tn(xi[i]), tn(yi[i])))
        }
        
        b.addPolyCmd(yi[i], fmt.Sprintf("1 - %v", b.pv(xi[i])))
    }

    return yi
}

func (b *ConstraintBuilder) Leftrotate(xi []string, n int) []string {
    l := len(xi)
    yi := make([]string, l)

    //for i := 0; i < l; i++ {
    //    yi[i] = b.NextVar()
    //}

    // TODO need to think about
    for i := 0; i < l; i++ {
        /*b.a = append(b.a, []term{term{big.NewInt(1), xi[(i + n) % l]}})
        b.b = append(b.b, []term{term{big.NewInt(1), ""}})
        b.c = append(b.c, []term{term{big.NewInt(1), yi[i]}})*/
        yi[i] = xi[(i + n) % l]
        //fmt.Printf("P %v = ( %v ) E", yi[i], xi[(i + n) % l])
        //fmt.Printf("P %v = %v E\n", yi[(i + n) % l], xi[i])
    }
    return yi
}

func (b *ConstraintBuilder) Add(values ...string) string {
    return b.Sum(values)
}

func (b *ConstraintBuilder) AddConst(v string, c int64) string {
    y := b.NextVar()
    
    if b.buildQAP {
        b.addCons(nil,
        		  nil,
        		  r(t(v), ti(c, ""), tn(y)))
    }
    
    b.addPolyCmd(y, fmt.Sprintf("%v + %v", b.pv(v), c))
    return y
}

func (b *ConstraintBuilder) Sum(values []string) string {
    return b.SumToVar(b.NextVar(), values);
}

func (b *ConstraintBuilder) SumToVar(result string, values []string) string {
    z := result
    
    Ci := make([]term, len(values) + 1)
    poly := new(bytes.Buffer)
    
    for i, v := range values {
        Ci[i] = t(v)
        
        fmt.Fprintf(poly, "%v", b.pv(v))
        if i < len(values) - 1 {
            fmt.Fprintf(poly, " + ")
        }
    }
    Ci[len(Ci) - 1] = tn(z)
    
    if b.buildQAP {
        b.addCons(nil, nil, Ci)
    }
    
    b.addPolyCmd(z, poly.String())
    
    return z
}

func (b *ConstraintBuilder) Mult(x, y string) string {
    z := b.NextVar()
    
    if b.buildQAP {
        b.addCons(r(t(x)),
        		  r(t(y)),
        		  r(tn(z)))
    }
    
    if b.buildSpec {
    	fmt.Fprintf(b.cmdsFile, "( %v ) * ( %v ) + ( - %v )\n", b.pv(x), b.pv(y), b.pv(z))
    } else {
        b.addPolyCmd(z, fmt.Sprintf("%v * %v", b.pv(x), b.pv(y)))
    }
    return z
}

func (b *ConstraintBuilder) MatrixVectorMul(matrix []int64, vector []string, num_of_rows int, num_of_columns int) []string {
  z := make([]string, num_of_rows)

  row := matrix
  for i := 0; i < num_of_rows; i++ {
    z[i] = b.NextVar()

    var Ci []term
    constPart := big.NewInt(0)
    poly := new(bytes.Buffer)

    for i, _ := range vector {
      /*println(i)*/
      if vector[i] == b.Zero || row[i] == 0 {
        // ignore zero terms
      } else if vector[i] == b.One {
        constPart.Add(constPart, big.NewInt(row[i]))
      } else {
        Ci = append(Ci,  ti(row[i], vector[i]))
        fmt.Fprintf(poly, "( %v * %v ) + ", b.pv(vector[i]), row[i])
      }
    }

    Ci = append(Ci, tb(constPart, ""))
    fmt.Fprintf(poly, "( %v * %v )", b.pv(b.One), constPart)

    Ci = append(Ci, tn(z[i]))

    if b.buildQAP {
      b.addCons(nil, nil, Ci)
    }

    /*b.addPolyCmd(z, poly.String())*/
    if b.buildSpec {
        fmt.Fprintf(b.cmdsFile, "(  ) * (  ) + ( %v - %v )\n", poly.String(), b.pv(z[i]))
    /*} else {*/
        /*fmt.Fprintf(b.cmdsFile, "P %v = %v E\n", b.pv(z[i]), poly.String())*/
    }

    row = row[num_of_columns:]
  }

  fmt.Fprintf(b.cmdsFile, "MATRIX_VEC_MUL NUM_ROWS %v NUM_COLUMNS %v ACTUAL_NUM_COLUMNS %v", num_of_rows, num_of_columns, len(vector))

  fmt.Fprintf(b.cmdsFile, " IN_VEC")
  for _, v := range vector {
    fmt.Fprintf(b.cmdsFile, " %v", b.pv(v))
  }

  fmt.Fprintf(b.cmdsFile, " OUT_VEC")
  for _, v := range z {
    fmt.Fprintf(b.cmdsFile, " %v", b.pv(v))
  }

  fmt.Fprintf(b.cmdsFile, "\n")

  return z
}

func (b *ConstraintBuilder) SumOfProducts(v1 []string, v2 []int64) string {
    z := b.NextVar()
    
    var Ci []term
    constPart := big.NewInt(0)
    poly := new(bytes.Buffer)
    
    for i, _ := range v1 {
    	if v1[i] == b.Zero || v2[i] == 0 {
    		// ignore zero terms
    	} else if v1[i] == b.One {
    		constPart.Add(constPart, big.NewInt(v2[i]))
    	} else {
    		Ci = append(Ci,  ti(v2[i], v1[i]))
    		fmt.Fprintf(poly, "( %v * %v ) + ", b.pv(v1[i]), v2[i])
    	}
    }
    
    Ci = append(Ci, tb(constPart, ""))
    fmt.Fprintf(poly, "( %v * %v )", b.pv(b.One), constPart)
    
    Ci = append(Ci, tn(z))
    
    if b.buildQAP {
        b.addCons(nil, nil, Ci)
    }
    
    b.addPolyCmd(z, poly.String())
    
    return z
}

func (b *ConstraintBuilder) Sub(x, y string) string {
    z := b.NextVar()
    
    if b.buildQAP {
        b.addCons(nil,
        		  nil,
        		  r(t(x), tn(y), tn(z)))
    }
    
    b.addPolyCmd(z, fmt.Sprintf("%v - %v", b.pv(x), b.pv(y)))
    return z
}

func (b *ConstraintBuilder) Mod(x string, n, l int) string {
    return b.Combine(b.ModBits(x, n, l))
}

// TODO: How do we support mod that isn't a power of 2?
func (b *ConstraintBuilder) ModBits(x string, n, l int) []string {
    xi := b.Split(x, n)
    return xi[n - l:]
}

// TODO: How are signed constants supposed to be handled?
func (b *ConstraintBuilder) SignedConstant(n int) string {
    return b.Constant(uint(n))
}

func (b *ConstraintBuilder) Constant(n uint) string {
    x := b.NextVar()
    
    if b.buildQAP {
        b.addCons(nil,
        		  nil,
        		  r(ti(int64(n), ""), tn(x)))
    }
    
    b.addPolyCmd(x, strconv.Itoa(int(n)))
    return x
}

func (b *ConstraintBuilder) Assignment(y, x string) {

    // x * 1 - y = 0
    if b.buildQAP {
        b.addCons(nil,
        		  nil,
        	      r(t(x), tn(y)))
    }

    b.addPolyCmd(y, b.pv(x))
}

// TODO: Did I get this right? I tried to implement what's on p.22 in the extended Ginger paper
func (b *ConstraintBuilder) IsEqualToZero(x string) string {
    y := b.NextVar()
    oneMinusY := b.Sub(b.One, y)
    
    m := b.NextVar()
    c1 := b.Sub(b.Mult(x, m), oneMinusY)
    b.Assignment(c1, b.Zero)
    
    c2 := b.Mult(oneMinusY, x)
    b.Assignment(c2, b.Zero)
    
    return y
}

func (b *ConstraintBuilder) EqualsBit(x, y string) string {
    return b.IsEqualToZero(b.Sub(x, y))
}

func (b *ConstraintBuilder) ArrayGet(array []string, index string) string {
    var result string
    
    for i,a := range array {
        m := b.EqualsBit(index, b.Constant(uint(i)))
        result = b.Add(result, b.Mult(m, a))
    }
    
    return result
}

func (b *ConstraintBuilder) IfThenElseArray(choiceBit string, cond1, cond2 []string) []string {
    result := make([]string, len(cond1))
    
    for i,_ := range result {
        result[i] = b.IfThenElse(choiceBit, cond1[i], cond2[i])
    }
                    
    return result
}

func (b *ConstraintBuilder) IfThenElse(choiceBit, cond1, cond2 string) string {
    result := b.NextVar()
                    
    // choiceBit * (cond1 - cond2) + cond2 - result = 0
    if b.buildQAP {
        b.addCons(r(t(choiceBit)),
        		  r(t(cond1), tn(cond2)),
        		  r(t(cond2), tn(result)))
    }
    
    // Special case it when we're making a spec file because the compiler backend barfs 
    if b.buildSpec {
        fmt.Fprintf(b.cmdsFile, "( %v ) * ( %v - %v ) + ( %v - %v )\n",
                    b.pv(choiceBit), b.pv(cond1), b.pv(cond2), b.pv(cond2), b.pv(result))
    } else {
        b.addPolyCmd(result, fmt.Sprintf("( %v ) * ( %v ) + ( - %v ) * ( %v ) + %v", 
                                         b.pv(choiceBit), b.pv(cond1), b.pv(choiceBit), b.pv(cond2), b.pv(cond2)))
    }
    
    return result
}

func (b *ConstraintBuilder) NextVar() string {
    nextVariable := "V" + strconv.Itoa(b.nextSubcript)
    b.nextSubcript++
    return nextVariable
}

func (b *ConstraintBuilder) NextVarBits(numBits int) string {
    nextVar := b.NextVar()
    b.varNumBits[nextVar] = numBits
    return nextVar
}

func (b *ConstraintBuilder) NextInput() string {
    nextVariable := "I" + strconv.Itoa(b.nextInputOutputSub)
    b.nextInputOutputSub++
    b.inputNum++
    return nextVariable
}

func (b *ConstraintBuilder) NextOutput() string {
    nextVariable := "O" + strconv.Itoa(b.nextInputOutputSub)
    b.nextInputOutputSub++
    return nextVariable
}

func (b *ConstraintBuilder) NewExternalVar(name string) string {
    _, exists := b.extVarIndices[name]
    if exists {
        log.Fatal("External variable " + name + " already exists!")
    } else {
    	b.extVars = append(b.extVars, name)
        b.extVarIndices[name] = len(b.extVars)
    }
    return name
}


func (b *ConstraintBuilder) getVarNumBits(varName string) int {
    numBits := b.varNumBits[varName]
    if numBits == 0 {
        numBits = FIELD_BITS
    }
    return numBits
}


/*
func (b *ConstraintBuilder) printTerm(t term) string {
    ret := t.coef.String()
    if t.variable != "" {
        if ret == "1" {
            ret = t.variable
//        } else if "-1" {
//            ret = "-" + t.variable
        } else {
            ret += " * " + t.variable
        }
    }
    return ret
}

func (b *ConstraintBuilder) printPoly(poly []term) string {
    ret := b.printTerm(poly[0])
    for i := 1; i < len(poly); i++ {
        t := b.printTerm(poly[i])
        if t[0] == '-' {
            ret += " - " + t[1:]
        } else {
            ret += " + " + t
        }
    }
    return ret
}

func (b *ConstraintBuilder) printPosPoly(poly []term) string {
    ret := ""
    for i := 0; i < len(poly); i++ {
        t := b.printTerm(poly[i])
        if t[0] != '-' {
            ret += " + " + t
        }
    }
    if len(ret) > 0 {
        return ret[3:]
    }
    return ret
}

func (b *ConstraintBuilder) printNegPoly(poly []term) string {
    ret := ""
    for i := 0; i < len(poly); i++ {
        t := b.printTerm(poly[i])
        if t[0] == '-' {
            ret += " + " + t [1:]
        }
    }
    return ret
}*/

func (b *ConstraintBuilder) printTmplHeader(w io.Writer) {
	if !b.outputTmpl {
		log.Fatal("We shouldn't be printing a template header if outputTmpl is false")
	}

    fmt.Fprintf(w, "#*\n\n")

    fmt.Fprintf(w, "EXTERNAL VARS:\n")
    for _, v := range b.extVars {
		fmt.Fprintf(w, "%v\n", v)
    }

    fmt.Fprintf(w, "\nNUM INTERNAL VARS: %v\n", b.nextSubcript)
    fmt.Fprintf(w, "NUM CONSTRAINTS: %v\n", b.matNumCons)
    fmt.Fprintf(w, "Aij: %v\n", b.GetAij())
    fmt.Fprintf(w, "Bij: %v\n", b.GetBij())
    fmt.Fprintf(w, "Cij: %v\n", b.GetCij())

    fmt.Fprintf(w, "\n*#\n")
}

func (b *ConstraintBuilder) WriteFiles() {
	if b.buildSpec {
		b.outputSpec(b.specFile)
		b.specFile.Close()
	}
	if b.buildPWS {
		b.outputPWS(b.pwsFile)
		b.pwsFile.Close()
	}
	if b.buildQAP {
		 for _, m := range b.matFiles {
		 	m.Close()
		 }
	}
	if b.f1IndexFile != nil {
		b.outputFIndex(b.f1IndexFile)
		b.f1IndexFile.Close()
	}
}

func (b *ConstraintBuilder) outputPWS(w io.Writer) {
    if b.buildSpec {
        log.Fatal("Can't output PWS: buildSpec is true")
    }
    if b.outputTmpl {
		b.printTmplHeader(w)
    }
    
    util.CopyOrDie(w, b.cmdsFile)
}

func (b *ConstraintBuilder) printVar(w io.Writer, varName string) {
    fmt.Fprintf(w, "%v //__merkle_%v uint bits %v\n", varName, varName, b.getVarNumBits(varName))
}

func (b *ConstraintBuilder) outputSpec(w io.Writer) {
    if !b.buildSpec {
        log.Fatal("Can't output spec: buildSpec is false")
    }
    
    // When creating something that can be spliced into an existing spec file, we don't need inputs
    // and outputs
    
//  fmt.Fprintf(w, "START_INPUT\n")
//  for i := 0; i < b.inputNum; i++ {
//      b.printVar(w, fmt.Sprintf("I%v", i))
//  }
//  fmt.Fprintf(w, "END_INPUT\n\n")
//  
//  fmt.Fprintf(w, "START_OUTPUT\n")
//  for i := b.inputNum; i < b.nextInputOutputSub; i++ {
//      b.printVar(w, fmt.Sprintf("O%v", i))
//  }
//  fmt.Fprintf(w, "END_OUTPUT\n\n")
    
    fmt.Fprintf(w, "START_VARIABLES\n")
    for _, v := range b.extVars {
        b.printVar(w, v)
    }
    for i := 0; i < b.nextSubcript; i++ {
        b.printVar(w, fmt.Sprintf("V%v", i))
    }
    fmt.Fprintf(w, "END_VARIABLES\n\n")

    fmt.Fprintf(w, "START_CONSTRAINTS\n")
    util.CopyOrDie(w, b.cmdsFile)
    fmt.Fprintf(w, "END_CONSTRAINTS\n")
}

func (b *ConstraintBuilder) outputFIndex(w io.Writer) {
    for i := 0; i < b.nextSubcript; i++ {
        fmt.Fprintf(w, "%v ", i)
    }
}

func (b *ConstraintBuilder) GetAij() int {
    return b.numNonZero[0]
}

func (b *ConstraintBuilder) GetBij() int {
    return b.numNonZero[1]
}

func (b *ConstraintBuilder) GetCij() int {
    return b.numNonZero[2]
}

func (b *ConstraintBuilder) GetConstraintNum() int {
    i := 1
    for i < (b.matNumCons + 1) {
        i *= 2
    }
    return i - 1
}

func (b *ConstraintBuilder) GetVarsNum() int {
    return b.nextSubcript
}

func (b *ConstraintBuilder) GetInputNum() int {
    return b.inputNum
}

func (b *ConstraintBuilder) GetOutputNum() int {
    return b.nextInputOutputSub - b.inputNum
}

func (b *ConstraintBuilder) PrintParams(w io.Writer) {
    fmt.Fprintf(w,
`num_cons = %v;
num_inputs = %v;
num_outputs = %v;
num_vars = %v;
num_aij = %v;
num_bij = %v;
num_cij = %v;
`,
        b.GetConstraintNum(),
        b.GetInputNum(),
        b.GetOutputNum(),
        b.GetVarsNum(),
        b.GetAij(),
        b.GetBij(),
        b.GetCij())
}

func (b *ConstraintBuilder) DiffVars(prevVars int) int {
    return b.GetVarsNum() - prevVars
}
