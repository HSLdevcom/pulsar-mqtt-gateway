FROM eclipse-temurin:11-alpine
#Install curl for health check
RUN apk add --no-cache curl

ADD target/pulsar-mqtt-gateway.jar /usr/app/pulsar-mqtt-gateway.jar
ENTRYPOINT ["java", "-XX:InitialRAMPercentage=10.0", "-XX:MaxRAMPercentage=95.0", "-jar", "/usr/app/pulsar-mqtt-gateway.jar"]
