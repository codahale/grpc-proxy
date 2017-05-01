# gprc-proxy

An experiment in writing a gRPC service proxy for functionality which is implemented in HTTP/1.1.

## No. Why.

Sometimes things are written in languages which aren't well-suited to running complex, long-lived
services like gRPC. Sometimes things are running in web servers and that already works really well.
Sometimes those things do important work that other stuff in other platforms needs to use.

As for gRPC, it's the best RPC framework because it's a thin layer on top of HTTP2, which has lovely
multiplexing, etc. etc. all in a totally standard protocol.

## Ok. How.

This project has three moving parts:

1. An HTTP/1.1 server (running on Jetty) which implements a Protobuf-based hello world service.
   Pretending to be a bunch of well-tested, battle-hardened business logic trapped in a doofy 
   runtime.
2. A gRPC client which implements the client-side of that service, but not over HTTP/1.1. Just a
   bog-standard gRPC client. How ever will it talk to the HTTP/1.1 server?
3. Our hero, a proxying gRPC server.

## What's it use

* gRPC 1.3.0
* OkHttp

## Should I use this

Not in its current form, hell no. This is just a proof-of-concept.

## License

Copyright © 2017 Coda Hale

Distributed under the Apache License 2.0.
