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

import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

class StatsTracerFactory extends ServerStreamTracer.Factory {

  private final LongAdder requests = new LongAdder();
  private final LongAdder responseTime = new LongAdder();
  private final Recorder latency = new Recorder(3);
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Override
  public ServerStreamTracer newServerStreamTracer(String fullMethodName, Metadata headers) {
    requests.increment();

    return new ServerStreamTracer() {
      final long start = System.nanoTime();

      @Override
      public void streamClosed(Status status) {
        final long duration = System.nanoTime() - start;
        responseTime.add(duration);
        latency.recordValue(TimeUnit.NANOSECONDS.toMicros(duration));
      }
    };
  }

  void start() {
    executor.scheduleAtFixedRate(() -> {
      final Histogram h = latency.getIntervalHistogram();
      final long n = requests.sumThenReset();
      System.out.printf("Stats at %s ==============\n", Instant.now());
      System.out.printf("  Throughput: %d req/sec\n", n);
      System.out.printf("  Avg Response Time: %2.4fs\n",
          (double) responseTime.sumThenReset() / n * 1e-9);
      System.out.printf("  p50:  %2.2fms\n", h.getValueAtPercentile(50) * 1e-3);
      System.out.printf("  p75:  %2.2fms\n", h.getValueAtPercentile(75) * 1e-3);
      System.out.printf("  p95:  %2.2fms\n", h.getValueAtPercentile(95) * 1e-3);
      System.out.printf("  p99:  %2.2fms\n", h.getValueAtPercentile(99) * 1e-3);
      System.out.printf("  p999: %2.2fms\n", h.getValueAtPercentile(99.9) * 1e-3);
    }, 1, 1, TimeUnit.SECONDS);
  }

  void stop() {
    executor.shutdown();
  }
}
