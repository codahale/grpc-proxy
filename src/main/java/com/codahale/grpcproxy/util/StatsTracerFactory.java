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

package com.codahale.grpcproxy.util;

import com.codahale.grpcproxy.stats.IntervalAdder;
import com.codahale.grpcproxy.stats.Recorder;
import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stream tracer factory which measures throughput, concurrency, response time, and latency
 * distribution.
 */
public class StatsTracerFactory extends ServerStreamTracer.Factory {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsTracerFactory.class);
  private static final long MIN_DURATION = TimeUnit.MICROSECONDS.toMicros(500);
  private static final long GOAL_DURATION = TimeUnit.MILLISECONDS.toMicros(10);
  private static final long MAX_DURATION = TimeUnit.SECONDS.toMicros(30);

  private final IntervalAdder bytesIn = new IntervalAdder();
  private final IntervalAdder bytesOut = new IntervalAdder();
  private final Recorder all = newRecorder();
  private final ConcurrentMap<String, Recorder> endpoints = new ConcurrentHashMap<>();
  private ScheduledExecutorService executor;

  @Override
  public ServerStreamTracer newServerStreamTracer(String fullMethodName, Metadata headers) {

    return new ServerStreamTracer() {
      final long start = System.nanoTime();
      final Recorder endpoint = endpoints.computeIfAbsent(fullMethodName, k -> newRecorder());

      @Override
      public void outboundWireSize(long bytes) {
        bytesOut.add(bytes);
      }

      @Override
      public void inboundWireSize(long bytes) {
        bytesIn.add(bytes);
      }

      @Override
      public void streamClosed(Status status) {
        final double duration = (System.nanoTime() - start) * 1e-9;
        LOGGER.debug(Markers.append("grpc_method_name", fullMethodName)
                            .and(Markers.append("status", status))
                            .and(Markers.append("duration", duration)), "request handled");
        all.record(start);
        endpoint.record(start);
      }
    };
  }

  public void start() {
    executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(this::report, 1, 1, TimeUnit.SECONDS);
  }

  public void stop() {
    executor.shutdown();
  }

  /**
   * Calculate and report the three parameters of Little's Law and some latency percentiles.
   *
   * This just writes them to stdout, but presumably we'd be reporting them to a centralized
   * service.
   */
  private void report() {
    LogstashMarker marker = Markers.append("all", all.interval())
                                   .and(Markers.append("bytes_in", bytesIn.interval()))
                                   .and(Markers.append("bytes_out", bytesOut.interval()));
    for (Entry<String, Recorder> entry : endpoints.entrySet()) {
      marker = marker.and(Markers.append(entry.getKey(), entry.getValue().interval()));
    }
    LOGGER.info(marker, "stats");
  }

  private Recorder newRecorder() {
    return new Recorder(MIN_DURATION, MAX_DURATION, GOAL_DURATION, TimeUnit.MICROSECONDS);
  }
}
