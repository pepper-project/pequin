#ifndef _INCLUDE_DB_H
#define _INCLUDE_DB_H

#pragma pack(push)
#pragma pack(1)

//Share code with the compiler internal version of this file
#include "../../compiler/cstdinc/db_shared.h"

extern hash_t* NULL_HASH;

int hasheq(hash_t* a, hash_t* b);

void setcommitmentCK(commitmentCK_t* ck);

void commitment_hash(uint8_t* message, int num_bits_message, uint8_t* key, commitment_t* hash);

#pragma pack(pop)

#endif /* include/db.h */
