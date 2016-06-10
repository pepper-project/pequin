package ref

import (
	. "vericomp/hash/SWIFFT"
	. "vericomp/hash/SWIFFT/impl"
)


// The powers of OMEGA in the field.
var omegaPowers = make([]int, (2 * N) + 1);


///////////////////////////////////////////////////////////////////////////////////////////////
// Helper functions implementation portion.
///////////////////////////////////////////////////////////////////////////////////////////////

func CalcOmegaPowers() {
	omegaPowers[0] = 1;
	
	for i := 1; i <= (2 * N); i++ { 
		omegaPowers[i] = (omegaPowers[i - 1] * OMEGA) % FIELD_SIZE;
	}
}

func BitReverse(index int) int {
	reversed := 0

	for bit := 0; bit < LOGN; bit++ {
		reversed = (reversed << 1) | (index & 1);
		index >>= 1
	}

	return reversed;
}

func InitializeSWIFFTX() {
  CalcOmegaPowers();
}

func DFT(input []int, output []int) {
	for i := 0; i < N; i++ { 
		sum := 0;

		for k := 0; k < N; k++ { 
			// The 2 is here because omega^2 is a primitive 64'th root in our field.
			power := k * (2 * i);
			sum += input[k] * omegaPowers[power % (2 * N)];
		}

		output[i] = sum % FIELD_SIZE;
	}
}

func memset(p []byte, val byte, length int) {
	for i:= 0; i < length; i++ {
		p[i] = val
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////
//
// SWIFFT/X functions implementation portion.
//
///////////////////////////////////////////////////////////////////////////////////////////////

func ComputeSingleSWIFFT(input []byte, m int, output []byte, a []int) {
	var carry uint8 = 0
	var in []byte = input
	var result = make([]int, N)

	for j := 0; j < m; j++ {
		// Convert N/8 input chars block to an array of N bits:
		var inputBits = make([]int, N);
		var reversedInputBits = make([]int, N);
		var dft = make([]int, N);
		
		for i := 0; i < N; i++ {
			inputBits[i] = int((in[i / 8] >> uint(i % 8)) & 1);
		}

		// Apply index bit reversal permutation to the N inputs (we defined the function like this,
		// so we do NOT need to do this when we perform FFT in the optimized version).
		for i := 0; i < N; i++ { 
			reversedInputBits[i] = inputBits[BitReverse(i)];
		}

		// Multiply by powers of omega:
		for i := 0; i < N; i++ {
			reversedInputBits[i] *= omegaPowers[i];
		}

		// DFT:
        DFT(reversedInputBits, dft);

		// Multiply by A's coefficients and sum the result up:
		for i := 0; i < N; i++ {
			result[i] += dft[i] * a[i];
		}
		
		in = in[8:]
		a = a[64:]
	}

	// Perform modular reduction of the result to get values in the field:
	for i := 0; i < N; i++ { 
		result[i] %= FIELD_SIZE;
	}

	// Convert results in our field into results in {0,...,256} using base change:
	for i := 0; i < 8; i++ {
		var carryBit int = TranslateToBase256(result[8*i:], output[8*i:]);
		carry |= uint8(carryBit << uint(i));
	}

	output[N] = carry;
}

func ComputeSingleSWIFFTX(input []byte, output []byte, doSmooth bool) {
	var intermediate = make([]byte, N * 3 + 8);
	var carry0, carry1, carry2 byte;

	// Do the three SWIFFTS while remembering the three carry bytes (each carry byte gets 
	// overriden by the following SWIFFT):
	ComputeSingleSWIFFT(input, M, intermediate, As);
	carry0 = intermediate[N];  

	ComputeSingleSWIFFT(input, M, intermediate[N:], As[N*M:]);
	carry1 = intermediate[2 * N];  

	ComputeSingleSWIFFT(input, M, intermediate[2*N:], As[2*N*M:]);
	carry2 = intermediate[3 * N];  

	// Put the three carry bytes into their place:
	intermediate[3 * N] = carry0;
	intermediate[(3 * N) + 1] = carry1;
	intermediate[(3 * N) + 2] = carry2;

	// Pad the intermediate output with 5 zeroes.
	memset(intermediate[3*N + 3:], 0, 5);

	// Apply the S-Box:
	for i := 0; i < (3 * N) + 8; i++ {
		intermediate[i] = SBox[intermediate[i]];
	}

	// The final second-tier SWIFFT:
	ComputeSingleSWIFFT(intermediate, (3 * EIGHTH_N) + 1 , output, As);

	// If it's the last compression function block, we smooth the result (denoted by 
	// 'FinalTransform' in the accompanying submission document).
	if doSmooth {
		var outputBits = make([]uint8, (N + 1) * 8)

		// Store the output temporarily as a bunch of bits:
		for i := 0; i < (N + 1) * 8; i++ {
			outputBits[i] = (output[i / 8] >> uint(i % 8)) & 1;
		}

		for i := 0; i < N + 1; i++ {
			output[i] = 0;
		}

		for i := 0; i < (N + 1) * 8; i++ { 
			if outputBits[i] == 1 {
				// The (N * M) is because we use the second (not first) set of As. 
				var AsRow []int = As[N*M + ((i/N)*N):]; 

				var AShift int = i % N;

				for j := 0; j < N; j++ { 
					if j < AShift {
						output[j] -= byte(AsRow[N + j - AShift]);
					} else { 
						output[j] += byte(AsRow[j - AShift]);
					}
				}
			}
		}
	}
}