package util

import (
	"log"
	"os"
	"io"
	"io/ioutil"
)

func OpenTempFile(name string) *os.File {
    rw, err := ioutil.TempFile("", name + ".tmp.")
    if err != nil {
        log.Fatal(err)
    }
    return rw
}

func OpenFile(fname string) *os.File {
	if fname == "" {
		return nil
	}
	
	f, err := os.Create(fname)
	if err != nil {
		log.Fatal(err)
	} 
	return f
}

func OpenFiles(fnames []string, files []*os.File) {
	for i, n := range fnames {
		files[i] = OpenFile(n)
	}
}

func CopyOrDie(dst io.Writer, src *os.File) {
    src.Seek(0, 0)
    _, err := io.Copy(dst, src)
    if err != nil {
        log.Fatal(err)
    }
}