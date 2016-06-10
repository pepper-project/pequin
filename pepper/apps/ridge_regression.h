#ifndef APPS_SFDL_RIDGE_REGRESSION_H
#define APPS_SFDL_RIDGE_REGRESSION_H

#include <fix_t.h>
#include <db.h>

const int NUM_RECORDS = 1024;

const int NUM_FEATURES = 8;

//What variables to regress over:
const int NUM_REGRESSION_IVARS = 7;
const int INDEPENDENT_VARS[NUM_REGRESSION_IVARS] = {0, 1, 2, 3, 4, 5, 6};
const int DEPENDENT_VAR = 7; //Arbitrary - column for patient recovery time

//If set to 1, compute A beta - b. The size of the
//elements of this vector is the forward error.
#define COMPUTE_SOLUTION_ERROR 0

struct patient {
  fix_t features [NUM_FEATURES];
};

typedef struct patient patient;

struct patientDB {
  patient records [NUM_RECORDS];
};

typedef struct patientDB patientDB;

struct In {
  //hash_t commitment;
  commitmentCK_t commitmentCK;
  commitment_t commitment;
  fix_t k; //regularization parameter
};

struct Out {
  //Compute beta such that F(beta) = 
  //   Sum(yi - beta^T xi)^2 + lambda(norm(beta)^2)
  //is minimized for any labeled record (xi, yi)
  fix_t beta [NUM_REGRESSION_IVARS];

#if COMPUTE_SOLUTION_ERROR == 1
  //Vector contains A beta - b
  fix_t solnerror [NUM_REGRESSION_IVARS];
#endif
};

#endif //APPS_SFDL_RIDGE_REGRESSION_H
