FROM eclipse-temurin:11-alpine
#Install curl for health check
RUN apk add --no-cache curl

ADD target/pulsar-mqtt-gateway-jar-with-dependencies.jar /usr/app/pulsar-mqtt-gateway.jar
ENTRYPOINT ["java", "-jar", "/usr/app/pulsar-mqtt-gateway.jar"]
