#!/bin/sh

cd /opt/driver-did-web/
mvn jetty:run -P war -Dorg.eclipse.jetty.annotations.maxWait=240
