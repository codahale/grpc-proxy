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
import io.grpc.stub.StreamObserver;

class GreeterService extends GreeterGrpc.GreeterImplBase {

  @Override
  public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
    final String message = "Hello " + request.getName();
    responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
    responseObserver.onCompleted();
  }
}
