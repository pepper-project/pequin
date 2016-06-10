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
#include "tolling.h"

cost_t compute_cost(struct pathdb* db){
  cost_t toRet = 0;

  int i;
  for(i = 0; i < MAX_TUPLES; i++){
    tuple_t* here = &(db->path[i]);

    toRet += here->toll;
  }

  return toRet;
}

int close_match(tuple_t* s, tuple_t* v, int32_t threshold){
  int toRet = 0;
  int64_t time_diff = ((int64_t)s->time) - v->time;
  if (time_diff < threshold &&
    time_diff > -threshold){
    if (s->toll_booth_id == v->toll_booth_id){
      toRet = 1;
    }
  }
  return toRet;
}

int verified_tolling(struct pathdb* db, VerifierSideIn* input, struct Out* output){
  output->cost = compute_cost(db);

  //Enforcement - make sure all spotchecks are in there
  int failed_spotchecks = 0;

  int i;

  for(i = 0; i < MAX_SPOTCHECKS; i++){
    //Find a match
    int found = 0;

    int j;
    //THIS IS SLOW. TODO: permutation network implementation.
    tuple_t* s = &(input->spotchecks[i]);
    for(j = 0; j < MAX_TUPLES; j++){
      tuple_t* v = &(db->path[j]);
      if (close_match(s,v,input->time_threshold)){
        found = 1;
      }
    }

    if (!found){
      failed_spotchecks = 1;
    }
  }

  output->rejected = failed_spotchecks;

  if (failed_spotchecks){
    output->cost = 0; //Prevent fishing attack
  }
  return 0;
}

int compute(struct In* input, struct Out* output){
  //Unpack the database
  struct pathdb db;
  //TODO change to decommit
  //hashget(&db, &input->commitment);
  setcommitmentCK(&input->commitmentCK);
  hash_t hash_of_db;
  commitmentget(&hash_of_db, &input->commitment);
  hashget(&db, &hash_of_db);

  return verified_tolling(&db, &input->verifier_in, output);
}
