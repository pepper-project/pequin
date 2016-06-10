#ifndef _LIST_H
#define _LIST_H   1
#include <stdint.h>
#include <db.h>

typedef uint32_t list_val_t;
typedef struct list_t {list_val_t value; hash_t next;} list_t;

/*
 Returns a hash to a list whose first node has value value, and whose
 next is the passed-in hash.
*/
hash_t prepend(list_val_t value, hash_t hash_of_list){
  list_t nlist;
  hash_t toRet;
  nlist.value = value;
  nlist.next = hash_of_list;
  hashput(&toRet, &nlist);

  return toRet;
}

#endif /* list.h */
