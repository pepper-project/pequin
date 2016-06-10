#include "ridge_regression.h"

void addMat(fix_t* A, fix_t* B, int m, int n){
  int i, j;
  for(i = 0; i < m; i++){
    for(j = 0;j < n; j++){
      A[i*n + j] = fix_add(A[i*n + j], B[i*n + j]);
    }
  }
}

fix_t* cholesky(fix_t* A, int d){
  int i, j, k;

  //Using the iteration provided in "Privacy Preserving Ridge Regression
  //on Hundreds of Millions of Records"
  //TODO: This iteration is ugly, todo replace it with 
  //block operations so it's easier to reason about
  for(j = 0; j < d; j++){
    for(k = 0; k < j; k++){
      for(i = j; i < d; i++){
        A[i*d + j] -= fix_mul(A[i*d + k], A[j*d + k]);
      }
    }
    A[j*d + j] = fix_sqrt(A[j*d + j]);
    for(k = j+1; k < d; k++){
      A[k*d + j] = fix_div(A[k*d + j], A[j*d + j]);
    }
  }

  return A;
}

//Solve Ax = b for x, where A is d x d upper triangular
//Trashes b
//A must have a nonzero diagonal
void backsubstituteUT(fix_t* x, fix_t* A, fix_t* b, int d){
  int i, j;
  for(i = d-1; i >= 0; i--){
    for(j = i + 1; j < d; j++){
      b[i] -= fix_mul(x[j], A[i*d + j]);
    }
    x[i] = fix_div(b[i],A[i*d + i]);
  }
}

//Solve Ax = b for x, where A is d x d lower triangular
//Trashes b
//A must have a nonzero diagonal
void backsubstituteLT(fix_t* x, fix_t* A, fix_t* b, int d){
  int i, j;
  for(i = 0; i < d; i++){
    for(j = 0; j < i; j++){
      b[i] -= fix_mul(x[j], A[i*d + j]);
    }
    x[i] = fix_div(b[i],A[i*d + i]);
  }
}

//Adds A * x to y, where A is d x d
void mvmul(fix_t* y, fix_t* A, fix_t* x, int d){
  int i,j;
  for(i = 0; i < d; i++){
    for(j = 0; j < d; j++){
      y[i] += fix_mul(A[i*d + j], x[j]);
    }
  }
}


int ridge_regression(fix_t regularization_k, patientDB* db, struct Out* output){
  int i, j, k;


  fix_t M [NUM_REGRESSION_IVARS * NUM_REGRESSION_IVARS] = {0};
  fix_t y [NUM_REGRESSION_IVARS] = {0};
  int d = NUM_REGRESSION_IVARS;

  for(i = 0; i < NUM_RECORDS; i++){
    patient* p = &db->records[i];
    fix_t a [NUM_REGRESSION_IVARS];
    for(j = 0; j < d; j++){
      a[j] = p->features[INDEPENDENT_VARS[j]];
    }
    fix_t dependent_var = p->features[DEPENDENT_VAR];

    /* Slow, shifts after each multiplication.
    //add a * a^T (outerproduct) to M
    for(j = 0; j < d; j++){
      for(k = 0; k < d; k++){
        M[j*d + k] += fix_mul(a[j], a[k]);
      }
    }
    //add dependent_var * a to y
    for(j = 0; j < d; j++){
      y[j] += fix_mul(dependent_var, a[j]);
    }
    */

    for(j = 0; j < d; j++){
      for(k = 0; k < d; k++){
        M[j*d + k] += a[j] * a[k]; //delay right shift
      }
    }
    for(j = 0; j < d; j++){
      y[j] += dependent_var * a[j]; //delay right shift
    }
  }

  //Regain the right shift we're missing on M and y
  for(j = 0; j < d; j++){
    for(k = 0; k < d; k++){
      M[j*d + k] = (fix_t)(M[j*d +k] >> FIX_SCALE_LOG2);
    }
  }
  for(j = 0; j < d; j++){
    y[j] = (fix_t)(y[j] >> FIX_SCALE_LOG2);
  }

  //Add lambda * identity to M
  for(i = 0; i < d; i++){
    M[i*d + i] += regularization_k;
  }

#if COMPUTE_SOLUTION_ERROR == 1
  //Compute A beta - b
  for(i = 0; i < d; i++){
    output->solnerror[i] = -y[i];
  }
  fix_t Mcopy [NUM_REGRESSION_IVARS * NUM_REGRESSION_IVARS];
  for(i = 0; i < d * d; i++){
    Mcopy[i] = M[i];
  }
#endif

  //The lower triangle of M is trashed.
  fix_t* L = cholesky(M, d);

  fix_t LT[NUM_REGRESSION_IVARS * NUM_REGRESSION_IVARS ];
  //Transpose L
  for(i = 0; i < d; i++){
    for(j = 0; j < d; j++){
      LT[i*d + j] = L[j*d + i];
    }
  }

  fix_t x[NUM_REGRESSION_IVARS];
  //This trashes y
  backsubstituteUT(x, LT, y, d);
  //This trashes x
  backsubstituteLT(output->beta, L, x, d);
#if COMPUTE_SOLUTION_ERROR == 1
  //Adds Acopy * beta to output->solnerror
  mvmul(output->solnerror, Mcopy, output->beta, d);
#endif
}

int compute(struct In* input, struct Out* output){
  patientDB db;
  //TODO unpack commitment instead of revealing GGH hash
  setcommitmentCK(&input->commitmentCK);
  hash_t hash;
  commitmentget(&hash, &input->commitment);
  hashget(&db, &hash);

  return ridge_regression(input->k, &db, output);
}
