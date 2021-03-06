// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "platform/globals.h"
#if defined(TARGET_OS_ANDROID)

#include "vm/isolate.h"
#include "vm/json_stream.h"
#include "vm/profiler.h"
#include "vm/signal_handler.h"

namespace dart {

DECLARE_FLAG(bool, profile);
DECLARE_FLAG(bool, trace_profiled_isolates);

static void ProfileSignalAction(int signal, siginfo_t* info, void* context_) {
  if (signal != SIGPROF) {
    return;
  }
  Isolate* isolate = Isolate::Current();
  if (isolate == NULL) {
    return;
  }
  // Thread owns no profiler locks at this point.
  {
    // Thread owns isolate profiler data mutex.
    ScopedMutex profiler_data_lock(isolate->profiler_data_mutex());
    IsolateProfilerData* profiler_data = isolate->profiler_data();
    if ((profiler_data == NULL) || !profiler_data->CanExpire() ||
        (profiler_data->sample_buffer() == NULL)) {
      // Descheduled.
      return;
    }
    if (profiler_data->thread_id() == Thread::GetCurrentThreadId()) {
      // Still scheduled on this thread.
      // TODO(johnmccutchan): Perform sample on Android.
    }
  }
  // Thread owns no profiler locks at this point.
  // This call will acquire both ProfilerManager::monitor and the
  // isolate's profiler data mutex.
  ProfilerManager::ScheduleIsolate(isolate, true);
}


int64_t ProfilerManager::SampleAndRescheduleIsolates(int64_t current_time) {
  if (isolates_size_ == 0) {
    return 0;
  }
  static const int64_t max_time = 0x7fffffffffffffffLL;
  int64_t lowest = max_time;
  intptr_t i = 0;
  while (i < isolates_size_) {
    Isolate* isolate = isolates_[i];
    ScopedMutex isolate_lock(isolate->profiler_data_mutex());
    IsolateProfilerData* profiler_data = isolate->profiler_data();
    if (profiler_data == NULL) {
      // Isolate has been shutdown for profiling.
      RemoveIsolate(i);
      // Remove moves the last element into i, do not increment i.
      continue;
    }
    ASSERT(profiler_data != NULL);
    if (profiler_data->ShouldSample(current_time)) {
      pthread_kill(profiler_data->thread_id(), SIGPROF);
      RemoveIsolate(i);
      // Remove moves the last element into i, do not increment i.
      continue;
    }
    if (profiler_data->CanExpire()) {
      int64_t isolate_time_left =
          profiler_data->TimeUntilExpiration(current_time);
      if (isolate_time_left < 0) {
        continue;
      }
      if (isolate_time_left < lowest) {
        lowest = isolate_time_left;
      }
    }
    i++;
  }
  if (isolates_size_ == 0) {
    return 0;
  }
  if (lowest == max_time) {
    return 0;
  }
  ASSERT(lowest != max_time);
  ASSERT(lowest > 0);
  return lowest;
}


void ProfilerManager::ThreadMain(uword parameters) {
  ASSERT(initialized_);
  ASSERT(FLAG_profile);
  SignalHandler::Install(ProfileSignalAction);
  if (FLAG_trace_profiled_isolates) {
    OS::Print("ProfilerManager Android ready.\n");
  }
  {
    // Signal to main thread we are ready.
    ScopedMonitor startup_lock(start_stop_monitor_);
    thread_running_ = true;
    startup_lock.Notify();
  }
  ScopedMonitor lock(monitor_);
  while (!shutdown_) {
    int64_t current_time = OS::GetCurrentTimeMicros();
    int64_t next_sample = SampleAndRescheduleIsolates(current_time);
    lock.WaitMicros(next_sample);
  }
  if (FLAG_trace_profiled_isolates) {
    OS::Print("ProfilerManager Android exiting.\n");
  }
  {
    // Signal to main thread we are exiting.
    ScopedMonitor shutdown_lock(start_stop_monitor_);
    thread_running_ = false;
    shutdown_lock.Notify();
  }
}


}  // namespace dart

#endif  // defined(TARGET_OS_ANDROID)
