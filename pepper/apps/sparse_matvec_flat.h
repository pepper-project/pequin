
#define N 5
#define K 10 

// sparse matrix * vector multiplication
// N x N matrix with K nonzero elements
//
// input matrix is in CSR representation:
//     elms = <K-length array>
//     inds = <K-length array>
//     ptrs = <N+1-length array>

struct In { int vector[N]; int elms[K]; int inds[K]; int ptrs[N+1]; };
struct Out { int out[N]; };

