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

import com.codahale.grpcproxy.helloworld.HelloReply;
import com.codahale.grpcproxy.helloworld.HelloRequest;
import java.io.IOException;
import java.util.concurrent.Executors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * An HTTP/1.1 server which parses protobuf messages in request bodies and emits protobuf messages
 * in response bodies. Implements, in its own way, the {@code helloworld.Greeter} service.
 */
public class LegacyHttpService extends AbstractHandler {

  public static void main(String[] args) throws Exception {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    final ThreadPool threadPool = new ExecutorThreadPool(Executors.newCachedThreadPool());
    final Server server = new Server(threadPool);
    server.setHandler(new LegacyHttpService());

    final ServerConnector connector = new ServerConnector(server);
    connector.setPort(8080);
    server.addConnector(connector);

    server.start();
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    final String method = baseRequest.getParameter("method");
    if ("helloworld.Greeter/SayHello".equals(method)) {
      baseRequest.setHandled(true);
      sayHello(baseRequest, response);
    }
  }

  private void sayHello(Request request, HttpServletResponse response)
      throws IOException {
    final HelloRequest req = HelloRequest.parseFrom(request.getInputStream());
    final String greeting = "Hello " + req.getName();
    final HelloReply resp = HelloReply.newBuilder().setMessage(greeting).build();
    resp.writeTo(response.getOutputStream());
  }
}
