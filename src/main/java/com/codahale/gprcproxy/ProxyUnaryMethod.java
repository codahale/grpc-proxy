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

import io.grpc.stub.ServerCalls.UnaryMethod;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Proxies a gRPC request to an HTTP backend.
 */
class ProxyUnaryMethod implements UnaryMethod<byte[], byte[]> {

  private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
  private final OkHttpClient client;
  private final HttpUrl url;
  private final String methodName;

  ProxyUnaryMethod(OkHttpClient client, HttpUrl url, String methodName) {
    this.client = client;
    this.url = url;
    this.methodName = methodName;
  }

  @Override
  public void invoke(byte[] request, StreamObserver<byte[]> responseObserver) {
    client.newCall(new Request.Builder().url(url.newBuilder()
                                                .addQueryParameter("method", methodName)
                                                .build())
                                        .post(RequestBody.create(OCTET_STREAM, request))
                                        .build())
          .enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
              responseObserver.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
              responseObserver.onNext(response.body().bytes());
              responseObserver.onCompleted();
            }
          });
  }
}
