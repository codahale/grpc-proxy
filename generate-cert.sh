#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# Generate a ECDSA NIST P256 private key.
openssl ecparam -out cert.key.x509 -name prime256v1 -genkey

# Generate a self-signed certificate for the key.
openssl req -new -x509 -sha256 -nodes -key cert.key.x509 -out cert.crt -batch -subj "/C=US/CN=localhost"

# Convert the key for PKCS8 format.
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in cert.key.x509 -out cert.key

# Remove the temporary key.
rm cert.key.x509
