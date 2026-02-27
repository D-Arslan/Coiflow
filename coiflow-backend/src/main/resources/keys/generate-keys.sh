#!/bin/bash
# Generate RSA key pair for JWT RS256 signing (dev only)
# Run this script from the keys/ directory

openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem

echo "Keys generated: private.pem, public.pem"
