#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <time.h>
#include <iostream>

#include "waksman_router.h"
using namespace std;

int numSwitches(int);
void printp(packet_t* packets, size_t n);
bool data_equal(data_t* data1, data_t* data2);

int packet_cmp(const void* a, const void* b) {
  data_t* pa = &(((packet_t*)a)->data);
  data_t* pb = &(((packet_t*)b)->data);
  if (pa->addr == pb->addr) {
    return (pa->timestamp < pb->timestamp) ? -1 : 1;
  } else {
    return (pa->addr < pb->addr) ? -1 : 1;
  }
}

int src_cmp(const void* a, const void* b) {
  packet_t* pa = ((packet_t*)a);
  packet_t* pb = ((packet_t*)b);
  return (pa->src < pb->src) ? -1 : 1;
}

void sort_packet(data_t* input, packet_t* packets, size_t n) {
  for (size_t i = 0; i < n; i++) {
    packets[i].data = input[i];
    packets[i].src = i;
  }
  qsort(packets, n, sizeof(packet_t), packet_cmp);
  for (size_t i = 0; i < n; i++) {
    packets[i].dst = i;
  }
  qsort(packets, n, sizeof(packet_t), src_cmp);
}

void route(packet_t* left_pkt, packet_t* right_pkt, data_t* intermediate, switch_t* left_sw, int n, size_t total_size) {
  //base cases, n = 2, route a single switch, n = 1, nothing to do. 
  if (n == 2) {
    assert(left_sw[0].set == false);
    if (data_equal(&left_pkt[0].data, &right_pkt[0].data)) {	
      left_sw[0].set = true;
      left_sw[0].swap = false;
    }
    else {
      assert(data_equal(&left_pkt[0].data, &right_pkt[1].data));
      left_sw[0].set = true;
      left_sw[0].swap = true;
    }     
    return;
  }
  if (n == 1) {
    assert(data_equal(&left_pkt[0].data, &right_pkt[0].data));
    return;
  }
   
  int leftSwitchCt = n / 2;
  int rightSwitchCt = leftSwitchCt - 1 + n % 2;
  int numLeftVars = leftSwitchCt * 2;
  int numRightVars = rightSwitchCt * 2;
  int topSize = floor(n/2.0);
  int bottomSize = ceil(n/2.0);
    
  //printf("(%d, %d, %d, %d), (%d, %d)\n", leftSwitchCt, topSize, bottomSize, rightSwitchCt, numLeftVars, numRightVars);  
  packet_t topLeft[topSize];
  packet_t topRight[topSize];
  packet_t botLeft[bottomSize];
  packet_t botRight[bottomSize];
  switch_t* right_sw = left_sw + leftSwitchCt;
  int nextIndex;
  int index = n - 1;
  //we always start by routing the bottom right output back to its input.

  packet_t tmp = right_pkt[index];

  int other_index = tmp.src;
  right_pkt[index].routed = true;
  left_pkt[other_index].routed = true;
 
  tmp.dst = bottomSize - 1;   
  tmp.src = other_index/2;
  //cout << "first from (right) index: " << index << endl;
  //cout << "first to (left) index: " << other_index << endl;
  if (other_index % 2 == 1) { //bottom of a switch, so route through the bottom
    left_sw[other_index/2].swap = false;
    left_sw[other_index/2].set = true;
    intermediate[other_index] = tmp.data;
    nextIndex = other_index - 1;       
  }
  else if (other_index == n - 1) { //bottom of odd size, a link, route through bottom
    nextIndex = -1;
  }
  
  else { //top of a switch
    left_sw[other_index/2].swap = true;
    left_sw[other_index/2].set = true;
    intermediate[other_index + 1] = tmp.data;
  
    nextIndex = other_index + 1;
  }
  botLeft[tmp.src] = tmp;
  botRight[tmp.dst]= tmp; 
  
  //route from left to right, then right to left, until there are no more packets left to route. 
  while(true) {
    if (nextIndex == -1 || left_pkt[nextIndex].routed) {
      for (int i = 0; i < n; i++) {
	if (left_pkt[i].routed == false) {
	  nextIndex = i;
	  break;
	}
      }
      
      if (nextIndex == -1 || left_pkt[nextIndex].routed) {
	break; //all packets routed at this level.
      }
    }
    index = nextIndex;

    //start by routing from left to right.

    tmp = left_pkt[index];
    int right_index = tmp.dst;
    //cout << "from (left) index: " << index << endl;
    //cout << "to (right) index" << right_index << endl;
    left_pkt[index].routed = true;
    right_pkt[right_index].routed = true;

    tmp.src = index/2;
    tmp.dst = right_index/2;

    bool routeBot = false;
    //left side: decide which sub to route through.
    if (index == n - 1 && n % 2 == 1) { //link in an odd network
      routeBot = true;
    } 
    else {
      if (!left_sw[index/2].set) {
	left_sw[index/2].swap = false;
	left_sw[index/2].set = true;
      }
      bool sw = left_sw[index/2].swap;

      if ( (index % 2 == 0 &&  sw) || (index % 2 == 1 && !sw) ) { //top and unswapped or bottom and swapped
	routeBot = true;
      }
      int offset = 0;
      if (index%2 == 1 && sw) 
	offset = -1;
      if (index %2 == 0 && sw)
	offset = 1;      
      intermediate[index + offset] = tmp.data;

    }

    int sw_index = right_index/2;
    if ((right_index == n - 1) || (right_index == n - 2 && n % 2 == 0) )  
      nextIndex = -1;
    else {
      bool isBot = (right_index % 2 == 1);
      if (isBot) 
	nextIndex = right_index - 1;
      else
	nextIndex = right_index + 1;

      int offset = 0;
      if (isBot ^ routeBot) { //swap
	if (!right_sw[sw_index].set) {
	  right_sw[sw_index].set = true;
	  right_sw[sw_index].swap = true;
	}
	assert(right_sw[sw_index].swap);

	if (isBot) 
	  offset = -1;
	else
	  offset = 1;
      }
      else { //noswap
	if (!right_sw[sw_index].set) {
	  right_sw[sw_index].set = true;
	  right_sw[sw_index].swap = false;
	}
	assert(right_sw[sw_index].swap == false);
      }
      intermediate[numLeftVars + right_index + offset] = tmp.data;
    }

    if (routeBot) {
      botLeft[tmp.src] = tmp;
      botRight[tmp.dst] = tmp;
    }

    else {
      topLeft[tmp.src] = tmp;
      topRight[tmp.dst] = tmp;
    }

    //now select an index on the right and route back from right to left.
    if (nextIndex == -1 || right_pkt[nextIndex].routed) {
      for (int i = 0; i < n; i++) {
	if (right_pkt[i].routed == false) {
	  nextIndex = i;
	  break;
	}
      }
      
      if (nextIndex == -1 || right_pkt[nextIndex].routed) {
	break; 
      }
    }
    //cout << "from (right) index " << nextIndex << endl;
    right_index = nextIndex;
    tmp = right_pkt[right_index];
    int left_index = tmp.src;
    //cout << "to (left) index: " << left_index << endl;
    
    left_pkt[left_index].routed = true;
    right_pkt[right_index].routed = true;

    tmp.src = left_index/2;
    tmp.dst = right_index/2;
    //right side: decide which sub  to route through.
    routeBot = false;
    if (right_index == n - 2 && n % 2 == 0) {//second to last in even, a link. NOTE: last is already done.
    }

    else { //a switch...
      int rsw_index = right_index/2;      
      if (!right_sw[rsw_index].set) {
	right_sw[rsw_index].swap = false;
	right_sw[rsw_index].set = true;
      }
      bool sw = right_sw[rsw_index].swap;

      if ( (right_index %2 == 0 && sw) || (right_index %2 == 1 && !sw) ) { //top and swapped or bottom and unswapped.
	routeBot = true;
      } 
      //cout << "routeBot: " << ((routeBot) ? "true" : "false")<< endl; 
      int offset = 0;
      if (right_index%2 == 1 && sw)
	offset = -1;
      if (right_index %2 == 0 && sw)
	offset = 1;
      int is3 = 0;
      if (n == 3)
	is3 = 1;
      intermediate[numLeftVars + right_index + offset - is3] = tmp.data;
    }

    int lsw_index = left_index/2;
    if (left_index == n - 1 && n % 2 == 1)
      nextIndex = -1;
    else {
      bool isBot = (left_index%2 == 1);
      if (isBot)
	nextIndex = left_index - 1;
      else
	nextIndex = left_index + 1;
      
      int offset = 0;
      if (isBot ^ routeBot) { //swap
	if (!left_sw[lsw_index].set) {
	  left_sw[lsw_index].set = true;
	  left_sw[lsw_index].swap = true;
	}
	assert(left_sw[lsw_index].swap);

	if (isBot)
	  offset = -1;
	else
	  offset = 1;
      }
      else { //noswap
	if (!left_sw[lsw_index].set) {
	  left_sw[lsw_index].set = true;
	  left_sw[lsw_index].swap = false;
	}
	assert(left_sw[lsw_index].swap == false);
      }
      intermediate[left_index + offset] = tmp.data;
    }

    if (routeBot) {
      botLeft[tmp.src] = tmp;
      botRight[tmp.dst] = tmp;
    }

    else {
      topLeft[tmp.src] = tmp;
      topRight[tmp.dst] = tmp;
    }
    

  }

  //prepare for the next recursive call to route the top and bottom subnetworks.
  data_t* topVars = intermediate + numLeftVars + numRightVars;
  switch_t* topSwitches = left_sw + leftSwitchCt + rightSwitchCt;
  int topSwc = numSwitches(topSize);
  int topVarc = 2 * topSwc - topSize;
  data_t* bottomVars = topVars + topVarc;
  switch_t* bottomSwitches = topSwitches + topSwc;

  //cout << "TOP LEFT" << endl;
  // printp(topLeft, topSize);
  // cout << "TOP RIGHT" << endl;
  // printp(topRight, topSize);
  //cout << "BOTTOM LEFT" << endl;
  //printp(botLeft, bottomSize);
  //cout << "BOTTOM RIGHT" << endl;
  // printp(botRight, bottomSize);
  route(topLeft, topRight, topVars, topSwitches, topSize, total_size);
  route(botLeft, botRight, bottomVars, bottomSwitches, bottomSize, total_size);
}

int numSwitches(int n) {
  int num_switches = 0;
  for (int i = 1; i <= n; i++) {
    num_switches += ceil(log2(i));
  }
  return num_switches;

}
bool data_equal(data_t* data1, data_t* data2) {
  return data1->addr == data2->addr && data1->timestamp == data2->timestamp && data1->type == data2->type && data1->value == data2->value;
}

void printp(packet_t* packets, size_t n) {
  for (size_t i = 0; i < n; i++) {
    printf("data: %d,%d\tsrc: %ld\tdst: %ld, routed: %d\n", packets[i].data.addr, packets[i].data.timestamp, packets[i].src, packets[i].dst, packets[i].routed);
  }
}

void wak_route(data_t* input, data_t* intermediate, data_t* output, switch_t* switches, size_t width, size_t num_switches) {
  for (size_t i = 0; i < num_switches; i++) {
    switches[i].set = false;
  }
 if (width < 2) {
    printf("Benes network has to be at least of width 2\n");
    return;
  }
  packet_t* leftPackets = new packet_t[width];
  packet_t* rightPackets = new packet_t[width];
  for (size_t i = 0; i < width; i++) {
    leftPackets[i].data = input[i];
    leftPackets[i].routed = false;
  }

  sort_packet(input, leftPackets, width);

  // for (size_t i = 0; i < width; i++) {
  //  printf("data: %d,%d\tsrc: %ld\tdst: %ld\n", leftPackets[i].data.addr, leftPackets[i].data.timestamp, leftPackets[i].src, leftPackets[i].dst);
  // }

  for (size_t i = 0; i < width; i++) {
    leftPackets[i].routed = false;
    rightPackets[leftPackets[i].dst] = leftPackets[i];
  }

  for (size_t i = 0; i < width; i++) {
    output[i] = rightPackets[i].data;
  }

  //perform the actual routing, filling in the switches and intermediate nodes. 
  route(leftPackets, rightPackets, intermediate, switches, width, width);
    
  delete[] leftPackets;
  delete[] rightPackets;

}

int test_benes_network() {

#define WIDTH 5

  data_t input[WIDTH];
  size_t num_switches = 0;
  for (int i = 1; i <= WIDTH; i++) {
    num_switches += ceil(log2(i));
   }
  size_t num_intermediate = 2 * num_switches - WIDTH;
  data_t intermediate[num_intermediate];
  data_t output[WIDTH];
  switch_t switches[num_switches];
  srand(time(NULL));
  for (int rep = 0; rep < 1; rep++) {
  for (size_t i = 0; i < WIDTH; i++) {
    input[i].addr = rand() % 100;
    input[i].timestamp = rand() % 100;
  }
  cout << "intermediate" << endl;
for (size_t j = 0; j < num_intermediate; j++) {
  intermediate[j].addr = 0;
  intermediate[j].timestamp = 0;
  }
  
  wak_route(input, intermediate, output, switches, WIDTH, num_switches);

  cout << "INPUTS" << endl;
  for (size_t i = 0; i < WIDTH; i++) {
    printf("%d,%d\n", input[i].addr, input[i].timestamp);
  }
  cout << "INTERMEDIATE" << endl;

  for (size_t j = 0; j < num_intermediate; j++) {
      printf("%d,%d\n", intermediate[j].addr, intermediate[j].timestamp);
  }
  
  cout << "OUTPUTS" << endl;
  for (size_t i = 0; i < WIDTH; i++) {
    printf("%d,%d\n", output[i].addr, output[i].timestamp);
  }

  for (size_t i = 0; i < num_switches; i++) {
    if (switches[i].swap) {
      printf("sw %d swap", i);
      } else {
      printf("sw %d nosw", i);
      }
    printf("\n");
  }

  } 
  return 0;
}


//int main() {
// test_benes_network();

//}
