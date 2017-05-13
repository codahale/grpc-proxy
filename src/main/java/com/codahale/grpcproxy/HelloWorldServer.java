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

import com.codahale.grpcproxy.helloworld.GreeterGrpc.GreeterImplBase;
import com.codahale.grpcproxy.helloworld.HelloReply;
import com.codahale.grpcproxy.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class HelloWorldServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRpcServer.class);

  private final EventLoopGroup bossEventLoopGroup;
  private final EventLoopGroup workerEventLoopGroup;
  private final Server server;
  private final StatsTracerFactory stats;

  private HelloWorldServer(int port) throws SSLException {
    this.stats = new StatsTracerFactory();
    this.bossEventLoopGroup = Netty.newBossEventLoopGroup();
    this.workerEventLoopGroup = Netty.newWorkerEventLoopGroup();
    this.server = NettyServerBuilder.forPort(port)
                                    .bossEventLoopGroup(bossEventLoopGroup)
                                    .workerEventLoopGroup(workerEventLoopGroup)
                                    .channelType(Netty.serverChannelType())
                                    .addStreamTracerFactory(stats)
                                    .sslContext(TLS.serverContext())
                                    .addService(new GreeterImplBase() {
                                      @Override
                                      public void sayHello(HelloRequest request,
                                          StreamObserver<HelloReply> responseObserver) {
                                        final String message = "Hello " + request.getName();
                                        responseObserver.onNext(HelloReply.newBuilder()
                                                                          .setMessage(message)
                                                                          .build());
                                        responseObserver.onCompleted();
                                      }
                                    })
                                    .build();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    final HelloWorldServer server = new HelloWorldServer(50051);
    server.start();
    server.blockUntilShutdown();
  }

  private void start() throws IOException {
    stats.start();
    server.start();
    LOGGER.info("Server started, listening on {}", server.getPort());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Use stderr here since the logger may have been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      HelloWorldServer.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  private void stop() {
    stats.stop();
    if (!server.isShutdown()) {
      server.shutdown();
    }
    bossEventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    workerEventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
  }

  private void blockUntilShutdown() throws InterruptedException {
    server.awaitTermination();
  }
}
