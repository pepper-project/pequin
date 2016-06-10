package main

import (
	. "vericomp/merkle"
	. "vericomp/util"
	"flag"
	"runtime/pprof"
)

type GenMerkle struct {
	Op string
	DBSize int
	NumValueVars int
	HashType string
	OutputTmpl bool
	PWSFile, QAPAFile, QAPBFile, QAPCFile, SpecFile string

	CPUProfile, MEMProfile string
}

func (gm *GenMerkle) parseFlags() {
	flag.StringVar(&gm.Op, "op", "get", "")
	flag.IntVar(&gm.DBSize, "dbSize", 4, "")
	flag.IntVar(&gm.NumValueVars, "numValueVars", 1536, "")
	flag.StringVar(&gm.HashType, "hashType", "null", "")
	
	flag.StringVar(&gm.SpecFile, "specFile", "", "")
	flag.StringVar(&gm.PWSFile, "pwsFile", "", "")
	flag.StringVar(&gm.QAPAFile, "qapAFile", "", "")
	flag.StringVar(&gm.QAPBFile, "qapBFile", "", "")
	flag.StringVar(&gm.QAPCFile, "qapCFile", "", "")
	flag.BoolVar(&gm.OutputTmpl, "outputTmpl", false, "")
	
	flag.StringVar(&gm.CPUProfile, "cpuProfile", "", "write cpu profile to file")
	flag.StringVar(&gm.MEMProfile, "memProfile", "", "write memory profile to this file")

	flag.Parse()
}

func main() {
	gm := new(GenMerkle)
	gm.parseFlags()

	if gm.CPUProfile != "" {
        pprof.StartCPUProfile(OpenFile(gm.CPUProfile))
        defer pprof.StopCPUProfile()
    }
	
	m := NewMerkle(gm.DBSize, gm.HashType, OpenFile(gm.SpecFile), OpenFile(gm.PWSFile), 
				   OpenFile(gm.QAPAFile), OpenFile(gm.QAPBFile), OpenFile(gm.QAPCFile), nil, gm.OutputTmpl)
	m.GenOp(gm.Op, gm.NumValueVars)
	m.WriteFiles()

	if gm.MEMProfile != "" {
        pprof.WriteHeapProfile(OpenFile(gm.MEMProfile))
    }
}

