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

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerMethodDefinition;
import io.grpc.stub.ServerCalls;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * A handler registry which maps gRPC service methods to proxy listeners.
 */
class ProxyHandlerRegistry extends HandlerRegistry {

  private final HttpUrl backend;
  private final OkHttpClient client;

  ProxyHandlerRegistry(HttpUrl backend) {
    this.backend = backend;
    final ConnectionPool connectionPool = new ConnectionPool(100, 5, TimeUnit.MINUTES);
    this.client = new OkHttpClient.Builder().connectionPool(connectionPool).build();
  }

  @Override
  public ServerMethodDefinition<?, ?> lookupMethod(String methodName,
      @Nullable String authority) {
    return ServerMethodDefinition.create(
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setRequestMarshaller(new ByteArrayMarshaller())
            .setResponseMarshaller(new ByteArrayMarshaller())
            .setType(MethodType.UNARY)
            .setFullMethodName(methodName)
            .build(),
        ServerCalls.asyncUnaryCall(new ProxyUnaryMethod(client, backend, methodName)));
  }
}
