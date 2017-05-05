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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.ConcurrentHistogram;

public class MetricsServerStreamTracer extends ServerStreamTracer {

  private final ConcurrentHistogram latency;
  private final long start;

  private MetricsServerStreamTracer(ConcurrentHistogram latency) {
    this.latency = latency;
    this.start = System.nanoTime();
  }

  @Override
  public void streamClosed(Status status) {
    latency.recordValue(TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
  }

  public static class Factory extends ServerStreamTracer.Factory {

    private final LongAdder connections = new LongAdder();
    private final ConcurrentHistogram latency = new ConcurrentHistogram(3);

    @Override
    public ServerStreamTracer newServerStreamTracer(String fullMethodName, Metadata headers) {
      connections.increment();
      return new MetricsServerStreamTracer(latency);
    }

    @Override
    public String toString() {
      return String.format("%d connections, %2.2fms p99",
          connections.sum(), latency.getValueAtPercentile(99) * 1e-3);
    }
  }
}
