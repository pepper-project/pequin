#include <stdint.h>
#include <db.h>
#include <binary_tree_int_hash_t.h>

typedef uint32_t KEY_t;
typedef uint64_t FName_t;
typedef uint64_t LName_t;
typedef uint32_t Age_t;
typedef uint32_t Major_t;
typedef uint32_t State_t;
typedef uint32_t PhoneNum_t;
typedef uint32_t Class_t;
typedef uint32_t Credits_t;
typedef uint32_t Average_t;

typedef struct Student {
    KEY_t KEY;
    FName_t FName;
    LName_t LName;
    Age_t Age;
    Major_t Major;
    State_t State;
    PhoneNum_t PhoneNum;
    Class_t Class;
    Credits_t Credits;
    Average_t Average;
} Student_t;

typedef struct StudentResult {
    KEY_t KEY;
    FName_t FName;
    LName_t LName;
    Age_t Age;
    Major_t Major;
    State_t State;
    PhoneNum_t PhoneNum;
    Class_t Class;
    Credits_t Credits;
    Average_t Average;
} StudentResult_t;

struct In {hash_t hash;};
struct Out {uint32_t rows; StudentResult_t result[3];};

int compute(struct In *input, struct Out *output) {
    uint32_t tempInt, tempRowID;
    uint32_t nextRowID, numberOfRows, rowOffset, i;
    Age_t tempAge;

    Student_t tempStudent, tempOldStudent;
    tree_t Age_index;
    tree_result_set_t result;
    hash_t tempHash;

    tree_init(&Age_index);
    /*SELECT KEY, Age FROM Student WHERE Age < 24*/
    Age_index.root = input->hash;
    tempAge = 24;

    tree_find_lt(&(Age_index), (tempAge), FALSE, &(result));
    output->rows = result.num_results;
    for (i = 0; i < 3; i++) {
        if (i < result.num_results) {
            hashget(&(tempStudent), &(result.results[i].value));
            output->result[i].KEY = tempStudent.KEY;
            output->result[i].FName = tempStudent.FName;
            output->result[i].LName = tempStudent.LName;
            output->result[i].Age = tempStudent.Age;
            output->result[i].Major = tempStudent.Major;
            output->result[i].State = tempStudent.State;
            output->result[i].PhoneNum = tempStudent.PhoneNum;
            output->result[i].Class = tempStudent.Class;
            output->result[i].Credits = tempStudent.Credits;
            output->result[i].Average = tempStudent.Average;
        } else {
            output->result[i].KEY = 0;
            output->result[i].FName = 0;
            output->result[i].LName = 0;
            output->result[i].Age = 0;
            output->result[i].Major = 0;
            output->result[i].State = 0;
            output->result[i].PhoneNum = 0;
            output->result[i].Class = 0;
            output->result[i].Credits = 0;
            output->result[i].Average = 0;
        }
    }
    return 0;
}
