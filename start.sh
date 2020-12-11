#!/bin/sh

export K8S_HOME=/home/tmax/hypercloud4-webhook
/usr/bin/java -jar -Dlogback.configurationFile=${K8S_HOME}/logback.xml ${K8S_HOME}/lib/hypercloud4-webhook.jar
