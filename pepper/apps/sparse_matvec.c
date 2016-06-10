#include <stdint.h>

#include "sparse_matvec.h"

// sparse matrix * vector multiplication
// N x N matrix with K nonzero elements
//
// input matrix is in CSR representation:
//     elms = <K-length array>
//     inds = <K-length array>
//     ptrs = <N+1-length array>


void compute( struct In *input, struct Out *output ) {
    int i;
    int j;

    for(i = 0; i < N; i++) {
        for(j = 0; j < K; j++) {
            int ip0 = input->ptrs[i];
            int ip1 = input->ptrs[i+1];
            if ( !(j < ip0) && (j < ip1) ) {
                int iej = input->elms[j];
                int inj = input->inds[j];
                int vij = input->vector[inj];
                output->out[i] += iej * vij;
            }
        }
    }
}
