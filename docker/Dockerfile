# Dockerfile for universalregistrar/driver-did-web

FROM maven:3-eclipse-temurin-17-focal AS build
MAINTAINER Markus Sabadello <markus@danubetech.com>

# build driver-did-web

ADD . /opt/driver-did-web
RUN cd /opt/driver-did-web && mvn clean install package -P war -N

# build image

FROM jetty:9-jdk17-eclipse-temurin
MAINTAINER Markus Sabadello <markus@danubetech.com>

# variables

ENV uniregistrar_driver_did_web_basePath=
ENV uniregistrar_driver_did_web_baseUrl=
ENV uniregistrar_driver_did_web_generatedFolder=

# copy from build-stage
COPY --from=build --chown=jetty /opt/driver-did-web/target/*.war /var/lib/jetty/webapps/ROOT.war

# change owner of the /opt to jetty
USER root
RUN chown -R jetty:jetty /opt
USER jetty

#done

EXPOSE 9080
#CMD  /opt/driver-did-web/ && java -Djetty.http.port=9080 -Dorg.eclipse.jetty.annotations.maxWait=240 -jar /usr/local/jetty/start.jar
CMD java -Djetty.http.port=9080 -Dorg.eclipse.jetty.annotations.maxWait=240 -jar /usr/local/jetty/start.jar
