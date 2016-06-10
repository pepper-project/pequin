package ref

import (
	. "vericomp"
	. "vericomp/hash/SWIFFT"
	"io"
	"fmt"
	"os"
)


type swifftx struct {
    cb *ConstraintBuilder
    
    // The powers of OMEGA in the field.
	omegaPowers []string
	aS []string
	sBox []string
	
	swifftOnly bool
	
	numSetupVars, numGenVars, numSWIFFTVars, numDFTVars, 
		numSplitVars, numMultOmegaVars, numMultAVars, numModVars, 
		numTransVars, numSboxVars, numSmoothVars int 
}

func NewSWIFFTX(swifftOnly bool, pwsFile *os.File) *swifftx {
	sw := new(swifftx)
	
	sw.swifftOnly = swifftOnly
	sw.cb = NewConstraintBuilder(nil, pwsFile, nil, nil, nil, nil, false)
	
	numVars := sw.cb.GetVarsNum()
	
	sw.calcOmegaPowers()
 	sw.makeAs()
 	if !swifftOnly {
  		sw.makeSBox()
  	}
  	
  	sw.numSetupVars += sw.cb.DiffVars(numVars)
  	
  	return sw
}

func (sw *swifftx) GenConstraints() {

	numVars := sw.cb.GetVarsNum()

	// TODO: Input and output variables are bytes. Is there a more efficient way?
	input := make([]string, SWIFFTX_INPUT_BLOCK_SIZE)
	for i, _ := range input {
		input[i] = sw.cb.NextInput()
	}
	output := make([]string, SWIFFTX_OUTPUT_BLOCK_SIZE)
	for i, _ := range output {
		output[i] = sw.cb.NextOutput()
	}
	
	out := make([]string, len(output))
	
	if sw.swifftOnly {
		sw.computeSingleSWIFFT(input, M, out, sw.aS)
	} else {
		sw.computeSingleSWIFFTX(input, out, true)
	}
	
	for i, v := range out {
		sw.cb.Assignment(output[i], v)
	}
	
	sw.numGenVars += sw.cb.DiffVars(numVars)
}

func (sw *swifftx) OutputConstraints() {
	sw.cb.WriteFiles()
}

func (sw *swifftx) OutputNumVars(writer io.Writer) {
	fmt.Fprintf(writer, "numSetupVars: %v\n", sw.numSetupVars)
	fmt.Fprintf(writer, "numGenVars: %v\n", sw.numGenVars)
	fmt.Fprintf(writer, "numSWIFFTVars: %v\n", sw.numSWIFFTVars)
	fmt.Fprintf(writer, "numSplitVars: %v\n", sw.numSplitVars)
	fmt.Fprintf(writer, "numMultOmegaVars: %v\n", sw.numMultOmegaVars)
	fmt.Fprintf(writer, "numDFTVars: %v\n", sw.numDFTVars)
	fmt.Fprintf(writer, "numMultAVars: %v\n", sw.numMultAVars)
	fmt.Fprintf(writer, "numModVars: %v\n", sw.numModVars)
	fmt.Fprintf(writer, "numTransVars: %v\n", sw.numTransVars)
	fmt.Fprintf(writer, "numSboxVars: %v\n", sw.numSboxVars)
	fmt.Fprintf(writer, "numSmoothVars: %v\n", sw.numSmoothVars)
}

///////////////////////////////////////////////////////////////////////////////////////////////
// Helper functions implementation portion.
///////////////////////////////////////////////////////////////////////////////////////////////

func (sw *swifftx) calcOmegaPowers() {
	size := (2 * N) + 1
	omegaPowers := make([]int, (2 * N) + 1)
	sw.omegaPowers = make([]string, size)
	
	omegaPowers[0] = 1;
	sw.omegaPowers[0] = sw.cb.SignedConstant(omegaPowers[0])
	
	for i := 1; i <= (2 * N); i++ { 
		omegaPowers[i] = (omegaPowers[i - 1] * OMEGA) % FIELD_SIZE;
		sw.omegaPowers[i] = sw.cb.SignedConstant(omegaPowers[i])
	}
}

func (sw *swifftx) makeAs() {
	var theAs []int
	if sw.swifftOnly {
		theAs = As[:N*M]
	} else {
		theAs = As
	}
	
	sw.aS = make([]string, len(theAs))
	for i, a := range theAs {
		sw.aS[i] = sw.cb.SignedConstant(a)
	}
}

func (sw *swifftx) makeSBox() {
	sw.sBox = make([]string, len(SBox))
	for i, s := range SBox {
		sw.sBox[i] = sw.cb.Constant(uint(s))
	}
}

func (sw *swifftx) bitReverse(index int) int {
	reversed := 0

	for bit := 0; bit < LOGN; bit++ {
		reversed = (reversed << 1) | (index & 1);
		index >>= 1
	}

	return reversed;
}

// Calculates Discrete Fourier Transform.
//
// Parameters:
// - input: the input. Assumed to be reversed.
// - output: the result.
func (sw *swifftx) dft(input []string, output []string) {

	numVars := sw.cb.GetVarsNum()
	
	for i := 0; i < N; i++ { 
		var sum string = sw.cb.Zero;

		for k := 0; k < N; k++ { 
			// The 2 is here because omega^2 is a primitive 64'th root in our field.
			power := k * (2 * i);
			sum = sw.cb.Add(sum, sw.cb.Mult(input[k], sw.omegaPowers[power % (2 * N)]))
		}

		// TODO: Do we need to reduce mod the field size here because I think we just do it later?
		//output[i] = sum % FIELD_SIZE;
		output[i] = sum
	}
	
	sw.numDFTVars += sw.cb.DiffVars(numVars)
}

func (sw *swifftx) translateToBase256(input []string, output []string) string {

//	pairs := make([]int, EIGHTH_N / 2);
//
//	for i := 0; i < EIGHTH_N; i += 2 {
//		// input[i] + 257 * input[i + 1]
//		pairs[i >> 1] = input[i] + input[i + 1] + (input[i + 1] << 8); 
//	}
//
//	for i := (EIGHTH_N / 2) - 1; i > 0; i-- {
//		for j := i - 1; j < (EIGHTH_N / 2) - 1; j++ { 
//			// pairs[j + 1] * 513, because 257^2 = 513 % 256^2.
//			var temp int = pairs[j] + pairs[j + 1] + (pairs[j + 1] << 9);
//			pairs[j] = temp & 0xffff;
//			pairs[j + 1] += (temp >> 16);
//		}
//	}
//
//	for i := 0; i < EIGHTH_N; i += 2 {
//		output[i] = byte(pairs[i >> 1] & 0xff);
//		output[i + 1] = byte((pairs[i >> 1] >> 8) & 0xff);
//	}
//
//	return (pairs[EIGHTH_N/2 - 1] >> 16);

	numVars := sw.cb.GetVarsNum()
	carry := sw.cb.Base257ToBase256(input, output)
	sw.numTransVars += sw.cb.DiffVars(numVars)
	
	return carry
}

func (sw *swifftx) memset(p []string, val string, length int) {
	for i:= 0; i < length; i++ {
		p[i] = val
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////
//
// SWIFFT/X functions implementation portion.
//
///////////////////////////////////////////////////////////////////////////////////////////////

func (sw *swifftx) computeSingleSWIFFT(input []string, m int, output []string, a []string) {
	
	numVars := sw.cb.GetVarsNum()
	
	var in []string = input
	
	var result = make([]string, N)
	sw.memset(result, sw.cb.Zero, len(result))

	for j := 0; j < m; j++ {
		numVars2 := sw.cb.GetVarsNum()
		
		// Convert N/8 input chars block to an array of N bits:
		var inputBits = make([]string, N);
		var reversedInputBits = make([]string, N);
		var dft = make([]string, N);
		
		for i := 0; i < EIGHTH_N; i++ {
			bits := sw.cb.Split(in[i], 8)
			for k := 0; k < 8; k++ {
				inputBits[i*8 + k] = bits[k]
			}
		}
		
		sw.numSplitVars += sw.cb.DiffVars(numVars2)

		// Apply index bit reversal permutation to the N inputs (we defined the function like this,
		// so we do NOT need to do this when we perform FFT in the optimized version).
		for i := 0; i < N; i++ { 
			reversedInputBits[i] = inputBits[sw.bitReverse(i)];
		}

		numVars2 = sw.cb.GetVarsNum()
		// Multiply by powers of omega:
		for i := 0; i < N; i++ {
			reversedInputBits[i] = sw.cb.Mult(reversedInputBits[i], sw.omegaPowers[i])
		}
		sw.numMultOmegaVars += sw.cb.DiffVars(numVars2)

		// dft:
        sw.dft(reversedInputBits, dft);

		numVars2 = sw.cb.GetVarsNum()
		// Multiply by A's coefficients and sum the result up:
		for i := 0; i < N; i++ {
			result[i] = sw.cb.Add(result[i], sw.cb.Mult(dft[i], a[i]))
		}
		sw.numMultAVars += sw.cb.DiffVars(numVars2)
		
		in = in[EIGHTH_N:]
		a = a[64:]
	}

	numVars2 := sw.cb.GetVarsNum()
	// Perform modular reduction of the result to get values in the field:
	for i := 0; i < N; i++ { 
		// How many bits could result[i] be? Let's say 32.
		result[i] = sw.cb.Mod(result[i], 32, 9)
	}
	sw.numModVars += sw.cb.DiffVars(numVars2)

	// Convert results in our field into results in {0,...,256} using base change:
	var carryBits = make([]string, 8)
	for i := 0; i < 8; i++ {
		// 7 - i because Combine() is big endian
		carryBits[7 - i] = sw.translateToBase256(result[8*i:], output[8*i:])
	}

	output[N] = sw.cb.Combine(carryBits)
	
	sw.numSWIFFTVars += sw.cb.DiffVars(numVars)
}

func (sw *swifftx) computeSingleSWIFFTX(input []string, output []string, doSmooth bool) {
	var intermediate = make([]string, N * 3 + 8);
	var carry0, carry1, carry2 string;

	// Do the three SWIFFTS while remembering the three carry bytes (each carry byte gets 
	// overriden by the following SWIFFT):
	sw.computeSingleSWIFFT(input, M, intermediate, sw.aS);
	carry0 = intermediate[N];  

	sw.computeSingleSWIFFT(input, M, intermediate[N:], sw.aS[N*M:]);
	carry1 = intermediate[2 * N];  

	sw.computeSingleSWIFFT(input, M, intermediate[2*N:], sw.aS[2*N*M:]);
	carry2 = intermediate[3 * N];  

	// Put the three carry bytes into their place:
	intermediate[3 * N] = carry0;
	intermediate[(3 * N) + 1] = carry1;
	intermediate[(3 * N) + 2] = carry2;

	// Pad the intermediate output with 5 zeroes.
	sw.memset(intermediate[3*N + 3:], sw.cb.Zero, 5);

	numVars := sw.cb.GetVarsNum()
	// Apply the S-Box:
	for i := 0; i < (3 * N) + 8; i++ {
		intermediate[i] = sw.cb.ArrayGet(sw.sBox, intermediate[i])
	}
	sw.numSboxVars += sw.cb.DiffVars(numVars)

	// The final second-tier SWIFFT:
	sw.computeSingleSWIFFT(intermediate, (3 * EIGHTH_N) + 1 , output, sw.aS);

	numVars = sw.cb.GetVarsNum()
	// If it's the last compression function block, we smooth the result (denoted by 
	// 'FinalTransform' in the accompanying submission document).
	if doSmooth {
		var outputBits = make([]string, (N + 1) * 8)

		// Store the output temporarily as a bunch of bits:		
		for i := 0; i < N + 1; i++ {
			bits := sw.cb.Split(output[i], 8)
			for k := 0; k < 8; k++ {
				outputBits[i*8 + k] = bits[k]
			}
		}

		for i := 0; i < N + 1; i++ {
			output[i] = sw.cb.Zero
		}

// TODO: Can I get away with this? I replaced a conditional where outputBits[i] == 1 with a
// multiply by outputBits[i] on the theory that since outputBits[i] is a bit, it has the same effect.
		
		for i := 0; i < (N + 1) * 8; i++ { 
//			if outputBits[i] == 1 {	
			// The (N * M) is because we use the second (not first) set of As. 
			var AsRow []string = sw.aS[N*M + ((i/N)*N):]; 

			var AShift int = i % N;

			for j := 0; j < N; j++ { 
				if j < AShift {
					output[j] = sw.cb.Sub(output[j], sw.cb.Mult(outputBits[i], AsRow[N + j - AShift]))
				} else { 
					output[j] = sw.cb.Add(output[j], sw.cb.Mult(outputBits[i], AsRow[j - AShift]))
				}
			}
//			}
		}
	}
	
	sw.numSmoothVars += sw.cb.DiffVars(numVars)
}