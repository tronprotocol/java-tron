# Quick Start

> Note: To run docker you need start docker service.

**Build a local docker image**

```shell
> cd java-tron/docker
> docker build -t tron-test .
```

**Run built image（refer to the home page）**

```shell
> docker run -it tron-test
> ./gradlew run -Pserver=true
```