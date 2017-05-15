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

package com.codahale.grpcproxy;

import com.codahale.grpcproxy.helloworld.GreeterGrpc;
import com.codahale.grpcproxy.helloworld.HelloReply;
import com.codahale.grpcproxy.helloworld.HelloRequest;
import com.codahale.grpcproxy.stats.Recorder;
import com.codahale.grpcproxy.stats.Snapshot;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.EventLoopGroup;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A gRPC client. This could be in any language.
 */
public class HelloWorldClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldClient.class);

  private final EventLoopGroup eventLoopGroup;
  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  private HelloWorldClient(String host, int port) throws SSLException {
    this.eventLoopGroup = Netty.newWorkerEventLoopGroup();
    this.channel = NettyChannelBuilder.forAddress(host, port)
                                      .eventLoopGroup(eventLoopGroup)
                                      .channelType(Netty.clientChannelType())
                                      .sslContext(TLS.clientContext())
                                      .build();
    this.blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  public static void main(String[] args) throws Exception {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    /* Access a service running on the local machine on port 50051 */
    final HelloWorldClient client = new HelloWorldClient("localhost", 50051);
    try {
      final Map<String, String> env = System.getenv();
      final int requests = Integer.parseInt(env.getOrDefault("REQUESTS", "1000000"));
      final int threads = Integer.parseInt(env.getOrDefault("THREADS", "20"));
      final Recorder recorder = new Recorder(500, TimeUnit.MINUTES.toMicros(1),
          TimeUnit.MILLISECONDS.toMicros(10), TimeUnit.MICROSECONDS);
      LOGGER.info("Initial request: {}", client.greet(requests));
      LOGGER.info("Sending {} requests from {} threads", requests, threads);

      final ExecutorService threadPool = Executors.newFixedThreadPool(threads);
      final Instant start = Instant.now();
      for (int i = 0; i < threads; i++) {
        threadPool.submit(() -> {
          for (int j = 0; j < requests / threads; j++) {
            final long t = System.nanoTime();
            client.greet(j);
            recorder.record(t);
          }
        });
      }
      threadPool.shutdown();
      threadPool.awaitTermination(20, TimeUnit.MINUTES);

      final Snapshot stats = recorder.interval();
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.info(Markers.append("stats", stats)
                         .and(Markers.append("duration", duration.toString())),
          "{} requests in {} ({} req/sec)", stats.count(), duration, stats.throughput());
    } finally {
      client.shutdown();
    }
  }

  private void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
  }

  private String greet(int i) {
    final HelloRequest request = HelloRequest.newBuilder().setName("world " + i).build();
    try {
      final HelloReply response = blockingStub.sayHello(request);
      return response.getMessage();
    } catch (StatusRuntimeException e) {
      LOGGER.warn("RPC failed: {}", e.getStatus());
      return null;
    }
  }
}
