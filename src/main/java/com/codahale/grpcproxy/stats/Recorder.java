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

package com.codahale.grpcproxy.stats;

import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Recorder {

  private static final Logger LOGGER = LoggerFactory.getLogger(Recorder.class.getCanonicalName());

  private final IntervalAdder count;
  private final IntervalAdder responseTime;
  private final org.HdrHistogram.Recorder latency;
  private final long goalLatency;
  private volatile Histogram histogram;

  public Recorder(long minLatency, long maxLatency, long goalLatency, TimeUnit latencyUnit) {
    this.goalLatency = latencyUnit.toMicros(goalLatency);
    this.count = new IntervalAdder();
    this.responseTime = new IntervalAdder();
    this.latency = new org.HdrHistogram.Recorder(latencyUnit.toMicros(minLatency),
        latencyUnit.toMicros(maxLatency), 1);
    this.histogram = latency.getIntervalHistogram(); // preload reporting histogram
  }

  public void record(long startNanoTime) {
    final long duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNanoTime);
    count.add(1);
    responseTime.add(duration);
    try {
      latency.recordValue(duration);
    } catch (ArrayIndexOutOfBoundsException ignored) {
      LOGGER.warn("Very slow value: {}us", duration);
    }
  }

  public Snapshot interval() {
    final IntervalCount requestCount = count.interval();
    final IntervalCount responseTimeCount = responseTime.interval();
    final Histogram h = latency.getIntervalHistogram(histogram);
    final long c = requestCount.count();
    final double x = requestCount.rate();
    final long satisfied = h.getCountBetweenValues(0, goalLatency);
    final long tolerating = h.getCountBetweenValues(goalLatency, goalLatency * 4);
    final double p50 = h.getValueAtPercentile(50) * 1e-6;
    final double p90 = h.getValueAtPercentile(90) * 1e-6;
    final double p99 = h.getValueAtPercentile(99) * 1e-6;
    final double p999 = h.getValueAtPercentile(99.9) * 1e-6;
    this.histogram = h;
    final double r, n, apdex;
    if (c == 0) {
      r = n = apdex = 0;
    } else {
      r = responseTimeCount.rate() / c * 1e-6;
      n = x * r;
      apdex = Math.min(1.0, (satisfied + (tolerating / 2.0)) / c);
    }
    return new AutoValue_Snapshot(c, x, n, r, p50, p90, p99, p999, apdex);
  }
}
