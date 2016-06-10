package main

import (
	. "vericomp/hash/ggh"
	"os"
	"bytes"
	"fmt"
	"math/big"
	"crypto/rand"
	"log"
)

func makeIV() *bytes.Buffer {
	b := new(bytes.Buffer)

	max := big.NewInt(256)

	for i := 0; i < N*Q_BITS/8; i++ {
		if i % 8 == 0 {
			fmt.Fprintln(b)
		}

		v,_ := rand.Int(rand.Reader, max)
		fmt.Fprintf(b, "%v, ", v)
	}
	fmt.Fprintln(b)

	return b
}

func makeA() *bytes.Buffer {
	b := new(bytes.Buffer)
	
	max := big.NewInt(Q)
	
	for i := 0; i < N; i++ {
		for j := 0; j < M; j++ {
			if j % 8 == 0 {
				fmt.Fprintln(b)
			}
			
			v,_ := rand.Int(rand.Reader, max)
			fmt.Fprintf(b, "%v, ", v)
		}
		fmt.Fprintln(b)
	}
	
	return b
}

func createFile(name string) *os.File {
	f, err := os.Create(name)
	if err != nil {
		log.Fatal("Couldn't open file: " + name)
	}
	return f
}

func writeGoFile(ivBuf, aBuf *bytes.Buffer) {
	f := createFile("gghA.go")
	
	fmt.Fprintln(f, "package ggh")
	fmt.Fprintln(f)
	
	fmt.Fprintln(f, "var GGHIV = []byte {")
	f.Write(ivBuf.Bytes())
	fmt.Fprintln(f, "}")

	fmt.Fprintln(f)
	
	fmt.Fprintln(f, "var AMat = []int64 {")
	f.Write(aBuf.Bytes())
	fmt.Fprintln(f, "}")
	
	f.Close()
}

func writeCHeader(ivBuf, aBuf *bytes.Buffer) {
	f := createFile("gghA.h")

	fmt.Fprintln(f, "#ifndef GGHA_H_")
	fmt.Fprintln(f, "#define GGHA_H_")
	fmt.Fprintln(f)
	fmt.Fprintln(f, "#include <stdint.h>")
	fmt.Fprintln(f)
	
	fmt.Fprintln(f, "static const uint8_t GGHIV[] = {")
	f.Write(ivBuf.Bytes())
	fmt.Fprintln(f, "};")

	fmt.Fprintln(f)
	
	fmt.Fprintln(f, "static const uint32_t AMat[] = {")
	f.Write(aBuf.Bytes())
	fmt.Fprintln(f, "};")

	fmt.Fprintln(f, "#endif // GGHA_H_")
	
	f.Close()
}

func main() {
	ivBuf := makeIV()
	aBuf := makeA()
	writeGoFile(ivBuf, aBuf)
	writeCHeader(ivBuf, aBuf)
}