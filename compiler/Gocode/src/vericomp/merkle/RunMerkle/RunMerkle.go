package main

import (
	"vericomp/merkle"
	"vericomp/util"
	"os"
	"flag"
	"fmt"
	"strconv"
)

func main() {	
	var dbSize int
	flag.IntVar(&dbSize, "dbSize", 16, "")
	
	var hashType string
	flag.StringVar(&hashType, "hashType", "ggh", "")
	
	var fnames [3]string
	flag.StringVar(&fnames[0], "pwsFile", "", "")
	flag.StringVar(&fnames[1], "f1File", "", "")
	flag.StringVar(&fnames[2], "qapFile", "", "")
	
	flag.Parse()
	
	var files [3]*os.File
	util.OpenFiles(fnames[:], files[:])
	
	fmt.Fprintf(os.Stderr, "dbSize: %d\n", dbSize)
	
	var qapMats [3]*os.File
	for i := 0; i < 3; i++ {
		qapMats[i] = util.OpenTempFile("qapMat" + strconv.Itoa(i))
	}
	
	m := merkle.NewMerkle(dbSize, hashType, nil, files[0], qapMats[0], qapMats[1], qapMats[2], files[1], false)
//	m.GenSumConstraints()
//	m.GenGetPutConstraints()
	m.GenOp("get", 1)
	m.WriteFiles()
	
	qapFile := files[2]
	for i, f := range qapMats {
		util.CopyOrDie(qapFile, f)
		if i < len(qapMats) - 1 {
			fmt.Fprintln(qapFile)
		}
	}
	qapFile.Close()

	m.GetCB().PrintParams(os.Stderr)
}

