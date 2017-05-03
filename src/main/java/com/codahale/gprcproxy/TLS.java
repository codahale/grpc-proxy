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

import static io.netty.handler.ssl.SslProvider.OPENSSL;

import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.nio.file.Paths;
import javax.net.ssl.SSLException;

class TLS {

  private static final File TLS_CERT = Paths.get("cert.crt").toFile();
  private static final File TLS_KEY = Paths.get("cert.key").toFile();

  private static void checkFiles() {
    if (!TLS_CERT.exists() || !TLS_KEY.exists()) {
      throw new IllegalStateException("Can't find cert files. Run generate-cert.sh");
    }
  }

  static SslContext clientContext() throws SSLException {
    checkFiles();
    return GrpcSslContexts.configure(SslContextBuilder.forClient(), OPENSSL)
                          .trustManager(TLS_CERT)
                          .keyManager(TLS_CERT, TLS_KEY)
                          .build();
  }

  static SslContext serverContext() throws SSLException {
    checkFiles();
    return GrpcSslContexts.configure(SslContextBuilder.forServer(TLS_CERT, TLS_KEY), OPENSSL)
                          .trustManager(TLS_CERT)
                          .clientAuth(ClientAuth.REQUIRE)
                          .build();
  }
}
