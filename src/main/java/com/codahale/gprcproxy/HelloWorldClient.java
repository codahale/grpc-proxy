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

import com.codahale.grpcproxy.helloworld.GreeterGrpc;
import com.codahale.grpcproxy.helloworld.HelloReply;
import com.codahale.grpcproxy.helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.net.ssl.SSLException;

/**
 * A gRPC client. This could be in any language.
 */
public class HelloWorldClient {

  private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  private HelloWorldClient(String host, int port) throws SSLException {
    this.channel = NettyChannelBuilder.forAddress(host, port)
                                      .sslContext(TLS.clientContext())
                                      .build();
    this.blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  public static void main(String[] args) throws Exception {
    /* Access a service running on the local machine on port 50051 */
    final HelloWorldClient client = new HelloWorldClient("localhost", 50051);
    try {
      final int requests = 10_000;
      System.out.println(client.greet(requests));
      System.out.println("sending " + requests + " requests in parallel");
      final Instant start = Instant.now();
      final long[] durations = IntStream.range(0, requests)
                                        .parallel()
                                        .mapToLong(i -> {
                                          final long rstart = System.nanoTime();
                                          client.greet(i);
                                          return System.nanoTime() - rstart;
                                        })
                                        .sorted()
                                        .toArray();
      final Duration duration = Duration.between(start, Instant.now());
      final double rate = requests / (duration.toNanos() * 1e-9);
      System.out.printf("%d requests in %s (%2.2f req/sec)\n", requests, duration, rate);
      System.out.printf("p50 latency: %2.2fms\n", p(durations, 0.50));
      System.out.printf("p75 latency: %2.2fms\n", p(durations, 0.75));
      System.out.printf("p90 latency: %2.2fms\n", p(durations, 0.90));
      System.out.printf("p95 latency: %2.2fms\n", p(durations, 0.95));
      System.out.printf("p99 latency: %2.2fms\n", p(durations, 0.99));
      System.out.printf("p999 latency: %2.2fms\n", p(durations, 0.999));
    } finally {
      client.shutdown();
    }
  }

  private static double p(long[] durations, double p) {
    int low = (int) Math.floor(p * durations.length);
    int high = (int) Math.ceil(p * durations.length);
    return (durations[low] + durations[high]) / 2.0 * 1e-6;
  }

  private void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  private String greet(int i) {
    final HelloRequest request = HelloRequest.newBuilder().setName("world " + i).build();
    try {
      final HelloReply response = blockingStub.sayHello(request);
      return response.getMessage();
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return null;
    }
  }
}
