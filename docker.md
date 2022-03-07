# Docker Shell Guide

java-tron support containerized processes, we maintain a Docker image with latest version build from our master branch on DockerHub. To simplify the use of Docker and common docker commands, we also provide a shell script to help you better manage container services，this guide describes how to use the script tool.


## Prerequisites

Requires docker to be installed on the system. Docker version >=20.10.12. 
  

## Quick Start

Shell can be obtained from the java-tron project or independently, you can get the script from [here](https://github.com/tronprotocol/java-tron/docker.sh) or download via the wget:
  ```shell
 $ wget https://raw.githubusercontent.com/tronprotocol/java-tron/develop/docker.sh
  ```

### Pull the mirror image
Get the `tronprotocol/java-tron` image from the DockerHub, this image contains the full JDK environment and the host network configuration file, using the script for simple docker operations.
```shell
$ sh docker.sh --pull
```

### Running the service
Before running the java-tron service, make sure the necessary ports are opened：
- `8090`: provides `HTTP` interface
- `50051`: provides `RPC` interface
- `18888`: P2P service listening interface

then start the java-tron service with the `--run` parameter
```shell
$ sh docker.sh --run
```

Custom parameters can be specified
1. **-p**: custom port mapping
2. **-c**: custom configuration files
3. **-v**: custom mount directory

**Example**

```shell
sh docker.sh --run -v /mydata:/containerdata -p 8080:8090 -p 40051:50051 -c /data/config.conf
```

If you want to see the logs of the java-tron service, please use the `--log` parameter

```shell
$ sh docker.sh --log | grep 'pushBlock'
```

If you want to stop the container of java-tron, you can execute

```shell
$ sh docker.sh --stop
```

## Build Image

If you do not want to use the default official image, you can also compile your own local image, first you need to change some parameters in the shell script to specify your own mirror info，`DOCKER_REPOSITORY` is your repository name, `DOCKER_IMAGES` is the image name,`DOCKER_TARGET` is the version number, here is an example：
```shell
DOCKER_REPOSITORY="you_repository"
DOCKER_IMAGES="java-tron"
DOCKER_TARGET="1.0"
```

then execute the build:

```shell
$ sh docker.sh --build
```

## Options

Parameters for all functions：

* **`--build`** Building a local mirror image

* **`--pull`**  download a docker mirror from **DockerHub**

* **`--run`**  run the docker mirror

* **`--log`**  exporting the java-tron run log on the container

* **`--stop`**  stopping a running container
  
* **`--rm`** remove container,only deletes the container, not the image, the `config` and `output-directory` directories.
