# How to quick start

## Introduction

This guide covers three common ways to get started with TRON:

- Run a mainnet FullNode with the official java-tron Docker image.
- Start an isolated local development chain with [TRON Runtime Environment (TRE)](https://hub.docker.com/r/tronbox/tre).
- Deploy a multi-node private network with the official [tron-docker](https://github.com/tronprotocol/tron-docker/tree/main/private_net) configuration.

## Dependencies

Install the latest Docker release for your platform:

- [macOS](https://docs.docker.com/desktop/setup/install/mac-install/)
- [Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
- [Linux](https://docs.docker.com/engine/install/)

All commands in this guide use POSIX shell syntax. On Windows, use Docker Desktop with Linux containers and run the commands from [WSL 2](https://docs.docker.com/desktop/features/wsl/) with Docker integration enabled. The examples are not intended for native PowerShell or Command Prompt.

## Run a mainnet FullNode

Pull the official image from Docker Hub:

```shell
docker pull tronprotocol/java-tron:latest
```

Create host directories for the blockchain database and application logs:

```shell
mkdir -p output-directory logs
```

Set JVM memory options for the architecture used by the Docker image. Run one of the following commands:

```shell
# amd64 / JDK 8
JAVA_TRON_JVM_OPTIONS="-Xms9G -Xmx12G -XX:MaxDirectMemorySize=1G"
```

```shell
# ARM64 / JDK 17
JAVA_TRON_JVM_OPTIONS="-Xmx9G -XX:MaxDirectMemorySize=1G"
```

These baseline values follow the official guidance for a host with 16 GB of memory. For hosts with 32 GB or more, size the heap using the official [JVM tuning guide][jvm-guide] and leave sufficient memory for direct buffers, native allocations, the operating system, and the database page cache.

Start the FullNode with the mainnet configuration bundled in the image:

```shell
docker run -d \
  --name java-tron \
  --restart unless-stopped \
  -v "$(pwd)/output-directory:/java-tron/output-directory" \
  -v "$(pwd)/logs:/java-tron/logs" \
  -p 127.0.0.1:8090:8090 \
  -p 127.0.0.1:50051:50051 \
  -p 18888:18888 \
  -p 18888:18888/udp \
  tronprotocol/java-tron:latest \
  -jvm "{$JAVA_TRON_JVM_OPTIONS}" \
  -c /java-tron/config.conf
```

The HTTP and gRPC APIs are bound to localhost by default, while the TCP and UDP P2P ports are available to the network. Change the API bindings only when remote access is required, and protect them with appropriate network controls. Pin a versioned image tag or digest for long-running or reproducible deployments. The image also loads architecture-specific GC options from `bin/java-tron.vmoptions`; do not copy JDK 8 GC options to an ARM64/JDK 17 deployment.

View the FullNode log:

```shell
docker exec java-tron tail -100f /java-tron/logs/tron.log
```

Stop the container:

```shell
docker stop java-tron
```

Restart the stopped container:

```shell
docker start java-tron
```

To recreate the container with a different image or configuration, remove the stopped container first:

```shell
docker rm java-tron
```

The bind-mounted database and logs remain on the host after the container is removed.

The optional `docker.sh` helper provides shorter commands for image builds, private network configuration, common port mappings, and lifecycle operations. See the [Docker Shell Guide](docker/docker.md) for details.

### Mainnet and SR requirements

A Mainnet FullNode requires production-grade CPU, memory, SSD capacity, and network bandwidth. The current official deployment requirements are:

| Deployment | CPU | Memory | High-performance SSD | Network bandwidth |
| --- | ---: | ---: | ---: | ---: |
| Minimum FullNode | 8 cores | 16 GB | 3 TB | 100 Mbps |
| Recommended FullNode | 16 cores | 32 GB | 3.5 TB or more | 100 Mbps |
| Block-producing SR | 32 cores | 64 GB | 3.5 TB or more | 100 Mbps |

The example above stores the database under `$(pwd)/output-directory`. Before starting it, ensure that the current filesystem has sufficient high-performance SSD capacity, or replace the host side of the volume mapping with a dedicated data disk.

A new node otherwise synchronizes the full chain; use a compatible [data snapshot][snapshot-guide] to reduce the initial synchronization time. [Lite FullNode][lite-guide] deployments have different storage requirements and require the corresponding Lite data and configuration.

Review the official [java-tron deployment guide][deployment-guide] and [JVM tuning guide][jvm-guide] before choosing JVM values, storage layout, snapshots, monitoring, and upgrade procedures.

Do not convert the quick-start container into a production Super Representative merely by adding `--witness`. An SR requires stronger hardware, protected block-signing keys, an SR-specific configuration, monitoring, backup, and operational failover. Follow the official [block-production deployment guide][block-production-guide] and review the [Super Representative requirements][sr-guide] before enabling block production.

[deployment-guide]: https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/
[jvm-guide]: https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#jvm-parameter-optimization-for-mainnet-fullnode-deployment
[snapshot-guide]: https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#speeding-up-node-data-synchronization
[lite-guide]: https://tronprotocol.github.io/documentation-en/using_javatron/litefullnode/
[block-production-guide]: https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#starting-a-block-production-node
[sr-guide]: https://tronprotocol.github.io/documentation-en/mechanism-algorithm/sr/

## Start a local development chain with TRE

[TRE](https://hub.docker.com/r/tronbox/tre) is the maintained successor for local smart-contract and DApp development. It provides a single-container development chain with funded test accounts, automatic block production, and commonly used HTTP and event APIs on port `9090`.

Pull and run the current stable image:

```shell
docker pull tronbox/tre
docker run --rm \
  --name tron \
  -p 127.0.0.1:9090:9090 \
  -e useDefaultPrivateKey=true \
  tronbox/tre
```

The container runs in the foreground. In another terminal, check that the HTTP service is running:

```shell
curl -fsS http://127.0.0.1:9090/healthcheck
```

Account generation and funding continue asynchronously after the HTTP service starts. Before running tests that depend on funded accounts, wait for `/admin/accounts` to report them as available:

```shell
until curl -fsS http://127.0.0.1:9090/admin/accounts | grep -q 'Available Accounts'; do
  sleep 1
done
```

The default image tag follows the current stable release. Pin a versioned tag or image digest in CI when reproducible builds are required. See the [TronBox documentation](https://tronbox.io/docs/quickstart) for contract development and deployment workflows.

> **Warning:** TRE is intended only for isolated development and testing. The default private key, funded accounts, and administrative APIs are not secure. Keep port `9090` bound to localhost and never expose this environment to production or an untrusted network.

## Deploy a multi-node private network

For multi-node private network deployment, follow the official [tron-docker private network guide](https://github.com/tronprotocol/tron-docker/tree/main/private_net).
