#include <common/measurement.h>

Measurement::Measurement() {
  reset();
  rclock_id = CLOCK_PROCESS_CPUTIME_ID;
}

Measurement& Measurement::
operator+=(const Measurement& other) {
  ru_time_sofar += other.ru_time_sofar;
  papi_time_sofar += other.papi_time_sofar;
  gtd_time_sofar += other.gtd_time_sofar;
  rclock_time_sofar += other.rclock_time_sofar;
  return *this;
}

const Measurement Measurement::
operator+(const Measurement& other) const {
  Measurement out = *this;
  out += other;
  return out;
}

Measurement& Measurement::
operator-=(const Measurement& other) {
  ru_time_sofar -= other.ru_time_sofar;
  papi_time_sofar -= other.papi_time_sofar;
  gtd_time_sofar -= other.gtd_time_sofar;
  rclock_time_sofar -= other.rclock_time_sofar;
  return *this;
}

const Measurement Measurement::
operator-(const Measurement& other) const {
  Measurement out = *this;
  out -= other;
  return out;
}

void Measurement::reset() {
  ru_time_sofar = 0;
  papi_time_sofar = 0;
  gtd_time_sofar = 0;
  rclock_time_sofar = 0;
}

void Measurement::clear() {
  reset();
}

void Measurement::begin_with_init() {
  reset();
  begin_with_history();
}

void Measurement::begin_with_history() {
  getrusage(RUSAGE_SELF, &ru_start);
  papi_start = PAPI_get_real_nsec();
  gettimeofday(&gtd_start, NULL);
  clock_gettime(rclock_id, &rclock_start);
}

void Measurement::end() {
  clock_gettime(rclock_id, &rclock_end);
  papi_end = PAPI_get_real_nsec();
  getrusage(RUSAGE_SELF, &ru_end);
  gettimeofday(&gtd_end, NULL);

  papi_time_sofar += (papi_end - papi_start);

  struct timeval start_time = ru_start.ru_utime;
  struct timeval end_time = ru_end.ru_utime;

  double ts = start_time.tv_sec*1000000 + (start_time.tv_usec);
  double te = end_time.tv_sec*1000000  + (end_time.tv_usec);
  ru_time_sofar += (te-ts);

  // add system time
  start_time = ru_start.ru_stime;
  end_time = ru_end.ru_stime;

  ts = start_time.tv_sec*1000000 + (start_time.tv_usec);
  te = end_time.tv_sec*1000000  + (end_time.tv_usec);
  ru_time_sofar += (te-ts);

  gtd_time_sofar += (gtd_end.tv_sec - gtd_start.tv_sec)*1000000LL +
                    gtd_end.tv_usec - gtd_start.tv_usec;

  ts = (double)rclock_start.tv_sec/1000000.0 + (double)rclock_start.tv_nsec / 1000.0;
  te = (double)rclock_end.tv_sec/1000000.0 + (double)rclock_end.tv_nsec / 1000.0;
  rclock_time_sofar += (te - ts);
}

double Measurement::get_ru_elapsed_time() const {
  return ru_time_sofar;
}

double Measurement::get_papi_elapsed_time() const {
  return papi_time_sofar/1000.0;
}

double Measurement::get_gtd_elapsed_time() const {
  return gtd_time_sofar;
}

double Measurement::get_rclock_elapsed_time() const {
  return gtd_time_sofar;
}
