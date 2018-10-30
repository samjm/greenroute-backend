FROM maven:3.5.2-jdk-9 AS build

COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml -P production clean package

FROM openjdk:8-jre-alpine

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    SLEEP_TIME=0 \
    JAVA_OPTS=""

# add directly the war
#ADD *.war /app.war
COPY --from=build /usr/src/app/target/back-sdk.war /usr/app/back-sdk.war

EXPOSE 8080
CMD echo "The application will start in ${SLEEP_TIME}s..." && \
    sleep ${SLEEP_TIME} && \
    java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.war

