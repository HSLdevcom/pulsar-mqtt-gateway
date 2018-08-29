## Description

Application for publishing Pulsar messages to MQTT. The application doesn't care about 
the data content, it only publishes the binary message as-is.

## Building

### Dependencies

This project depends on [transitdata-common](https://gitlab.hsl.fi/transitdata/transitdata-common) project.

Either use released versions from public maven repository or build your own and install to local maven repository:
  - ```cd transitdata-common && mvn install```  

### Locally

- ```mvn compile```  
- ```mvn package```  

### Docker image

- At the moment the Docker image requires the common-lib (common.jar) to be found in /dependencies folder. Please copy it there.
   - This problem will resolve itself once we have common-lib available in a public maven repository.
- Run [this script](build-image.sh) to build the Docker image


## Running

Requirements:
- Pulsar Cluster
  - By default uses localhost, override host in PULSAR_HOST if needed.
    - Tip: f.ex if running inside Docker in OSX set `PULSAR_HOST=host.docker.internal` to connect to the parent machine
  - You can use [this script](https://gitlab.hsl.fi/transitdata/transitdata-doc/bin/pulsar/pulsar-up.sh) to launch it as Docker container
- Connection to an external MQTT server. configure username and password via files
  - Set filepath for username via env variable FILEPATH_USERNAME_SECRET, default is `/run/secrets/mqtt_broker_username` 
  - Set filepath for password via env variable FILEPATH_PASSWORD_SECRET, default is `/run/secrets/mqtt_broker_password` 

All other configuration options are configured in the [config file](src/main/resources/environment.conf) 
which can also be configured externally via env variable CONFIG_PATH

Launch Docker container with

```docker-compose -f compose-config-file.yml up <service-name>```   

