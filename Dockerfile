FROM maven:3.5.2-jdk-8-alpine AS build

#FROM maven:3.5.2-jdk-8-alpine AS MAVEN_TOOL_CHAIN
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn -P production package

#COPY src /usr/src/app/src
#COPY pom.xml /usr/src/app
#RUN mvn -f /usr/src/app/pom.xml -P production clean package

FROM openjdk:8-jre-alpine

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    SLEEP_TIME=0 \
    JAVA_OPTS=""

# add directly the war
#ADD *.war /app.war
COPY --from=build /tmp/target/back-sdk.war /app.war

EXPOSE 8080
CMD echo "The application will start in ${SLEEP_TIME}s..." && \
    sleep ${SLEEP_TIME} && \
    java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.war

