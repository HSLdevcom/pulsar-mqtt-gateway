# pulsar-mqtt-gateway [![CI/CD](https://github.com/HSLdevcom/pulsar-mqtt-gateway/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/HSLdevcom/pulsar-mqtt-gateway/actions/workflows/ci-cd.yml)

## Description

Application for publishing Pulsar messages to MQTT. The application doesn't care about 
the data content, it only publishes the binary message as-is.

## Building

### Dependencies

This project depends on [transitdata-common](https://github.com/HSLdevcom/transitdata-common) project.

`transitdata-common` is published to GitHub Packages, which requires authentication even for reading.
Export a GitHub token with the `read:packages` scope before building; `.mvn/settings.xml` reads it:

```bash
export GITHUB_ACTOR=<your GitHub username>
export GITHUB_TOKEN=<token with read:packages>
```

Alternatively, build your own version and install it to the local Maven repository:
- `cd transitdata-common && ./mvnw install`

### Locally

Requires Java 25. Use the Maven wrapper:

- `./mvnw compile`
- `./mvnw test` runs the unit tests (`*Test`)
- `./mvnw verify` also runs the integration tests (`*IT`). They use Testcontainers (Pulsar and Mosquitto), so Docker must be running.
- `./mvnw spotless:apply` formats the code; CI only runs `spotless:check`
- `./mvnw package` builds `target/pulsar-mqtt-gateway.jar`

### Docker image

- Run [this script](build-image.sh) to build the Docker image. It passes `GITHUB_TOKEN` to the build as a BuildKit secret.

### CI/CD

[`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) uses the shared
[`ci-cd-java.yml`](https://github.com/HSLdevcom/transitdata-shared-workflows) workflow: formatting check, unit tests
inside the JDK base image, integration tests, Docker build. Pushes to `main` publish `hsldevcom/pulsar-mqtt-gateway:main`
and `:sha-<sha>`; tags `vX.Y.Z` publish `:X.Y.Z`.

## Running

### Dependencies

* Pulsar
* Connection to a MQTT broker

### Environment variables

* `MQTT_HAS_AUTHENTICATION`: whether the MQTT broker uses authentication
* `MQTT_BROKER_USERNAME`: username for the MQTT broker, required if `MQTT_HAS_AUTHENTICATION` is `true`
* `MQTT_BROKER_PASSWORD`: password for the MQTT broker, required if `MQTT_HAS_AUTHENTICATION` is `true`
* `MQTT_BROKER_HOST`: URL of the MQTT broker
* `MQTT_TOPIC`: MQTT topic where to publish messages
* `MQTT_MAX_INFLIGHT`: maximum amount of MQTT messages in-flight
* `MQTT_CLIENT_ID`: MQTT client ID
* `MQTT_RETAIN_MESSAGE`: whether to send MQTT messages with retained flag
* `MQTT_KEEP_ALIVE_INTERVAL`: interval for MQTT keep-alive, in seconds
