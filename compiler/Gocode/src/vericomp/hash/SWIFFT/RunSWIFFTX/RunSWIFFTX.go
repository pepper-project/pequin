package main

import (
	. "vericomp/hash/SWIFFT/constraints/ref"
	"os"
	"flag"
	"log"
)

func main() {
	var swifftOnly bool
	flag.BoolVar(&swifftOnly, "swifftOnly", false, "")
	
	var consFileName string
	flag.StringVar(&consFileName, "constraintsFile", "", "")
	
	flag.Parse()
	
	consFile, err := os.Create(consFileName)
	if err != nil {
		log.Fatal(err)
	}
	
	sw := NewSWIFFTX(swifftOnly, consFile)
	sw.GenConstraints()
	sw.OutputConstraints()
	sw.OutputNumVars(os.Stderr)
}

