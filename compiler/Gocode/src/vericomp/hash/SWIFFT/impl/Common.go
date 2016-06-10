package impl

import (
	. "vericomp/hash/SWIFFT"
)

func TranslateToBase256(input []int, output []byte) int {

	pairs := make([]int, EIGHTH_N / 2);

	for i := 0; i < EIGHTH_N; i += 2 {
		// input[i] + 257 * input[i + 1]
		pairs[i >> 1] = input[i] + input[i + 1] + (input[i + 1] << 8); 
	}

	for i := (EIGHTH_N / 2) - 1; i > 0; i-- {
		for j := i - 1; j < (EIGHTH_N / 2) - 1; j++ { 
			// pairs[j + 1] * 513, because 257^2 = 513 % 256^2.
			var temp int = pairs[j] + pairs[j + 1] + (pairs[j + 1] << 9);
			pairs[j] = temp & 0xffff;
			pairs[j + 1] += (temp >> 16);
		}
	}

	for i := 0; i < EIGHTH_N; i += 2 {
		output[i] = byte(pairs[i >> 1] & 0xff);
		output[i + 1] = byte((pairs[i >> 1] >> 8) & 0xff);
	}

	return (pairs[EIGHTH_N/2 - 1] >> 16);
}