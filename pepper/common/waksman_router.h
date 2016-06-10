#include <stdint.h>

typedef struct data {
  uint32_t addr;
  uint32_t timestamp;
  uint32_t type;
  int64_t value;
} data_t;

typedef struct packet {
  data_t data;
  size_t src;
  size_t dst;
  bool routed;
} packet_t;

typedef struct routing_switch {
  bool swap;
  bool set;
} switch_t;

void wak_route(data_t* input, data_t* intermediate, data_t* output, switch_t* switches, size_t width, size_t num_switches);
