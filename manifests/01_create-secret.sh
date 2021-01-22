#!/bin/sh
kubectl -n hypercloud5-system delete secret hypercloud-webhook-certs
kubectl -n hypercloud5-system create secret generic hypercloud-webhook-certs \
    --from-file=./pki/server.crt \
    --from-file=./pki/server.key
