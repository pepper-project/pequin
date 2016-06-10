#ifndef _STUDENT_DB_H
#define _STUDENT_DB_H

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
typedef uint32_t Honored_t;
typedef struct Address {
  uint64_t address[5];
} Address_t;

typedef struct Student_handle {
  hash_t KEY_index;
  hash_t Major_index;
  hash_t LName_index;
  hash_t State_index;
  hash_t Age_index;
  hash_t Class_index;
  hash_t FName_index;
  hash_t Credits_index;
  hash_t Average_index;
  hash_t PhoneNum_index;
  hash_t Address_index;
} Student_handle_t;

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
  Honored_t Honored;
  Address_t Address;
} Student_t;

typedef struct Student_result {
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
  Honored_t Honored;
} Student_result_t;

#endif
