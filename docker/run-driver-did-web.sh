#!/bin/sh

mkdir -p ${uniregistrar_driver_did_web_basePath}

cd /opt/driver-did-web/
mvn jetty:run -P war -Dorg.eclipse.jetty.annotations.maxWait=240
