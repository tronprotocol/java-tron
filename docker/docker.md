# Docker Shell Guide

java-tron support containerized processes, we maintain a Docker image with latest version build from our master branch on DockerHub. To simplify the use of Docker and common docker commands, we also provide a shell script to help you better manage container services，this guide describes how to use the script tool.


## Prerequisites

Requires a docker to be installed on the system. Docker version >=20.10.12. 


## Quick Start

Shell can be obtained from the java-tron project or independently, you can get the script from [here](https://github.com/tronprotocol/java-tron/blob/develop/docker/docker.sh) or download via the wget:
```shell
$ wget https://raw.githubusercontent.com/tronprotocol/java-tron/develop/docker/docker.sh
```

### Pull the mirror image
Get the `tronprotocol/java-tron` image from the DockerHub, this image contains the full JDK environment and the host network configuration file, using the script for simple docker operations.
```shell
$ sh docker.sh --pull
```

### Run the service
Before running the java-tron service, make sure some ports on your local machine are open,the image has the following ports automatically exposed:
- `8090`: used by the HTTP based JSON API
- `50051`: used by the GRPC based API
- `18888`: TCP and UDP, used by the P2P protocol running the network

#### Full node on the main network

```shell
$ sh docker.sh --run --net main
```
or you can use `-p` to customize the port mapping, more custom parameters, please refer to [Options](#Options)

```shell
$ sh docker.sh --run --net main -p 8080:8090 -p 40051:50051 
```

#### Full node on the nile test network
```shell
$ sh docker.sh --run --net test
```

#### Full node on the private network
you can also build your own private-net and will download a configuration file from the network for your private network, which will be stored in your local `config` directory.
```shell
$ sh docker.sh --run --net private
```
#### Configuration
The script will automatically download and use the corresponding configuration file from the github repository according to the `--net` parameter. if you don't want to update the configuration file every time you start the service, please add a startup parameter.

```shell
$ sh docker.sh --run --update-config false
```

Or use the `-c` parameter to specify your own configuration file, which will not automatically download a new configuration file from github repository.


### View logs
If you want to see the logs of the java-tron service, please use the `--log` parameter

```shell
$ sh docker.sh --log | grep 'pushBlock'
```
### Stop the service

If you want to stop the container of java-tron, you can execute

```shell
$ sh docker.sh --stop
```

## Build Image

If you do not want to use the default official image, you can also compile your own local image, first you need to change some parameters in the shell script to specify your own mirror info.
`DOCKER_REPOSITORY` is your repository name
`DOCKER_IMAGES` is the image name
`DOCKER_TARGET` is the version number, here is an example:

```shell
DOCKER_REPOSITORY="your_repository"
DOCKER_IMAGES="java-tron"
DOCKER_TARGET="1.0"
```

then execute the build:

```shell
$ sh docker.sh --build
```

## Options

Parameters for all functions：

* **`--build`** building a local mirror image
* **`--pull`** download a docker mirror from **DockerHub**
* **`--run`** run the docker mirror
* **`--log`** exporting the java-tron run log on the container
* **`--stop`** stopping a running container
* **`--rm`** remove container,only deletes the container, not the image
* **`-p`** publish a container's port to the host, format:`-p hostPort:containerPort`
* **`-c`** specify other java-tron configuration file in the container
* **`-v`** bind mount a volume for the container,format: `-v host-src:container-dest`, the `host-src` is an absolute path
* **`--net`** select the network, you can join the main-net, test-net
* **`--update-config`** update configuration file, default true


