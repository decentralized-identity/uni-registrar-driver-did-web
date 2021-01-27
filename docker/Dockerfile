# Dockerfile for universalregistrar/driver-did-web

FROM maven:3-openjdk-11
MAINTAINER Markus Sabadello <markus@danubetech.com>

USER root

ENV PATH $JAVA_HOME/bin:$PATH

# build driver-did-web

ADD . /opt/driver-did-web
RUN cd /opt/driver-did-web && mvn clean install package -N -DskipTests

# variables

ENV uniregistrar_driver_did_web_basePath=
ENV uniregistrar_driver_did_web_baseUrl=

# done

EXPOSE 9080

RUN chmod a+rx /opt/driver-did-web/docker/run-driver-did-web.sh
CMD "/opt/driver-did-web/docker/run-driver-did-web.sh"
