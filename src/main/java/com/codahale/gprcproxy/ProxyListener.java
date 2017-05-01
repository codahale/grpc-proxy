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

import com.google.common.io.ByteStreams;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.Status;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * A listener which proxies request bodies and response bodies to/from a backend HTTP/1.1 server.
 */
class ProxyListener extends Listener<InputStream> {

  private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
  private final OkHttpClient client;
  private final HttpUrl url;
  private final String methodName;
  private final ServerCall<InputStream, InputStream> serverCall;

  ProxyListener(OkHttpClient client, HttpUrl url, String methodName,
      ServerCall<InputStream, InputStream> serverCall) {
    this.client = client;
    this.url = url;
    this.methodName = methodName;
    this.serverCall = serverCall;
  }

  @Override
  public void onReady() {
    serverCall.request(1);
  }

  @Override
  public void onMessage(InputStream message) {
    try {
      // read the request body
      final ByteArrayOutputStream msg = new ByteArrayOutputStream();
      try (InputStream in = message) {
        ByteStreams.copy(in, msg);
      }
      final RequestBody body = RequestBody.create(OCTET_STREAM, msg.toByteArray());

      // send the request to the backend
      client.newCall(new Request.Builder().url(url.newBuilder()
                                                  .addQueryParameter("method", methodName)
                                                  .build())
                                          .post(body)
                                          .build())
            .enqueue(new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                serverCall.close(Status.INTERNAL, new Metadata());
              }

              @Override
              public void onResponse(Call call, okhttp3.Response response) throws IOException {
                // proxy the response body
                if (response.code() == 200) {
                  serverCall.sendHeaders(new Metadata());
                  serverCall.sendMessage(response.body().byteStream());
                  serverCall.close(Status.OK, new Metadata());
                } else if (response.code() == 404) {
                  serverCall.close(Status.UNIMPLEMENTED, new Metadata());
                } else if (response.code() <= 500) {
                  serverCall.close(Status.INTERNAL, new Metadata());
                } else {
                  serverCall.close(Status.UNKNOWN, new Metadata());
                }
              }
            });
    } catch (Exception e) {
      e.printStackTrace();
      serverCall.close(Status.INTERNAL, new Metadata());
    }
  }
}
