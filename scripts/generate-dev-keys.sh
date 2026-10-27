#!/usr/bin/env bash
# Generates a throw-away RSA key pair for local JWT signing into ./keys (ignored by git).
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/privateKey.pem
openssl rsa -pubout -in keys/privateKey.pem -out keys/publicKey.pem
echo "Generated keys/privateKey.pem and keys/publicKey.pem"
