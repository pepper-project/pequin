/*
  With this computation, we show that pantry can be used to allow
a toll collector (the verifier) to collect tolls from a customer (the
prover) without the customer exposing their driving history.

In particular, we implement a system where a toll collector wants to
charge the customer every time he passes one of various toll booths,
each with possibly different tolls that may vary on time of day.

At the start of each month, the toll collector broadcasts a signed set
of parameters determining the toll prices for the upcoming month.
This way, the customers are aware of the costs
they will be paying that month if they choose to drive on toll roads.

During the month, the insurance company randomly
"spot checks" the customer at toll booths. At each spot check, a
server stores the tuple
   (time, toll_booth_id, toll)
where toll is the cost of driving past that toll booth.

At the end of the month,
the customer commits to a database containing tuples
   (time, toll_booth_id, toll)

The verifier then performs the following computation on the prover
  Input: H, A commitment to a database
         spotchecks, a list of spotchecks

  Unpack the commitment H to a database

  for each spotcheck s
    if no tuple in the database is a close match to s
      return REJECT
 
  COST = 0
  for each tuple t_i in the database
    COST += t_i.toll
  return COST

In order to maximize privacy, spot checks should be infrequent
(for example, only 1 in 100 tolls are observed). 
The fewer spot checks are performed,
however, the less likely customers are to be detected if they lie
to the insurance company. This problem can be offset by adding 
high penalties to being caught and by adding "tamper detection" features
to the black box given to customers.

*/
#include <stdint.h>
#include <db.h>

//Represents an amount of money in cents
typedef uint32_t cost_t;
typedef uint16_t toll_booth_id_t;

struct tuple {
  int32_t time;
  toll_booth_id_t toll_booth_id;
  cost_t toll;
};

typedef struct tuple tuple_t;

//Testing purposes
//We assume no car drives through more than 50 toll booths in a month
const int MAX_TUPLES = 512;

//At the end of the month, the client should fabricate tolls to toll
//booth id 0 with cost 0.

//There should be at least one spot check in a month, and the last
//spotcheck should be duplicated to fill up the spotchecks list.
const int MAX_SPOTCHECKS = 5;

typedef struct VerifierSideIn {
  tuple_t spotchecks[MAX_SPOTCHECKS];
  //If a tuple exists with correct toll booth id and time differing
  //by at most time_threshold, then accept.
  int32_t time_threshold;
} VerifierSideIn;

struct In {
  //hash_t commitment;
  commitmentCK_t commitmentCK;
  commitment_t commitment;
  VerifierSideIn verifier_in;
};

struct Out {
  int rejected;
  cost_t cost;
};

struct pathdb {
  tuple_t path [MAX_TUPLES];
};
