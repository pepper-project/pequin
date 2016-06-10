package sha1

import (
    "math/big"
    "crypto/sha1"
    "fmt"
    "flag"
    . "vericomp"
)

const chunkSize = 512
const wordSize = 32
const chunkSizeInWord = chunkSize / wordSize
const internalStateSizeInWord = 80
const digestSize = 160

type sha1Hasher struct {
    builder *ConstraintBuilder
    zero, one string
    h1, h2, h3, h4, h5 string
    k1, k2, k3, k4 string
}

func newSHA1Hasher(builder *ConstraintBuilder) *sha1Hasher {
    h := &sha1Hasher{}
    h.builder = builder
    h.zero = h.builder.Constant(0)
    h.one = h.builder.Constant(1)
    h.h1 = h.builder.Constant(0x67452301)
    h.h2 = h.builder.Constant(0xEFCDAB89)
    h.h3 = h.builder.Constant(0x98BADCFE)
    h.h4 = h.builder.Constant(0x10325476)
    h.h5 = h.builder.Constant(0xC3D2E1F0)

    h.k1 = h.builder.Constant(0x5A827999)
    h.k2 = h.builder.Constant(0x6ED9EBA1)
    h.k3 = h.builder.Constant(0x8F1BBCDC)
    h.k4 = h.builder.Constant(0xCA62C1D6)

    return h
}

func (h *sha1Hasher) toBigEndianBits(x int64) []string {
    xbig := big.NewInt(x)
    xi := make([]string, 64)
    for i := 0; i < 64; i++ {
        if xbig.Bit(64 - i - 1) == 0 {
            xi[i] = h.zero
        } else {
            xi[i] = h.one
        }
    }
    return xi
}

func (h *sha1Hasher) preprocessing(input []string) []string {
    lengthAsBigEndian64Bits := h.toBigEndianBits(int64(len(input)))

    // preprocessing
    input = append(input, h.one)
    padding :=(chunkSize - ((len(input) + 64) % chunkSize)) % chunkSize
    for i := 0; i < padding; i++ {
        input = append(input, h.zero)
    }
    input = append(input, lengthAsBigEndian64Bits...)
    return input
}

func (h *sha1Hasher) block(chunk []string) {
    a, b, c, d, e := h.h1, h.h2, h.h3, h.h4, h.h5
    ai, bi, ci, di := h.builder.Split(a, wordSize), h.builder.Split(b, wordSize), h.builder.Split(c, wordSize), h.builder.Split(d, wordSize)
    w := make([][]string, internalStateSizeInWord)
    for j := 0; j < chunkSizeInWord; j++ {
        w[j] = chunk[j * wordSize : j * wordSize + wordSize]
    }
    for j := chunkSizeInWord; j < internalStateSizeInWord; j++ {
        w[j] = h.builder.Leftrotate(h.builder.Xor(h.builder.Xor(h.builder.Xor(w[j-3], w[j-8]), w[j-14]), w[j-16]), 1)
    }

    var fi []string
    var k string
    for j := 0; j < 80; j++ {
        if  j < 20 {
            fi, k = h.builder.Or(h.builder.And(bi, ci), h.builder.And(h.builder.Not(bi), di)), h.k1
        } else if j < 40 {
            fi, k = h.builder.Xor(h.builder.Xor(bi, ci), di), h.k2
        } else if j < 60 {
            fi, k = h.builder.Or(h.builder.Or(h.builder.And(bi, ci), h.builder.And(bi, di)), h.builder.And(ci, di)), h.k3
        } else {
            fi, k = h.builder.Xor(h.builder.Xor(bi, ci), di), h.k4
        }
        f := h.builder.Combine(fi)

        t1 := h.builder.Combine(h.builder.Leftrotate(ai, 5))
        t2 := h.builder.Mod(h.builder.Add(h.builder.Add(h.builder.Add(h.builder.Add(t1, f), e), k), h.builder.Combine(w[j])), wordSize + 3, wordSize)

        e, d, di = d, c, ci
        ci = h.builder.Leftrotate(bi, 30); c = h.builder.Combine(ci)
        b, bi = a, ai
        a = t2; ai = h.builder.Split(a, wordSize)
    }
    h.h1 = h.builder.Mod(h.builder.Add(h.h1, a), wordSize + 1, wordSize)
    h.h2 = h.builder.Mod(h.builder.Add(h.h2, b), wordSize + 1, wordSize)
    h.h3 = h.builder.Mod(h.builder.Add(h.h3, c), wordSize + 1, wordSize)
    h.h4 = h.builder.Mod(h.builder.Add(h.h4, d), wordSize + 1, wordSize)
    h.h5 = h.builder.Mod(h.builder.Add(h.h5, e), wordSize + 1, wordSize)

}

func (h *sha1Hasher) sha1(input, output []string) {
    input = h.preprocessing(input)

    numOfChunks := len(input) / chunkSize
    for i := 0; i < numOfChunks; i++ {
        chunk := input[i * chunkSize : i * chunkSize + chunkSize]
        h.block(chunk)
    }

    hi := make([][]string, 5)
    hi[0], hi[1], hi[2], hi[3], hi[4] = h.builder.Split(h.h1, wordSize), h.builder.Split(h.h2, wordSize), h.builder.Split(h.h3, wordSize), h.builder.Split(h.h4, wordSize), h.builder.Split(h.h5, wordSize)

    for i := 0; i < 5; i++ {
        for j := 0; j < wordSize; j++ {
            h.builder.Assignment(output[i * wordSize + j], hi[i][j])
        }
    }
}

func Reverse(s string) string {
    n := len(s)
    runes := make([]rune, n)
    for _, rune := range s {
        n--
        runes[n] = rune
    }
    return string(runes[n:])
}

func main() {
    flag.Parse()
    for _, input := range flag.Args() {
        h := sha1.New()
        /*num, _ := new(big.Int).SetString(input, 2)
        h.Write(num.Bytes())
        for _, b := range num.Bytes() {
            for i := 7; i >=0; i-- {
                fmt.Print((b & (1 << uint(i))) >> uint(i))
            }
        }
        fmt.Println()*/
        buf := make([]byte, 0)
        var temp byte
        for i := 0; i < len(input); i++ {
            if input[i] == '1' {
                temp += 1 << uint((7 - (i % 8)))
            }
            if i % 8 == 7 {
                buf = append(buf, temp)
                temp = 0
            }
        }
        if len(input) % 8 != 0 {
            buf = append(buf, temp)
        }
/*        for _, b := range buf {
            for i := 7; i >=0; i-- {
                fmt.Print((b & (1 << uint(i))) >> uint(i))
            }
        }
        fmt.Println()*/
        h.Write(buf)
        output := h.Sum(nil)
        for _, b := range output {
            for i := 7; i >=0; i-- {
                fmt.Print((b & (1 << uint(i))) >> uint(i))
            }
        }
        fmt.Println()
    }
    /*h.Reset()
    output := h.Sum(nil)
    for _, b := range output {
        for i := 7; i >=0; i-- {
            fmt.Print((b & (1 << uint(i))) >> uint(i))
        }
    }
    fmt.Println()*/

/*    h.Reset()
    num, _ = new(big.Int).SetString(Reverse(input), 2)
    h.Write(num.Bytes())
    output = h.Sum(nil)
    for _, b := range output {
        for i := 7; i >=0; i-- {
            fmt.Print((b & (1 << uint(i))) >> uint(i))
        }
    }
    fmt.Println()*/
}
