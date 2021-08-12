# Dockerfile for universalregistrar/driver-did-web

FROM maven:3-openjdk-11 AS build
MAINTAINER Markus Sabadello <markus@danubetech.com>

# build driver-did-web

ADD . /opt/driver-did-web
RUN cd /opt/driver-did-web && mvn clean install package -P war -N -DskipTests


FROM jetty:9.4-jre11-slim
MAINTAINER Markus Sabadello <markus@danubetech.com>

# variables

ENV uniregistrar_driver_did_web_basePath=
ENV uniregistrar_driver_did_web_baseUrl=
ENV uniregistrar_driver_did_web_generatedFolder=

# copy from build-stage
COPY --from=build --chown=jetty /opt/driver-did-web/target/*.war /var/lib/jetty/webapps/ROOT.war

#done

EXPOSE 9080
CMD java -Djetty.http.port=9080 -Dorg.eclipse.jetty.annotations.maxWait=240 -jar /usr/local/jetty/start.jar
