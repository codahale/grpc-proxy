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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Snapshot {

  @JsonProperty
  public abstract long count();

  @JsonProperty
  public abstract double throughput();

  @JsonProperty
  public abstract double concurrency();

  @JsonProperty
  public abstract double latency();

  @JsonProperty
  public abstract double p50();

  @JsonProperty
  public abstract double p90();

  @JsonProperty
  public abstract double p99();

  @JsonProperty
  public abstract double p999();

  @JsonProperty
  public abstract double apdex();
}
