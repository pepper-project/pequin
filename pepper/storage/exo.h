#ifndef EXO_H_
#define EXO_H_
/*
 * Implement ramput/ramget, hashput/hashget, in native c outside
 * zaatar framework. Useful when writing exogenous checks.
 *
 */
#include <stdio.h>
#include <stdlib.h>

#include <include/db.h>
#include <storage/merkle_ram.h>
#include <storage/hasher.h>
#include <storage/hash_block_store.h>

#define ramput(addr, p_data) __ramput(addr, p_data, sizeof(*p_data))
#define ramget(p_data, addr) __ramget(p_data, addr, sizeof(*p_data))
#define hashput(hash, p_data) __hashput(hash, p_data, sizeof(*p_data))
#define hashget(p_data, hash) __hashget(p_data, hash, sizeof(*p_data))
#define commitmentput(hash, p_data) __commitmentput(hash, p_data, sizeof(*p_data))
#define commitmentget(p_data, hash) __commitmentget(p_data, hash, sizeof(*p_data))
#define ramput2(ram, addr, p_data) __ramput(ram, addr, p_data, sizeof(*p_data))
#define ramget2(ram, p_data, addr) __ramget(ram, p_data, addr, sizeof(*p_data))
#define hashput2(bs, hash, p_data) __hashput(bs, hash, p_data, sizeof(*p_data))
#define hashget2(bs, p_data, hash) __hashget(bs, p_data, hash, sizeof(*p_data))
#define commitmentput2(bs, hash, p_data) __commitmentput(bs, hash, p_data, sizeof(*p_data))
#define commitmentget2(bs, p_data, hash) __commitmentget(bs, p_data, hash, sizeof(*p_data))

/*#define _strcpy(dst, src) \
    { \
        int tempI; \
        char tempCharArr[] = src; \
        for (tempI = 0; tempI < sizeof(src); tempI++) { \
            dst[tempI] = src[tempI]; \
        } \
    }
*/

//int hasheq(hash_t* a, hash_t* b);
void initBlockStore();
HashType* getRootHash();
void setBlockStoreAndRAM(HashBlockStore* bs, MerkleRAM* ram);
void deleteBlockStoreAndRAM();

void __ramput(uint32_t addr, void* data, uint32_t size);
void __ramget(void* var, uint32_t addr, uint32_t size);
void __hashput(hash_t* hash, void* data, uint32_t size);
void __hashget(void* var, hash_t* hash, uint32_t size);
void __commitmentput(commitment_t* hash, void* data, uint32_t size);
void __commitmentget(void* var, commitment_t* hash, uint32_t size);
void hashfree(hash_t* hash);
// TODO: reconcile the two methods below with the two above, later
void __hashbits(hash_t *hash, void *data, uint32_t size);
void __ramput(MerkleRAM* ram, uint32_t addr, void* data, uint32_t size);
void __ramget(MerkleRAM* ram, void* var, uint32_t addr, uint32_t size);
void __hashput(HashBlockStore *bs, hash_t* hash, void* data, uint32_t size);
void __hashget(HashBlockStore *bs, void* var, hash_t* hash, uint32_t size);
void __commitmentput(HashBlockStore *bs, commitment_t* hash, void* data, uint32_t size);
void __commitmentget(HashBlockStore *bs, void* var, commitment_t* hash, uint32_t size);
void hashfree(HashBlockStore *bs, hash_t* hash);

#endif // EXO_H_
