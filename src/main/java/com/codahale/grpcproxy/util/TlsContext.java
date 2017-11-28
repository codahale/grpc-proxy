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

package com.codahale.grpcproxy.util;

import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.File;
import javax.net.ssl.SSLException;

public class TlsContext {

  private final File trustedCerts, cert, key;

  public TlsContext(String trustedCertsPath, String certPath, String keyPath) {
    this.trustedCerts = new File(trustedCertsPath);
    if (!trustedCerts.exists()) {
      throw new IllegalArgumentException("Can't find " + trustedCertsPath);
    }

    this.cert = new File(certPath);
    if (!cert.exists()) {
      throw new IllegalArgumentException("Can't find " + certPath);
    }

    this.key = new File(keyPath);
    if (!key.exists()) {
      throw new IllegalArgumentException("Can't find " + keyPath);
    }
  }

  public SslContext toClientContext() throws SSLException {
    return GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
        .trustManager(trustedCerts)
        .keyManager(cert, key)
        .build();
  }

  public SslContext toServerContext() throws SSLException {
    return GrpcSslContexts.configure(SslContextBuilder.forServer(cert, key), SslProvider.OPENSSL)
        .trustManager(trustedCerts)
        .clientAuth(ClientAuth.REQUIRE)
        .build();
  }
}
