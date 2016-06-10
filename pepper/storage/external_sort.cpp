#include <common/utility.h>
#include <storage/external_sort.h>
#include <iostream>

#define BUFSIZE 8192

void dump_array(char* array_to_dump, long size_in_bytes, long offset, const char* filename, const char* folder_name) {
  //cout << "dumping " << size_in_bytes << " bytes into " << filename << " at offset " << offset << endl;
  fstream fp;
  open_file_update(fp, filename, folder_name);
  fp.seekp(offset);
  if (!fp.write(array_to_dump, size_in_bytes)) {
    cout << "Error writing to " << filename << endl;
    fp.close();
    exit(1);
  }
  //cout << "bytes actually dumped " << fp.tellp() << endl;
  fp.close();
}

void dump_array(char* array_to_dump, long size_in_bytes, const char* filename, const char* folder_name) {
  //cout << "dumping " << size_in_bytes << " bytes into " << filename << endl;
  ofstream fp;
  open_file_write(fp, filename, folder_name);
  if (!fp.write(array_to_dump, size_in_bytes)) {
    cout << "Error writing to " << filename << endl;
    fp.close();
    exit(1);
  }
  //cout << "bytes actually dumped " << fp.tellp() << endl;
  fp.close();
}

void load_array(char* array_to_fill, long size_in_bytes, long offset, const char* filename, const char* folder_name) {
  ifstream fp;
  open_file_read(fp, filename, folder_name);
  //cout << "loading " << size_in_bytes << " bytes from " << filename << " at offset " << offset << endl;
  fp.seekg(offset);
  if (!fp.read(array_to_fill, size_in_bytes)) {
    cout << "Error reading " << size_in_bytes << " bytes from " << filename << " at offest " << offset << endl;
    fp.close();
    exit(1);
  }
  //cout << "bytes actually loaded " << fp.gcount() << endl;
  fp.close();
}

void load_array(char* array_to_fill, long size_in_bytes, const char* filename, const char* folder_name) {
  load_array(array_to_fill, size_in_bytes, 0, filename, folder_name);
}

void quicksort_key_value_pairs(char* buffer, int left, int right, long entry_size, int (*comparator)(const void*, const void*)) {
  if (left >= right)
    return;
  int i = left;
  int j = right;

  char* tmp_entry = new char[entry_size];
  char* pivot = new char[entry_size];
  memcpy(pivot, buffer + entry_size * ((left + right) / 2), entry_size);

  while (i <= j) {
    while (comparator(buffer + i * entry_size, pivot) < 0) i++;
    while (comparator(buffer + j * entry_size, pivot) > 0) j--;
    if (i <= j) {
      memcpy(tmp_entry, buffer + i * entry_size, entry_size);
      memcpy(buffer + i * entry_size, buffer + j * entry_size, entry_size);
      memcpy(buffer + j * entry_size, tmp_entry, entry_size);
      i++;
      j--;
    }
  }
  delete[] tmp_entry;
  delete[] pivot;

  quicksort_key_value_pairs(buffer, left, i - 1, entry_size, comparator);
  quicksort_key_value_pairs(buffer, i, right, entry_size, comparator);
}

void quicksort_key_value_pairs(char* buffer, int number_of_entries, long entry_size, int (*comparator)(const void*, const void*)) {
  quicksort_key_value_pairs(buffer, 0, number_of_entries - 1, entry_size, comparator);
}

void partition_file_into_chunks(const char* filename, const char* folder_name, int number_of_chunks, long entry_size) {
  // get file size, devide by number of chunks.
  char full_filename[BUFLEN];
  snprintf(full_filename, BUFLEN - 1, "%s/%s", folder_name, filename);
  off_t file_size = get_file_size(full_filename);
  long number_of_entries = file_size / entry_size;
  long entries_per_chunk = (number_of_entries + number_of_chunks - 1) / number_of_chunks;
  int j = 0;
  for (long i = 0; i < number_of_entries; i+= entries_per_chunk, j++) {
    long actual_entries_per_chunk = entries_per_chunk;
    if (number_of_entries - i < entries_per_chunk) {
      actual_entries_per_chunk = number_of_entries - i;
    }
    partition_one_chunk(filename, folder_name, i, j, actual_entries_per_chunk, entry_size);
  }
}

void partition_one_chunk(const char* filename_prefix, const char* folder_name, long offset, int chunk_id, int number_of_entries, long entry_size) {
  char filename[BUFLEN];
  char* buffer = new char[number_of_entries * entry_size];
  load_array(buffer, number_of_entries * entry_size, offset * entry_size, filename_prefix, folder_name);
  snprintf(filename, BUFLEN - 1, "%s_%d", filename_prefix, chunk_id);
  dump_array(buffer, number_of_entries * entry_size, filename, folder_name);
  delete[] buffer;
}

void sort_chunks(const char* filename_prefix, const char* folder_name, int number_of_chunks, long entry_size, int (*comparator)(const void*, const void*)) {

  int finished_chunks = 0;
#pragma omp parallel
  {
#pragma omp for
    for (int i = 0; i < number_of_chunks; i++){
      char filename[BUFLEN];
      snprintf(filename, BUFLEN - 1, "%s_%d", filename_prefix, i);
      sort_one_chunk(filename, folder_name, entry_size, comparator);
#pragma omp critical
      {
        finished_chunks++;
        printf("sorting %s: finished chunks = %d progress = %0.1f%%\n", filename_prefix, finished_chunks, 100.0 * finished_chunks/number_of_chunks);
      }
    }
  }
}

void sort_one_chunk(const char* filename, const char* folder_name, long entry_size, int (*comparator)(const void*, const void*)) {
  char full_filename[BUFLEN];
  snprintf(full_filename, BUFLEN - 1, "%s/%s", folder_name, filename);
  off_t file_size = get_file_size(full_filename);
  long number_of_entries = file_size / entry_size;
  //printf("chunk size: %ld entry size: %ld number of entries: %ld\n", file_size, entry_size, number_of_entries);

  char* buffer_to_sort = new char[number_of_entries * entry_size];
  // load each chunk into memory, sort it and write it back as one sorted chunk.
  load_array(buffer_to_sort, number_of_entries * entry_size, filename, folder_name);
  quicksort_key_value_pairs(buffer_to_sort, number_of_entries, entry_size, comparator);
  dump_array(buffer_to_sort, number_of_entries * entry_size, filename, folder_name);
  delete[] buffer_to_sort;
}

void merge_sorted_chunks(const char* filename_prefix, const char* folder_name, int number_of_chunks, long entry_size, int (*comparator)(const void*, const void*)) {
  typedef struct chunk {
    ifstream fp;
    char* buffer;
    char* current_pointer;
    char* buffer_tail;
  } chunk_t;

  ofstream sorted_fp;
  open_file_write(sorted_fp, filename_prefix, folder_name);

  chunk_t* chunks = new chunk_t[number_of_chunks];
  char* sorted_buf = new char[BUFSIZE * entry_size];

  for (int i = 0; i < number_of_chunks; i++) {
    char filename[BUFLEN];
    snprintf(filename, BUFLEN - 1, "%s_%d", filename_prefix, i);
    open_file_read(chunks[i].fp, filename, folder_name);

    chunks[i].buffer = new char[BUFSIZE * entry_size];
    chunks[i].fp.read(chunks[i].buffer, BUFSIZE * entry_size);
    chunks[i].buffer_tail = chunks[i].buffer + chunks[i].fp.gcount();
    if (chunks[i].buffer_tail <= chunks[i].buffer) {
      chunks[i].current_pointer == NULL;
    } else {
      chunks[i].current_pointer = chunks[i].buffer;
    }
  }

  char* smallest_entry;
  int smallest_kv_pair_index = -1;
  int buf_index = 0;
  while (true) {
    smallest_entry = NULL;
    smallest_kv_pair_index = -1;
    // TODO improve with losers tree.
    // find the smallest among all chunks and this will be the next element to write
    for (int i = 0; i < number_of_chunks; i++) {
      if (chunks[i].current_pointer != NULL) {
        if (smallest_kv_pair_index == -1 || comparator(chunks[i].current_pointer, smallest_entry) < 0) {
          smallest_entry = chunks[i].current_pointer;
          smallest_kv_pair_index = i;
        }
      }
    }
    // This means all chunks are over.
    if (smallest_kv_pair_index == -1) {
      break;
    }
    // write result into output buffer.
    memcpy(sorted_buf + buf_index * entry_size, smallest_entry, entry_size);
    buf_index++;
    chunks[smallest_kv_pair_index].current_pointer += entry_size;

    // the output buffer is full and dump the buffer to disk.
    if (buf_index >= BUFSIZE) {
      sorted_fp.write(sorted_buf, BUFSIZE * entry_size);
      buf_index = 0;
    }

    // prepare the input buffer.
    if (chunks[smallest_kv_pair_index].current_pointer >= chunks[smallest_kv_pair_index].buffer_tail) {
      // the current buffer is used up.
      chunks[smallest_kv_pair_index].fp.read(chunks[smallest_kv_pair_index].buffer, BUFSIZE * entry_size);
      chunks[smallest_kv_pair_index].buffer_tail = chunks[smallest_kv_pair_index].buffer + chunks[smallest_kv_pair_index].fp.gcount();
      // check if this chunk has ended
      if (chunks[smallest_kv_pair_index].buffer_tail <= chunks[smallest_kv_pair_index].buffer) {
        chunks[smallest_kv_pair_index].current_pointer = NULL;
      } else {
        chunks[smallest_kv_pair_index].current_pointer = chunks[smallest_kv_pair_index].buffer;
      }
    }
  }
  // clean up
  for (int i = 0; i < number_of_chunks; i++) {
    chunks[i].fp.close();
    delete[] chunks[i].buffer;
  }
  sorted_fp.write((char*)sorted_buf, buf_index * entry_size);
  sorted_fp.close();

  delete[] chunks;
  delete[] sorted_buf;
}

void external_sort(const char* filename, const char* folder_name, int number_of_chunks, long entry_size, int (*comparator)(const void*, const void*), bool partition) {
  if (partition) {
    partition_file_into_chunks(filename, folder_name, number_of_chunks, entry_size);
  }
  sort_chunks(filename, folder_name, number_of_chunks, entry_size, comparator);
  merge_sorted_chunks(filename, folder_name, number_of_chunks, entry_size, comparator);
}
