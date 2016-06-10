package opt

import (
	. "vericomp/hash/SWIFFT"
	. "vericomp/hash/SWIFFT/impl"
)

// The size of the inner FFT lookup table:
const W = 8


// Calculates the sum and the difference of two numbers.
//
// Parameters:
// - A: the first operand. After the operation stores the sum of the two operands.
// - B: the second operand. After the operation stores the difference between the first and the
//   second operands.
func ADD_SUB(A *int, B *int) {
	var temp int = *B
	*B = *A - *B
	*A = *A + temp
}
// Quickly reduces an integer modulo 257.
//
// Parameters:
// - A: the input.
func Q_REDUCE(A int) int {
	return (A & 0xff) - (A >> 8)
}



// Since we need to do the setup only once, this is the indicator variable:
var wasSetupDone bool = false;

// This array stores the powers of omegas that correspond to the indices, which are the input 
// values. Known also as the "outer FFT twiddle factors".
var multipliers = make([]int, N)

// This array stores the powers of omegas, multiplied by the corresponding values.
// We store this table to save computation time.
//
// To calculate the intermediate value of the compression function (the first out of two 
// stages), we multiply the k-th bit of x_i by w^[(2i + 1) * k]. {x_i} is the input to the 
// compression function, i is between 0 and 31, x_i is a 64-bit value.
// One can see the formula for this (intermediate) stage in the SWIFFT FSE 2008 paper -- 
// formula (2), section 3, page 6.
var fftTable = make([]int, 256 * EIGHTH_N)


///////////////////////////////////////////////////////////////////////////////////////////////
// Helper functions implementation portion.
///////////////////////////////////////////////////////////////////////////////////////////////

// Translates an input integer into the range (-FIELD_SIZE / 2) <= result <= (FIELD_SIZE / 2).
//
// Parameters:
// - x: the input integer.
//
// Returns:
// - The result, which equals (x MOD FIELD_SIZE), such that |result| <= (FIELD_SIZE / 2).
func Center(x int) int { 
	var result int = x % FIELD_SIZE

	if result > (FIELD_SIZE / 2) {
		result -= FIELD_SIZE;
	}

	if result < (FIELD_SIZE / -2) { 
		result += FIELD_SIZE;
	}

	return result; 
}

// Calculates bit reversal permutation.
//
// Parameters:
// - input: the input to reverse.
// - numOfBits: the number of bits in the input to reverse.
//
// Returns:
// - The resulting number, which is obtained from the input by reversing its bits.
func ReverseBits(input int, numOfBits int) int {
	var reversed int = 0;

	for input |= numOfBits; input > 1; input >>= 1 {
		reversed = (reversed << 1) | (input & 1);
	}

	return reversed;
}


func InitializeSWIFFTX() {
	// The powers of OMEGA
	omegaPowers := make([]int, 2 * N) 
	omegaPowers[0] = 1;

	if wasSetupDone {
		return;
	}

	for i := 1; i < (2 * N); i++ {
		omegaPowers[i] = Center(omegaPowers[i - 1] * OMEGA);  
	}

	for i := 0; i < (N / W); i++ {
		for j := 0; j < W; j++ {
			multipliers[(i << 3) + j] = omegaPowers[ReverseBits(i, N / W) * (2 * j + 1)];
		}
	}

	for x := 0; x < 256; x++ {
		for j := 0; j < 8; j++ {
			var temp int = 0
			for k := 0; k < 8; k++ {
				temp += omegaPowers[(EIGHTH_N * (2 * j + 1) * ReverseBits(k, W)) % (2 * N)] * ((x >> uint(k)) & 1);
			}
	
			fftTable[(x << 3) + j] = Center(temp);
		}
	}

	wasSetupDone = true;
}


func FFT(input []byte, output []int) {

	var mult []int = multipliers
	var F0, F1, F2, F3, F4, F5, F6, F7, F8, F9,
					 F10, F11, F12, F13, F14, F15, F16, F17, F18, F19,
					 F20, F21, F22, F23, F24, F25, F26, F27, F28, F29,
					 F30, F31, F32, F33, F34, F35, F36, F37, F38, F39,
					 F40, F41, F42, F43, F44, F45, F46, F47, F48, F49,
					 F50, F51, F52, F53, F54, F55, F56, F57, F58, F59,
					 F60, F61, F62, F63 int
		
	// First loop unrolling:
	var table []int = fftTable[(input[0] << 3):]

	F0 = mult[0] * table[0];
	F8 = mult[1] * table[1];
	F16 = mult[2] * table[2];
	F24 = mult[3] * table[3];
	F32 = mult[4] * table[4];
	F40 = mult[5] * table[5];
	F48 = mult[6] * table[6];
	F56 = mult[7] * table[7];

	mult = mult[8:]
	table = fftTable[(input[1] << 3):]

	F1 = mult[0] * table[0];
	F9 = mult[1] * table[1];
	F17 = mult[2] * table[2];
	F25 = mult[3] * table[3];
	F33 = mult[4] * table[4];
	F41 = mult[5] * table[5];
	F49 = mult[6] * table[6];
	F57 = mult[7] * table[7];
	
	mult = mult[8:]
	table = fftTable[(input[2] << 3):]

	F2 = mult[0] * table[0];
	F10 = mult[1] * table[1];
	F18 = mult[2] * table[2];
	F26 = mult[3] * table[3];
	F34 = mult[4] * table[4];
	F42 = mult[5] * table[5];
	F50 = mult[6] * table[6];
	F58 = mult[7] * table[7];
		
	mult = mult[8:]
	table = fftTable[(input[3] << 3):]

	F3 = mult[0] * table[0];
	F11 = mult[1] * table[1];
	F19 = mult[2] * table[2];
	F27 = mult[3] * table[3];
	F35 = mult[4] * table[4];
	F43 = mult[5] * table[5];
	F51 = mult[6] * table[6];
	F59 = mult[7] * table[7];
	
	mult = mult[8:]
	table = fftTable[(input[4] << 3):]

	F4 = mult[0] * table[0];
	F12 = mult[1] * table[1];
	F20 = mult[2] * table[2];
	F28 = mult[3] * table[3];
	F36 = mult[4] * table[4];
	F44 = mult[5] * table[5];
	F52 = mult[6] * table[6];
	F60 = mult[7] * table[7];
	
	mult = mult[8:]
	table = fftTable[(input[5] << 3):]

	F5 = mult[0] * table[0];
	F13 = mult[1] * table[1];
	F21 = mult[2] * table[2];
	F29 = mult[3] * table[3];
	F37 = mult[4] * table[4];
	F45 = mult[5] * table[5];
	F53 = mult[6] * table[6];
	F61 = mult[7] * table[7];
	
	mult = mult[8:]
	table = fftTable[(input[6] << 3):]

	F6 = mult[0] * table[0];
	F14 = mult[1] * table[1];
	F22 = mult[2] * table[2];
	F30 = mult[3] * table[3];
	F38 = mult[4] * table[4];
	F46 = mult[5] * table[5];
	F54 = mult[6] * table[6];
	F62 = mult[7] * table[7];
	
	mult = mult[8:]
	table = fftTable[(input[7] << 3):]

	F7 = mult[0] * table[0];
	F15 = mult[1] * table[1];
	F23 = mult[2] * table[2];
	F31 = mult[3] * table[3];
	F39 = mult[4] * table[4];
	F47 = mult[5] * table[5];
	F55 = mult[6] * table[6];
	F63 = mult[7] * table[7];

	// Second loop unrolling:
	// Iteration 0:
	ADD_SUB(&F0, &F1)
	ADD_SUB(&F2, &F3)
	ADD_SUB(&F4, &F5)
	ADD_SUB(&F6, &F7)

	F3 <<= 4;  
	F7 <<= 4;

	ADD_SUB(&F0, &F2)
	ADD_SUB(&F1, &F3)
	ADD_SUB(&F4, &F6) 
	ADD_SUB(&F5, &F7)

	F5 <<= 2;
	F6 <<= 4;
	F7 <<= 6;

	ADD_SUB(&F0, &F4)
	ADD_SUB(&F1, &F5)
	ADD_SUB(&F2, &F6)
	ADD_SUB(&F3, &F7)

	output[0] = Q_REDUCE(F0);
	output[8] = Q_REDUCE(F1);
	output[16] = Q_REDUCE(F2);
	output[24] = Q_REDUCE(F3);
	output[32] = Q_REDUCE(F4);
	output[40] = Q_REDUCE(F5);
	output[48] = Q_REDUCE(F6);
	output[56] = Q_REDUCE(F7);

	// Iteration 1:
	ADD_SUB(&F8, &F9)
	ADD_SUB(&F10, &F11)
	ADD_SUB(&F12, &F13)
	ADD_SUB(&F14, &F15)

	F11 <<= 4;  
	F15 <<= 4;

	ADD_SUB(&F8, &F10)
	ADD_SUB(&F9, &F11)
	ADD_SUB(&F12, &F14) 
	ADD_SUB(&F13, &F15)

	F13 <<= 2;
	F14 <<= 4;
	F15 <<= 6;

	ADD_SUB(&F8, &F12)
	ADD_SUB(&F9, &F13)
	ADD_SUB(&F10, &F14)
	ADD_SUB(&F11, &F15)

	output[1] = Q_REDUCE(F8);
	output[9] = Q_REDUCE(F9);
	output[17] = Q_REDUCE(F10);
	output[25] = Q_REDUCE(F11);
	output[33] = Q_REDUCE(F12);
	output[41] = Q_REDUCE(F13);
	output[49] = Q_REDUCE(F14);
	output[57] = Q_REDUCE(F15);

	// Iteration 2:
	ADD_SUB(&F16, &F17)
	ADD_SUB(&F18, &F19)
	ADD_SUB(&F20, &F21)
	ADD_SUB(&F22, &F23)

	F19 <<= 4;  
	F23 <<= 4;

	ADD_SUB(&F16, &F18)
	ADD_SUB(&F17, &F19)
	ADD_SUB(&F20, &F22) 
	ADD_SUB(&F21, &F23)

	F21 <<= 2;
	F22 <<= 4;
	F23 <<= 6;

	ADD_SUB(&F16, &F20)
	ADD_SUB(&F17, &F21)
	ADD_SUB(&F18, &F22)
	ADD_SUB(&F19, &F23)

	output[2] = Q_REDUCE(F16);
	output[10] = Q_REDUCE(F17);
	output[18] = Q_REDUCE(F18);
	output[26] = Q_REDUCE(F19);
	output[34] = Q_REDUCE(F20);
	output[42] = Q_REDUCE(F21);
	output[50] = Q_REDUCE(F22);
	output[58] = Q_REDUCE(F23);

	// Iteration 3:
	ADD_SUB(&F24, &F25)
	ADD_SUB(&F26, &F27)
	ADD_SUB(&F28, &F29)
	ADD_SUB(&F30, &F31)

	F27 <<= 4;  
	F31 <<= 4;

	ADD_SUB(&F24, &F26)
	ADD_SUB(&F25, &F27)
	ADD_SUB(&F28, &F30) 
	ADD_SUB(&F29, &F31)

	F29 <<= 2;
	F30 <<= 4;
	F31 <<= 6;

	ADD_SUB(&F24, &F28)
	ADD_SUB(&F25, &F29)
	ADD_SUB(&F26, &F30)
	ADD_SUB(&F27, &F31)

	output[3] = Q_REDUCE(F24);
	output[11] = Q_REDUCE(F25);
	output[19] = Q_REDUCE(F26);
	output[27] = Q_REDUCE(F27);
	output[35] = Q_REDUCE(F28);
	output[43] = Q_REDUCE(F29);
	output[51] = Q_REDUCE(F30);
	output[59] = Q_REDUCE(F31);

	// Iteration 4:
	ADD_SUB(&F32, &F33)
	ADD_SUB(&F34, &F35)
	ADD_SUB(&F36, &F37)
	ADD_SUB(&F38, &F39)

	F35 <<= 4;  
	F39 <<= 4;

	ADD_SUB(&F32, &F34)
	ADD_SUB(&F33, &F35)
	ADD_SUB(&F36, &F38) 
	ADD_SUB(&F37, &F39)

	F37 <<= 2;
	F38 <<= 4;
	F39 <<= 6;

	ADD_SUB(&F32, &F36)
	ADD_SUB(&F33, &F37)
	ADD_SUB(&F34, &F38)
	ADD_SUB(&F35, &F39)

	output[4] = Q_REDUCE(F32);
	output[12] = Q_REDUCE(F33);
	output[20] = Q_REDUCE(F34);
	output[28] = Q_REDUCE(F35);
	output[36] = Q_REDUCE(F36);
	output[44] = Q_REDUCE(F37);
	output[52] = Q_REDUCE(F38);
	output[60] = Q_REDUCE(F39);

	// Iteration 5:
	ADD_SUB(&F40, &F41)
	ADD_SUB(&F42, &F43)
	ADD_SUB(&F44, &F45)
	ADD_SUB(&F46, &F47)

	F43 <<= 4;  
	F47 <<= 4;

	ADD_SUB(&F40, &F42)
	ADD_SUB(&F41, &F43)
	ADD_SUB(&F44, &F46) 
	ADD_SUB(&F45, &F47)

	F45 <<= 2;
	F46 <<= 4;
	F47 <<= 6;

	ADD_SUB(&F40, &F44)
	ADD_SUB(&F41, &F45)
	ADD_SUB(&F42, &F46)
	ADD_SUB(&F43, &F47)

	output[5] = Q_REDUCE(F40);
	output[13] = Q_REDUCE(F41);
	output[21] = Q_REDUCE(F42);
	output[29] = Q_REDUCE(F43);
	output[37] = Q_REDUCE(F44);
	output[45] = Q_REDUCE(F45);
	output[53] = Q_REDUCE(F46);
	output[61] = Q_REDUCE(F47);

	// Iteration 6:
	ADD_SUB(&F48, &F49)
	ADD_SUB(&F50, &F51)
	ADD_SUB(&F52, &F53)
	ADD_SUB(&F54, &F55)

	F51 <<= 4;  
	F55 <<= 4;

	ADD_SUB(&F48, &F50)
	ADD_SUB(&F49, &F51)
	ADD_SUB(&F52, &F54) 
	ADD_SUB(&F53, &F55)

	F53 <<= 2;
	F54 <<= 4;
	F55 <<= 6;

	ADD_SUB(&F48, &F52)
	ADD_SUB(&F49, &F53)
	ADD_SUB(&F50, &F54)
	ADD_SUB(&F51, &F55)

	output[6] = Q_REDUCE(F48);
	output[14] = Q_REDUCE(F49);
	output[22] = Q_REDUCE(F50);
	output[30] = Q_REDUCE(F51);
	output[38] = Q_REDUCE(F52);
	output[46] = Q_REDUCE(F53);
	output[54] = Q_REDUCE(F54);
	output[62] = Q_REDUCE(F55);

	// Iteration 7:
	ADD_SUB(&F56, &F57)
	ADD_SUB(&F58, &F59)
	ADD_SUB(&F60, &F61)
	ADD_SUB(&F62, &F63)

	F59 <<= 4;  
	F63 <<= 4;

	ADD_SUB(&F56, &F58)
	ADD_SUB(&F57, &F59)
	ADD_SUB(&F60, &F62) 
	ADD_SUB(&F61, &F63)

	F61 <<= 2;
	F62 <<= 4;
	F63 <<= 6;

	ADD_SUB(&F56, &F60)
	ADD_SUB(&F57, &F61)
	ADD_SUB(&F58, &F62)
	ADD_SUB(&F59, &F63)

	output[7] = Q_REDUCE(F56);
	output[15] = Q_REDUCE(F57);
	output[23] = Q_REDUCE(F58);
	output[31] = Q_REDUCE(F59);
	output[39] = Q_REDUCE(F60);
	output[47] = Q_REDUCE(F61);
	output[55] = Q_REDUCE(F62);
	output[63] = Q_REDUCE(F63);
}

// Calculates the FFT part of SWIFFT.
// We divided the SWIFFT calculation into two, because that way we could save 2 computations of
// the FFT part, since in the first stage of SWIFFTX the difference between the first 3 SWIFFTs
// is only the A's part.
//
// Parameters:
// - input: the input to FFT.
// - m: the input size divided by 8. The function performs m FFTs.
// - output: will store the result.
func SWIFFTFFT(input []byte, m int, output []int) {

	for i := 0; i < m; i++ {
		FFT(input[i*EIGHTH_N:], output[i*N:]);
	}
}

// Calculates the 'sum' part of SWIFFT, including the base change at the end.
// We divided the SWIFFT calculation into two, because that way we could save 2 computations of
// the FFT part, since in the first stage of SWIFFTX the difference between the first 3 SWIFFTs
// is only the A's part.
//
// Parameters:
// - input: the input. Of size 64 * m.
// - m: the input size divided by 64.
// - output: will store the result.
// - a: the coefficients in the sum. Of size 64 * m.
func SWIFFTSum(input []int, m int, output []byte, a []int) { 

	result := make([]int, N)
	var carry int = 0

	for j := 0; j < N; j++ { 
		var sum int = 0
		var f []int = input[j:]
		var k []int = a[j:]
		
		for i := 0; i < m; i++ {
			sum += f[i*N] * k[i*N];
		}

		result[j] = sum;
	}

	for j := 0; j < N; j++ { 
		result[j] = ((FIELD_SIZE << 22) + result[j]) % FIELD_SIZE;
	}
	
	for j := 0; j < 8; j++ { 
		var carryBit int = TranslateToBase256(result[(j << 3):], output[(j << 3):]);
		carry |= carryBit << uint(j);
	}

	output[N] = byte(carry);
}


func memset(p []byte, val byte, length int) {
	for i:= 0; i < length; i++ {
		p[i] = val
	}
}

func ComputeSingleSWIFFTX(input []byte, output []byte, doSmooth bool) {

	// Will store the result of the FFT parts:
	fftOut := make([]int, N * M)
	intermediate := make([]byte, N * 3 + 8)
	var carry0,carry1,carry2 byte

	// Do the three SWIFFTS while remembering the three carry bytes (each carry byte gets 
	// overriden by the following SWIFFT):

	// 1. Compute the FFT of the input - the common part for the first 3 SWIFFTs: 
	SWIFFTFFT(input, M, fftOut);

	// 2. Compute the sums of the 3 SWIFFTs, each using a different set of coefficients: 
	
	// 2a. The first SWIFFT:
	SWIFFTSum(fftOut, M, intermediate[:N], As);
	// Remember the carry byte:
	carry0 = intermediate[N];  

	// 2b. The second one:
	SWIFFTSum(fftOut, M, intermediate[N:2*N], As[M*N:]);
	carry1 = intermediate[2 * N];  

	// 2c. The third one:
	SWIFFTSum(fftOut, M, intermediate[2*N:], As[2*M*N:]);
	carry2 = intermediate[3 * N];  

	//2d. Put three carry bytes in their place
	intermediate[3 * N] = carry0;
	intermediate[(3 * N) + 1] = carry1;
	intermediate[(3 * N) + 2] = carry2;

	// Padding  intermediate output with 5 zeroes.
	memset(intermediate[3*N + 3:], 0, 5);

	// Apply the S-Box:
	for i := 0; i < (3 * N) + 8; i++ {
		intermediate[i] = SBox[intermediate[i]];
	}

	// 3. The final and last SWIFFT:
	SWIFFTFFT(intermediate, 3 * (N/8) + 1, fftOut);
	SWIFFTSum(fftOut,       3 * (N/8) + 1, output, As);
	
	if doSmooth {
		sum := make([]byte, N);

		for i := 0; i < (N + 1) * 8; i++ { 
			var AsRow []int 
			var AShift int;

			if  (output[i >> uint(3)] & (1 << uint(i & 7))) == 0 {
				continue;
			}

			AsRow = As[N * M + (i & ^(N - 1)):]
			AShift = i & 63;

			for j := AShift; j < N; j++ {
				sum[j] += byte(AsRow[j - AShift]);
			}

			for j := 0; j < AShift; j++ { 
				sum[j] -= byte(AsRow[N - AShift + j]);
			}
		}

		for i := 0; i < N; i++ {
			output[i] = sum[i];
		}

		output[N] = 0;
	}
}