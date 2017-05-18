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

import io.airlift.airline.Cli;
import io.airlift.airline.Help;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Runner {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    Cli.<Runnable>builder("grpc-proxy")
        .withDefaultCommand(Help.class)
        .withCommands(Help.class, ProxyRpcServer.Cmd.class, LegacyHttpService.Cmd.class,
            HelloWorldClient.Cmd.class, HelloWorldServer.Cmd.class)
        .build()
        .parse(args)
        .run();
  }
}
