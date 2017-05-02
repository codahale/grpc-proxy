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

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Builder;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerMethodDefinition;
import io.grpc.stub.ServerCalls;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * A handler registry which maps gRPC service methods to proxy listeners.
 */
class DynamicHandlerRegistry extends HandlerRegistry {

  private final HttpUrl url;
  private final OkHttpClient client;

  DynamicHandlerRegistry(HttpUrl url) {
    this.url = url;
    this.client = new OkHttpClient.Builder().build();
  }

  @Override
  public ServerMethodDefinition<?, ?> lookupMethod(String methodName,
      @Nullable String authority) {

    final Builder<byte[], byte[]> md = MethodDescriptor.newBuilder();
    md.setRequestMarshaller(new ByteArrayMarshaller());
    md.setResponseMarshaller(new ByteArrayMarshaller());
    md.setType(MethodType.UNARY);
    md.setFullMethodName(methodName);
//    md.setIdempotent(true); // is GET, DELETE, or PUT
//    md.setSafe(true); // is GET

    final DynamicUnaryCall handler = new DynamicUnaryCall(client, url, methodName);
    return ServerMethodDefinition.create(md.build(), ServerCalls.asyncUnaryCall(handler));
  }
}
