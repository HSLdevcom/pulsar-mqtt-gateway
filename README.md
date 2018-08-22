## Description

Application for publishing GTFS-RT messages from Transitdata Pulsar topics to an external MQTT-broker.

More system-level documentation can be found in [this project](https://gitlab.hsl.fi/transitdata/transitdata-doc).

## Building

### Dependencies

This project depends on [transitdata-common](https://gitlab.hsl.fi/transitdata/transitdata-common) project.

### Locally

- Build and install common lib to local maven repository before compiling this one.
  - ```cd transitdata-common && mvn install```  
- ```mvn compile```  
- ```mvn package```  

### Docker image

- At the moment the Docker image requires the common-lib (common.jar) to be found in /dependencies folder. Please copy it there.
   - This problem will resolve itself once we have common-lib available in a public maven repository.
- Run [this script](build-image.sh) to build the Docker image


## Running

Requirements:
- Local Pulsar Cluster
  - By default uses localhost, override host in PULSAR_HOST if needed.
    - Tip: f.ex if running inside Docker in OSX set `PULSAR_HOST=host.docker.internal` to connect to the parent machine
  - You can use [this script](https://gitlab.hsl.fi/transitdata/transitdata-doc/bin/pulsar/pulsar-up.sh) to launch it as Docker container
- Connection to an external MQTT server. configure username and password.

Launch Docker container with

```docker-compose -f compose-config-file.yml up <service-name>```   

See [the documentation-project](https://gitlab.hsl.fi/transitdata/transitdata-doc) for details
