# pulsar-mqtt-gateway [![Test and create Docker image](https://github.com/HSLdevcom/pulsar-mqtt-gateway/actions/workflows/test-and-build.yml/badge.svg)](https://github.com/HSLdevcom/pulsar-mqtt-gateway/actions/workflows/test-and-build.yml)

## Description

Application for publishing Pulsar messages to MQTT. The application doesn't care about 
the data content, it only publishes the binary message as-is.

## Building

### Dependencies

This project depends on [transitdata-common](https://github.com/HSLdevcom/transitdata-common) project.

Either use released versions from the public GitHub Packages repository (Maven) or build your own and install to local Maven repository:
- `cd transitdata-common && mvn install`

### Locally

- `mvn compile`
- `mvn package`  

### Docker image

- Run [this script](build-image.sh) to build the Docker image


## Running

### Dependencies

* Pulsar
* Connection to a MQTT broker

### Environment variables

* `MQTT_HAS_AUTHENTICATION`: whether the MQTT broker uses authentication
* `FILEPATH_USERNAME_SECRET`: path to the file containing the username, default is `/run/secrets/mqtt_broker_username`
* `FILEPATH_PASSWORD_SECRET`: path to the file containing the password, default is `/run/secrets/mqtt_broker_password`
* `MQTT_BROKER_HOST`: URL of the MQTT broker
* `MQTT_TOPIC`: MQTT topic where to publish messages
* `MQTT_MAX_INFLIGHT`: maximum amount of MQTT messages in-flight
* `MQTT_CLIENT_ID`: MQTT client ID
* `MQTT_RETAIN_MESSAGE`: whether to send MQTT messages with retained flag
* `MQTT_KEEP_ALIVE_INTERVAL`: interval for MQTT keep-alive, in seconds
