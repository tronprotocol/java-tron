# Docker Shell Guide

java-tron provides docker images to build projects quickly. To simplify the use of Docker, common docker commands are simplified. We provide a shell script to use. Please make sure you have docker installed on your computer before using it.



## Features

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

**Detail log**

```
07:32:01.732 INFO  [main] [app](FullNode.java:53) Full node running.
07:32:02.830 WARN  [main] [app](LocalWitnesses.java:104) privateKey is null
07:32:03.562 INFO  [main] [app](Args.java:1029) Bind address wasn't set, Punching to identify it...
07:32:03.745 INFO  [main] [app](Args.java:1032) UDP local bound to: 172.17.0.2
07:32:03.748 INFO  [main] [app](Args.java:1047) External IP wasn't set, using checkip.amazonaws.com to identify it...
07:32:04.446 INFO  [main] [app](Args.java:1061) External address identified: 203.12.203.3
07:32:04.477 INFO  [main] [app](Args.java:1144)

07:32:04.477 INFO  [main] [app](Args.java:1145) ************************ Net config ************************
07:32:04.477 INFO  [main] [app](Args.java:1146) P2P version: 11111
07:32:04.477 INFO  [main] [app](Args.java:1147) Bind IP: 172.17.0.2
07:32:04.477 INFO  [main] [app](Args.java:1148) External IP: 203.12.203.3
07:32:04.478 INFO  [main] [app](Args.java:1149) Listen port: 18888
07:32:04.478 INFO  [main] [app](Args.java:1150) Discover enable: true
07:32:04.479 INFO  [main] [app](Args.java:1151) Active node size: 0
07:32:04.479 INFO  [main] [app](Args.java:1152) Passive node size: 0
07:32:04.480 INFO  [main] [app](Args.java:1153) FastForward node size: 2
07:32:04.480 INFO  [main] [app](Args.java:1154) Seed node size: 29
07:32:04.480 INFO  [main] [app](Args.java:1155) Max connection: 30
07:32:04.480 INFO  [main] [app](Args.java:1156) Max connection with same IP: 2
07:32:04.480 INFO  [main] [app](Args.java:1157) Solidity threads: 4
```

If you need to see key information, you can filter directly using grep:

```shell
sh docker.sh --log | grep 'pushBlock'
```

When you need to stop the container of java-tron, you can execute: 

```sh
sh docker.sh --stop
```



## Options

* **--pull**  

  download a docker mirror from **DockerHub**

* **--run**  

  run the **tronprotocol/java-tron** docker mirror

* **--log**  

  exporting the java-tron run log on the container

* **--stop**  
  
  stopping a running container
  
* **--rm**

  remove mirror image

