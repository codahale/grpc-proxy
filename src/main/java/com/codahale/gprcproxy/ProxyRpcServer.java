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

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import okhttp3.HttpUrl;

/**
 * A gRPC server which proxies requests to an HTTP/1.1 backend server.
 */
public class ProxyRpcServer {

  private static final Logger logger = Logger.getLogger(ProxyRpcServer.class.getName());

  private final ExecutorService executor;
  private final Server server;
  private final StatsTracerFactory stats;

  private ProxyRpcServer(int port, HttpUrl backend) throws SSLException {
    this.executor = Executors.newFixedThreadPool(20);
    this.stats = new StatsTracerFactory();
    this.server = NettyServerBuilder.forPort(port)
                                    .executor(executor)
                                    .addStreamTracerFactory(stats)
                                    .sslContext(TLS.serverContext())
                                    .fallbackHandlerRegistry(new ProxyHandlerRegistry(backend))
                                    .build();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    final ProxyRpcServer server = new ProxyRpcServer(50051,
        HttpUrl.parse("http://localhost:8080/grpc"));
    server.start();
    server.blockUntilShutdown();
  }

  private void start() throws IOException {
    stats.start();
    server.start();
    logger.info("Server started, listening on " + server.getPort());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Use stderr here since the logger may have been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      ProxyRpcServer.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  private void stop() {
    stats.stop();
    if (!server.isShutdown()) {
      server.shutdown();
    }
    executor.shutdown();
  }

  private void blockUntilShutdown() throws InterruptedException {
    server.awaitTermination();
  }
}
