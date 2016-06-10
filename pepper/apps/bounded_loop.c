#define SIZE 25
struct In {int vector[SIZE];};
struct Out {long long int subSum;};

/*
  Computes the sum of the first k elements of vector, such that 
  all of the summed elements are nonnegative.
*/
void compute(struct In *input, struct Out *output){
  int a;
  output->subSum = 0;
  for(a = 0; a < SIZE && input->vector[a] > -1; a++){
    output->subSum += input->vector[a];
  }
}
