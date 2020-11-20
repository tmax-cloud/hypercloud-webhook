#!/bin/sh
kubectl delete deploy postgres &&
rm -rf /mnt/postgres/* 
