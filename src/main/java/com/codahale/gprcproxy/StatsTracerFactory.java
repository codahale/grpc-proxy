/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codahale.gprcproxy;

import com.codahale.gprcproxy.stats.Recorder;
import com.codahale.gprcproxy.stats.Snapshot;
import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A stream tracer factory which measures throughput, concurrency, response time, and latency
 * distribution.
 */
class StatsTracerFactory extends ServerStreamTracer.Factory {

  private static final long MIN_DURATION = TimeUnit.MICROSECONDS.toMicros(500);
  private static final long MAX_DURATION = TimeUnit.SECONDS.toMicros(30);

  private final com.codahale.gprcproxy.stats.Recorder all = newRecorder();
  private final ConcurrentMap<String, Recorder> endpoints = new ConcurrentHashMap<>();
  private ScheduledExecutorService executor;

  @Override
  public ServerStreamTracer newServerStreamTracer(String fullMethodName, Metadata headers) {

    return new ServerStreamTracer() {
      final long start = System.nanoTime();
      final Recorder endpoint = endpoints.computeIfAbsent(fullMethodName, k -> newRecorder());

      @Override
      public void streamClosed(Status status) {
        all.record(start);
        endpoint.record(start);
      }
    };
  }

  void start() {
    executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(this::report, 1, 1, TimeUnit.SECONDS);
  }

  void stop() {
    executor.shutdown();
  }

  /**
   * Calculate and report the three parameters of Little's Law and some latency percentiles.
   *
   * This just writes them to stdout, but presumably we'd be reporting them to a centralized
   * service.
   */
  private void report() {
    final Snapshot allStats = all.interval();
    final Map<String, Snapshot> endpointStats = new TreeMap<>();
    endpoints.forEach((method, recorder) -> endpointStats.put(method, recorder.interval()));

    System.out.printf("Stats at %s ==============\n", Instant.now());
    System.out.printf("All endpoints:\n");
    System.out.printf("  Throughput: %2.2f req/sec\n", allStats.throughput());
    System.out.printf("  Avg Response Time: %2.4fs\n", allStats.latency());
    System.out.printf("  Avg Concurrent Clients: %2.4f\n", allStats.concurrency());
    System.out.printf("  p50:  %2.4fs\n", allStats.p50());
    System.out.printf("  p90:  %2.4fs\n", allStats.p90());
    System.out.printf("  p99:  %2.4fs\n", allStats.p99());
    System.out.printf("  p999:  %2.4fs\n", allStats.p999());

    endpointStats.forEach((method, stats) -> {
      System.out.printf("%s:\n", method);
      System.out.printf("  Throughput: %2.2f req/sec\n", stats.throughput());
      System.out.printf("  Avg Response Time: %2.4fs\n", stats.latency());
      System.out.printf("  Avg Concurrent Clients: %2.4f\n", stats.concurrency());
      System.out.printf("  p50:  %2.4fs\n", stats.p50());
      System.out.printf("  p90:  %2.4fs\n", stats.p90());
      System.out.printf("  p99:  %2.4fs\n", stats.p99());
      System.out.printf("  p999:  %2.4fs\n", stats.p999());
    });
  }

  private Recorder newRecorder() {
    return new Recorder(MIN_DURATION, MAX_DURATION, TimeUnit.MICROSECONDS);
  }
}
