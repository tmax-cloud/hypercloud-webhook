#!/bin/sh
kubectl -n hypercloud4-system delete secret hypercloud4-webhook-certs
kubectl -n hypercloud4-system create secret generic hypercloud4-webhook-certs \
    --from-file=./pki/server.crt \
    --from-file=./pki/server.key
