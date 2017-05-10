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

package com.codahale.gprcproxy.stats;

import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Histogram;

public class Recorder {

  private final IntervalAdder count;
  private final IntervalAdder responseTime;
  private final org.HdrHistogram.Recorder latency;
  private volatile Histogram histogram;

  public Recorder(long minLatency, long maxLatency, TimeUnit latencyUnit) {
    this.count = new IntervalAdder();
    this.responseTime = new IntervalAdder();
    this.latency = new org.HdrHistogram.Recorder(latencyUnit.toMicros(minLatency),
        latencyUnit.toMicros(maxLatency), 1);
    this.histogram = latency.getIntervalHistogram(); // preload reporting histogram
  }

  public boolean record(long startNanoTime) {
    final long duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNanoTime);
    count.add(1);
    responseTime.add(duration);
    try {
      latency.recordValue(duration);
      return true;
    } catch (ArrayIndexOutOfBoundsException ignored) {
      return false;
    }
  }

  public Snapshot interval() {
    final IntervalCount requestCount = count.interval();
    final IntervalCount responseTimeCount = responseTime.interval();
    final long c = requestCount.count();
    final double x = requestCount.mean();
    final double r, n;
    if (c == 0) {
      r = n = 0;
    } else {
      r = responseTimeCount.mean() / c * 1e-6;
      n = x * r;
    }
    this.histogram = latency.getIntervalHistogram(histogram);
    return new AutoValue_Snapshot(c, x, n, r,
        histogram.getValueAtPercentile(50) * 1e-6,
        histogram.getValueAtPercentile(90) * 1e-6,
        histogram.getValueAtPercentile(99) * 1e-6,
        histogram.getValueAtPercentile(99.9) * 1e-6);
  }
}
