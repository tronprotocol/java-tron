# Docker Shell Guide

java-tron provides docker images to build projects quickly. To simplify the use of Docker, common docker commands are simplified. We provide a shell script to use. Please make sure you have docker installed on your computer before using it.



## Features
* Building the mirror image
* Get docker mirrors
* Start Service
* Stop the service
* View Logs
* Delete containers



## Prerequisites

Requires docker to be installed on the system. Docker version >=20.10.12.



## Getting

Shell can be obtained from the java-tron project or independently.

You can get the script from [here](https://github.com/tronprotocol/java-tron/docker.sh),Or download via the tool:

* wget

  ```shell
  wget https://raw.githubusercontent.com/tronprotocol/java-tron/develop/docker.sh
  ```

* curl

  ```shell
  curl -LO https://raw.githubusercontent.com/tronprotocol/java-tron/develop/docker.sh
  ```

* git clone

  ```shell
  git clone https://github.com/tronprotocol/java-tron.git
  cd java-tron
  ```
  
  

## Quick Start

This guide will help you to quickly build **java-tron** services using docker.
Get the script here and pull the mirror image and run the service.

```shell
git clone https://github.com/tronprotocol/java-tron.git
cd java-tron
sh docker.sh --pull
sh docker.sh --run
docker ps 
```

Output run log,you can see the log output from java-tron.

```shell
sh docker.sh --log
```

If you need to see key information, you can filter directly using grep:

```shell
sh docker.sh --log | grep 'pushBlock'
```

When you need to stop the container of java-tron, you can execute: 

```shell
sh docker.sh --stop
```

## Build Image

The default mirror name for constructing a mirror image has the following parameters: 

```shell
...
DOCKER_REPOSITORY="tronprotocol"
DOCKER_IMAGES="java-tron"
# latest or version
DOCKER_TARGET="latest"
...
```

You can change the parameters to your own mirror name:

```shell
...
DOCKER_REPOSITORY="you_repository"
DOCKER_IMAGES="java-tron"
# latest or version
DOCKER_TARGET="1.0"
...
```

Execute the build

```shell
sh docker.sh --build
```

## Pull Image

Get the `tronprotocol/java-tron` image from the docker hub, this image contains the full JDK environment and the host network configuration file, using the script for simple docker operations.

```shell
sh docker.sh --pull
```

## Run

Three necessary ports need to be open for java-tron to start.
1. HTTP: 8090
2. RPC: 50051
3. LISTEN: 18888

When started with `docker.sh`, the `config` and `database` directories will be mounted in the
directory to the directory where host executes `docker.sh`

```shell
sh docker.sh --run
```

## Options

Parameters for all functions：

* **`--build`** Building a local mirror image

* **`--pull`**  download a docker mirror from **DockerHub**

* **`--run`**  run the **tronprotocol/java-tron** docker mirror

* **`--log`**  exporting the java-tron run log on the container

* **`--stop`**  stopping a running container
  
* **`--rm`** remove mirror image
