#!/bin/bash
if [  $# -le 1 ] 
then 
		echo "usage: $0 -image <img> [ -test <num> ] [ -log OFF|ALL|FINE ] [-sleep secods]"
		exit 1
fi 

eval $(docker-machine env default)

#convert windows path into unix path.
# C: drive is converted to /c/
CERT=${DOCKER_CERT_PATH//\\/\/}
CERT=${CERT/C:/\/c}

#update the images, in particular the client
docker pull smduarte/sd17-client2:latest

#execute the client with the given command line parameters
docker run --network=sd-net -it -e DOCKER_TLS_VERIFY -e DOCKER_HOST -e DOCKER_CERT_PATH=$CERT  -v $CERT:$CERT smduarte/sd17-client2:latest $*
