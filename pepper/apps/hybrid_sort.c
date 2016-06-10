#include <stdint.h>
#include <list.h>

//Size of array to sort
#define SIZE 2
//Sorting mode
#define SORT_INSERTION 0
#define SORT_RADIX 1
#define SORT_MODE SORT_RADIX

struct In {uint32_t array[SIZE];}; //Array to sort
struct Out {uint32_t array[SIZE];}; //sorted array

void compute(struct In *input, struct Out *output){
  if (SORT_MODE == SORT_INSERTION){
    insertion_sort(input, output);
  }
  if (SORT_MODE == SORT_RADIX){
    radix_sort(input, output);
  }
}

int test_bit(uint32_t a, int i){
  return (a >> i)&1;
}

void list_to_arr(uint32_t *a, hash_t list, int num){
  int i;
  for(i = 0; i < num; i++){
    list_t data;
    hashget(&data, &list); //Never a null lookup.
    a[i] = data.value;
    list = data.next;
  }
}

void radix_sort(struct In *input, struct Out *output){
  int i,j;
  uint32_t *a,*b;
  hash_t list;

  a = input->array;
  /*
  for(i = 0; i < 32; i++) {
    list = *NULL_HASH;
    //First grab all elements of a that have a 1 at bit i
    for(j = SIZE-1; j >= 0; j--){
      if (test_bit(a[j], i)){
	list = prepend(a[j], list);
      }
    }
    //Now grab all elements of a that have a 0 at bit i
    for(j = SIZE-1; j >= 0; j--){
      if (test_bit(a[j], i)){
      } else {
	//Prepend a[j] to the list.
	list = prepend(a[j], list);
      }
    }
    //Now traverse the list, writing the values into a.
    for(j = 0; j < SIZE; j++){
      list_t data;
      hashget(&data, &list); //Never a null lookup.
      a[j] = data.value;
      list = data.next;
    }
  }
  */
  for(i = 0; i < SIZE; i++){
    list = prepend(a[i], list);
  }
  list_to_arr(a, list, SIZE);

  //Write a to output.
  for(i = 0; i < SIZE; i++){
    output->array[i] = a[i];
  }
}

void insertion_sort(struct In *input, struct Out *output){
  int i,j;
  int inserted;
  uint32_t *a,*b;

  a = input->array;
  b = output->array;

  //Duplicate a
  for(i = 0; i < SIZE; i++){
    b[i] = a[i];
  }

  for(i = 0; i < SIZE; i++) {
    inserted = 0;
    //Try to insert a[i] into a[0...i-1]
    for(j = 0; j < i; j++) {
      if (b[i] < b[j]) {
	if (inserted) {
	  b[j] = a[j-1];
	} else {
	  b[j] = a[i];
	  inserted = 1;
	}
      } else {
	b[j] = a[j];
      }
    }
    if (inserted) {
      b[i] = a[i-1];
    } else {
      b[i] = a[i];
    }

    //Copy b into a
    for(j = 0; j < SIZE; j++){
      a[j] = b[j];
    }
  }
  
  //b may already be output.array, but assign it anyway.
  for(i = 0; i < SIZE; i++){
    output->array[i] = b[i];
  }
}



