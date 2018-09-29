#!/bin/bash

eval $(docker-machine env default)

#convert windows path into unix path.
# C: drive is converted to /c/
CERT=${DOCKER_CERT_PATH//\\/\/}
CERT=${CERT/C:/\/c}

#update the images, in particular the client
docker pull smduarte/sd17-services:latest


#execute the client with the given command line parameters
docker run -e DOCKER_TLS_VERIFY -e DOCKER_HOST -e DOCKER_CERT_PATH=$CERT  -v $CERT:$CERT smduarte/sd17-services:latest