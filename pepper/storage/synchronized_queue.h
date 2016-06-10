#ifndef SYNCHRONIZED_QUEUE_H_
#define SYNCHRONIZED_QUEUE_H_

#include <queue>
#include <pthread.h>
#include <vector>

/*
 * A thread safe queue implementation.
 *
 * Since it's a template, everything is inside the header file. Otherwise
 * undefined reference error would occur. Any idea how to fix this?
 */
template<class T>
class SynchronizedQueue {
  public:
    SynchronizedQueue();
    void Push(T element);
    T Pop();
    std::vector<T> FlushQueue();
  private:
    std::queue<T> queue;

    pthread_mutex_t queue_access_mutex;
    pthread_cond_t queue_nonempty_condition;
};

template<class T> SynchronizedQueue<T>::SynchronizedQueue() {
  pthread_mutex_init(&queue_access_mutex, NULL);
  pthread_cond_init(&queue_nonempty_condition, NULL);
}

template<class T> void SynchronizedQueue<T>::Push(T element) {
  pthread_mutex_lock(&queue_access_mutex);
  queue.push(element);
  pthread_cond_signal(&queue_nonempty_condition);
  pthread_mutex_unlock(&queue_access_mutex);
}

template<class T> T SynchronizedQueue<T>::Pop() {
  pthread_mutex_lock(&queue_access_mutex);
  while(queue.empty()) {
    pthread_cond_wait(&queue_nonempty_condition, &queue_access_mutex);
  }
  T result = queue.front();
  queue.pop();
  pthread_mutex_unlock(&queue_access_mutex);
  return result;
}

template<class T> std::vector<T> SynchronizedQueue<T>::FlushQueue() {
  pthread_mutex_lock(&queue_access_mutex);
  std::vector<T> results;
  while (!queue.empty()) {
    T result = queue.front();
    queue.pop();
    results.push_back(result);
  }

  pthread_mutex_unlock(&queue_access_mutex);
  return results;
}

#endif /* SYNCHRONIZED_QUEUE_H_ */
