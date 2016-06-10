#ifndef EXTERNAL_SORT_H_
#define EXTERNAL_SORT_H_

void dump_array(char* array_to_dump, long size_in_bytes, long offset, const char* filename, const char* folder_name);

void dump_array(char* array_to_dump, long size_in_bytes, const char* filename, const char* folder_name);

void load_array(char* array_to_fill, long size_in_bytes, long offset, const char* filename, const char* folder_name);

void load_array(char* array_to_fill, long size_in_bytes, const char* filename, const char* folder_name);

void partition_one_chunk(const char* filename_prefix, const char* folder_name, long offset, int chunk_id, int number_of_entries, long entry_size);

void partition_file_into_chunks(const char* filename, const char* folder_name, int number_of_chunks, long entry_size);

void sort_one_chunk(const char* filename, const char* folder_name, long entry_size, int (*comparator)(const void*, const void*));

void sort_chunks(const char* filename_prefix, const char* folder_name, int number_of_chunks, long entry_size, int (*comparator)(const void*, const void*));

void merge_sorted_chunks(const char* filename_prefix, const char* folder_name, int number_of_chunks, long entry_size, int (*comparator)(const void*, const void*));

void external_sort(const char* filename, const char* folder_name, int number_of_chunks, long entry_size, int (*comparator)(const void*, const void*), bool partition);

#endif /*EXTERNAL_SORT_H_ */
