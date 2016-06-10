#ifndef CODE_PEPPER_COMMON_MEASUREMENT_H_  
#define CODE_PEPPER_COMMON_MEASUREMENT_H_  

#include <time.h>
#include <cstddef>
#include <sys/time.h>
#include <stdint.h>
#include <sys/resource.h>
#include <papi.h>

#define INNER_LOOP_TINY 1
#define INNER_LOOP_SMALL 5 
#define INNER_LOOP_MEDIUM 100
#define INNER_LOOP_LARGE 1000
#define INNER_LOOP_XLARGE 10000

class Measurement {
  private:
    struct rusage ru_start, ru_end;
    long long papi_start, papi_end;
    double ru_time_sofar, papi_time_sofar, gtd_time_sofar, rclock_time_sofar;
    struct timeval gtd_start, gtd_end;
    clockid_t rclock_id;
    struct timespec rclock_start, rclock_end;

  public:
    Measurement();

    Measurement& operator+=(const Measurement& other);
    const Measurement operator+(const Measurement& other) const;

    Measurement& operator-=(const Measurement& other);
    const Measurement operator-(const Measurement& other) const;

    void clear(); // Same as reset. For consistency w/ STL convention
    void reset();
    void begin_with_init();
    void begin_with_history();
    void end();

    double get_ru_elapsed_time() const;
    double get_papi_elapsed_time() const;
    double get_gtd_elapsed_time() const;
    double get_rclock_elapsed_time() const;
};
#endif  // CODE_PEPPER_COMMON_MEASUREMENT_H_
