#include <stdint.h>
//The number of rounds of binary search to perform
//equivalently, the number of bits in the representation of SIZE
//So, for 8 - 15, logSize is 4, for 16 - 31, logSize is 5, etc.
#define logSIZE 5
#define SIZE 16

struct In {uint32_t search;};
struct Out {uint32_t output[20];};

/*
  Search a sorted array for a value in logarithmic time using getdb /
  logdb. Whether or not the value is found, an index is returned where
  the value could be inserted into the list so that the list would still be
  sorted.

  For now, the getdb and ramput functions assume a single database.
  Eventually, they will take a compile time resolvable handle to
  a database.
*/
void compute(struct In *input, struct Out *output){
  uint32_t i, left, right, mid;
  uint32_t lower_bound, upper_bound;
  uint32_t value, index;
  uint32_t temp;

  /*temp = 11; ramput(0, &temp);*/
  /*temp = 10; ramput(1, &temp);*/
  /*temp = 0; ramput(2, &temp);*/
  /*temp = 10; ramput(3, &temp);*/
  /*temp = 5; ramput(4, &temp);*/
  /*temp = 3; ramput(5, &temp);*/
  /*temp = 2; ramput(6, &temp);*/
  /*temp = 2; ramput(7, &temp);*/
  /*temp = 6; ramput(8, &temp);*/
  /*temp = 9; ramput(9, &temp);*/
  /*temp = 13; ramput(10, &temp);*/
  /*temp = 5; ramput(11, &temp);*/
  /*temp = 3; ramput(12, &temp);*/
  /*temp = 7; ramput(13, &temp);*/
  /*temp = 2; ramput(14, &temp);*/
  /*temp = 14; ramput(15, &temp);*/
  /*temp = 3; ramput(16, &temp);*/
  /*temp = 8; ramput(17, &temp);*/
  /*temp = 0; ramput(18, &temp);*/
  /*temp = 3; ramput(19, &temp);*/
  /*temp = 4; ramput(20, &temp);*/
  /*temp = 3; ramput(21, &temp);*/
  /*temp = 14; ramput(22, &temp);*/
  /*temp = 13; ramput(23, &temp);*/
  /*temp = 7; ramput(24, &temp);*/
  /*temp = 12; ramput(25, &temp);*/
  /*temp = 14; ramput(26, &temp);*/
  /*temp = 11; ramput(27, &temp);*/
  /*temp = 5; ramput(28, &temp);*/
  /*temp = 8; ramput(29, &temp);*/
  /*temp = 15; ramput(30, &temp);*/
  /*temp = 5; ramput(31, &temp);*/
  /*temp = 0; ramput(32, &temp);*/
  /*temp = 2; ramput(33, &temp);*/
  /*temp = 2; ramput(34, &temp);*/
  /*temp = 2; ramput(35, &temp);*/
  /*temp = 3; ramput(36, &temp);*/
  /*temp = 3; ramput(37, &temp);*/
  /*temp = 5; ramput(38, &temp);*/
  /*temp = 5; ramput(39, &temp);*/
  /*temp = 6; ramput(40, &temp);*/
  /*temp = 7; ramput(41, &temp);*/
  /*temp = 9; ramput(42, &temp);*/
  /*temp = 10; ramput(43, &temp);*/
  /*temp = 10; ramput(44, &temp);*/
  /*temp = 11; ramput(45, &temp);*/
  /*temp = 13; ramput(46, &temp);*/
  /*temp = 14; ramput(47, &temp);*/
  /*temp = 2; ramput(48, &temp);*/
  /*temp = 6; ramput(49, &temp);*/
  /*temp = 7; ramput(50, &temp);*/
  /*temp = 14; ramput(51, &temp);*/
  /*temp = 5; ramput(52, &temp);*/
  /*temp = 12; ramput(53, &temp);*/
  /*temp = 4; ramput(54, &temp);*/
  /*temp = 11; ramput(55, &temp);*/
  /*temp = 8; ramput(56, &temp);*/
  /*temp = 13; ramput(57, &temp);*/
  /*temp = 9; ramput(58, &temp);*/
  /*temp = 1; ramput(59, &temp);*/
  /*temp = 3; ramput(60, &temp);*/
  /*temp = 0; ramput(61, &temp);*/
  /*temp = 10; ramput(62, &temp);*/
  /*temp = 15; ramput(63, &temp);*/
  /*temp = 0; ramput(64, &temp);*/
  /*temp = 3; ramput(65, &temp);*/
  /*temp = 3; ramput(66, &temp);*/
  /*temp = 3; ramput(67, &temp);*/
  /*temp = 4; ramput(68, &temp);*/
  /*temp = 5; ramput(69, &temp);*/
  /*temp = 5; ramput(70, &temp);*/
  /*temp = 7; ramput(71, &temp);*/
  /*temp = 8; ramput(72, &temp);*/
  /*temp = 8; ramput(73, &temp);*/
  /*temp = 11; ramput(74, &temp);*/
  /*temp = 12; ramput(75, &temp);*/
  /*temp = 13; ramput(76, &temp);*/
  /*temp = 14; ramput(77, &temp);*/
  /*temp = 14; ramput(78, &temp);*/
  /*temp = 15; ramput(79, &temp);*/
  /*temp = 2; ramput(80, &temp);*/
  /*temp = 0; ramput(81, &temp);*/
  /*temp = 3; ramput(82, &temp);*/
  /*temp = 5; ramput(83, &temp);*/
  /*temp = 4; ramput(84, &temp);*/
  /*temp = 12; ramput(85, &temp);*/
  /*temp = 15; ramput(86, &temp);*/
  /*temp = 8; ramput(87, &temp);*/
  /*temp = 1; ramput(88, &temp);*/
  /*temp = 13; ramput(89, &temp);*/
  /*temp = 11; ramput(90, &temp);*/
  /*temp = 9; ramput(91, &temp);*/
  /*temp = 7; ramput(92, &temp);*/
  /*temp = 6; ramput(93, &temp);*/
  /*temp = 10; ramput(94, &temp);*/
  /*temp = 14; ramput(95, &temp);*/
  /*CQL("SELECT name,age FROM S WHERE age < 8", 16, output->output);*/

  lower_bound = 64;
  left = 64;
  right = 79;
  for(i = 0; i < logSIZE; i++) {
    mid = (left + right) >> 1;
    ramget(&value, mid);
    if (value < 8) {
      left = mid + 1;
    } else {
      right = mid - 1;
    }
  }
  upper_bound = right;
  for (i = 0; i < 3; i++) {
    if (i + lower_bound < upper_bound) {
      ramget(&index, i + lower_bound + 16);
      ramget(&(output->output[i * 2 + 0]), index + 0);
      ramget(&(output->output[i * 2 + 1]), index + 16);
    } else {
      output->output[i * 2 + 0] = 0;
      output->output[i * 2 + 1] = 0;
    }
  }
}
