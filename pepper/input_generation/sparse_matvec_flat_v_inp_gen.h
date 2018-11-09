#include <apps/sparse_matvec_flat.h>

void sparse_matvec_flat_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {
  srand(time(NULL));
  // randomly distribute elements into rows
  int buckets[N] = {0,};
  int ptrs[N+1] = {0,};
  // first figure out how many go into each row
  for(int i=0; i < K; i++) {
      int r;
      do {
          r = rand() % N;
      } while (buckets[r] >= N);
      buckets[r]++;
  }
  // then compute the row pointers
  ptrs[0] = 0;
  for(int i=0; i < N; i++) {
      ptrs[i+1] = ptrs[i] + buckets[i];
  }

  int inds[K] = {0,};
  // distribute elemnts within each row
  for(int i=0; i < N; i++) {
      memset(buckets, 0, N * sizeof(int));
      int nelms = ptrs[i+1] - ptrs[i];

      // yes, this is quick and dirty
      while(nelms > 0) {
          int r = rand() % N;
          if (buckets[r] == 0) {
              buckets[r] = 1;
              nelms--;
          }
      }

      int k = ptrs[i];
      for(int j=0; j < N; j++) {
          if (buckets[j] != 0) {
              inds[k++] = j;
          }
      }
  }

  // input_q layout is
  // vector[N] (random values % 1024)
  // elms[K]   (random values % 1024)
  // inds[K]   (computed above)
  // ptrs[N+1] (compuated above)
  for(int i=0; i < N + K; i++) {
      mpq_set_ui(input_q[i], rand() % 1024, 1);
  }
  for(int i=0; i < K; i++) {
      mpq_set_ui(input_q[N+K+i], inds[i], 1);
  }
  for(int i=0; i < N+1; i++) {
      mpq_set_ui(input_q[N+K+K+i], ptrs[i], 1);
  }
}
