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

import com.codahale.grpcproxy.helloworld.HelloReply;
import com.codahale.grpcproxy.helloworld.HelloRequest;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * An HTTP/1.1 server which parses protobuf messages in request bodies and emits protobuf messages
 * in response bodies. Implements, in its own way, the {@code helloworld.Greeter} service.
 */
public class HttpServer {

  public static void main(String[] args) throws Exception {
    final Server server = new Server(8080);
    final AbstractHandler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response) throws IOException, ServletException {
        final String method = baseRequest.getParameter("method");
        System.out.println(method);
        if ("helloworld.Greeter/SayHello".equals(method)) {
          baseRequest.setHandled(true);
          final HelloRequest req = HelloRequest.parseFrom(request.getInputStream());
          System.out.println("Saying hello to " + req + " at " + baseRequest);
          final HelloReply resp = HelloReply.newBuilder().setMessage("Hello " + req.getName())
                                            .build();
          response.setStatus(HttpServletResponse.SC_OK);
          resp.writeTo(response.getOutputStream());
        }
      }
    };
    server.setHandler(handler);
    server.start();
    server.join();
  }
}
