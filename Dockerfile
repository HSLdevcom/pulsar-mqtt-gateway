FROM openjdk:8-jre-slim
#Install curl for health check
RUN apt-get update && apt-get install -y --no-install-recommends curl
ADD target/pulsar-mqtt-gateway-jar-with-dependencies.jar /usr/app/pulsar-mqtt-gateway.jar
ENTRYPOINT ["java", "-jar", "/usr/app/pulsar-mqtt-gateway.jar"]