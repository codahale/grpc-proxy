#!/bin/bash
# Based on a script which was written by Martijn Vermaat <martijn@vermaat.name>
set -euo pipefail
IFS=$'\n\t'

if [ $# -lt 1 ]; then
    echo "Usage: $0 <common name (or DNS name)> <DNS names or ip addresses...>"
    exit 1
fi

common_name="$1"
args="${@:2}"
config="$(mktemp)"

dnss=
ips=
for arg in ${args}; do
    if [[ "${arg}" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
        ips+="${arg} "
    else
        dnss+="${arg} "
    fi
done

altnames=
subjectaltline=

i=0
for dns in ${dnss}; do
    i=$(($i+1))
    altnames+="DNS.${i} = ${dns}"$'\n'
    subjectaltline="subjectAltName = @alt_names"
done

i=0
for ip in ${ips}; do
    i=$(($i+1))
    altnames+="IP.${i} = ${ip}"$'\n'
    subjectaltline="subjectAltName = @alt_names"
done

cat >"${config}" <<EOF
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_ca
req_extensions = v3_req
prompt = no

[req_distinguished_name]
CN = ${common_name}

[v3_ca]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
basicConstraints = CA:TRUE
${subjectaltline}

[v3_req]
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
basicConstraints = CA:FALSE
${subjectaltline}

[alt_names]
${altnames}
EOF

# Generate a ECDSA NIST P256 private key.
openssl ecparam -out cert.key.x509 -name prime256v1 -genkey

# Generate a self-signed certificate for the key.
openssl req -new -x509 -sha256 -nodes -key cert.key.x509 -out cert.crt -batch -config "${config}"

# Convert the key for PKCS8 format.
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in cert.key.x509 -out cert.key

# Remove the temporary key.
rm cert.key.x509
