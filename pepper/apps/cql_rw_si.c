#include <stdint.h>
#include <db.h>
#include <avl_tree.h>

#include "cql_rw_si.h"

int compute(struct In *input, struct Out *output) {
  Student_handle_t handle;
  tree_t KEY_index;
  Student_t student;

  hashget(&handle, &(input->db_handle));
  KEY_index.root = handle.KEY_index;
  student = input->student;

  int i;
  uint32_t path_depth, encoded_tree_path;

  KEY_t tempKEY = student.KEY;
  FName_t tempFName = student.FName;
  LName_t tempLName = student.LName;
  Age_t tempAge = student.Age;
  Major_t tempMajor = student.Major;
  State_t tempState = student.State;
  PhoneNum_t tempPhoneNum = student.PhoneNum;
  Class_t tempClass = student.Class;
  Credits_t tempCredits = student.Credits;
  Average_t tempAverage = student.Average;
  Honored_t tempHonored = student.Honored;
  Address_t tempAddress = student.Address;

  CQL("INSERT INTO Student (KEY, FName, LName, Age, Major, State, PhoneNum, Class, Credits, Average, Honored, Address) VALUES (tempKEY, tempFName, tempLName, tempAge, tempMajor, tempState, tempPhoneNum, tempClass, tempCredits, tempAverage, tempHonored, tempAddress)", path_depth, encoded_tree_path);

  handle.KEY_index = KEY_index.root;
  hashput(&(output->db_handle), &handle);

  output->path_depth = path_depth;
  output->tree_path = encoded_tree_path;
  return 0;
}

