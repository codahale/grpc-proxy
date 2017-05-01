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

import io.grpc.MethodDescriptor.Marshaller;
import java.io.InputStream;

/**
 * A dumb marshaller which refuses to marshal.
 */
class StreamMarshaller implements Marshaller<InputStream> {

  @Override
  public InputStream stream(InputStream value) {
    return value;
  }

  @Override
  public InputStream parse(InputStream stream) {
    return stream;
  }
}
