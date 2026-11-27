#!/bin/bash
docker build --secret id=github_token,env=GITHUB_TOKEN -t hsldevcom/pulsar-mqtt-gateway .
