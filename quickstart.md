# java-tron Quick Start

Choose the workflow that matches your goal. Production node operation, local smart-contract testing, and private-network deployment have different security and resource requirements.

| Goal | Recommended entry point |
| --- | --- |
| Build or run java-tron directly | [java-tron README](README.md) |
| Run a single FullNode with the Bash helper | [java-tron Docker shell guide](docker/docker.md) |
| Build a development image from the current checkout | [java-tron Docker shell guide](docker/docker.md#build-an-image) |
| Run a FullNode with Docker Compose | [tron-docker single-node guide](https://github.com/tronprotocol/tron-docker/tree/main/single_node) |
| Build and test release images for amd64 and arm64 | [tron-docker image guide](https://github.com/tronprotocol/tron-docker/tree/main/tools/docker) |
| Create a multi-node private network | [tron-docker private-network guide](https://github.com/tronprotocol/tron-docker/tree/main/private_net) |
| Test smart contracts locally | [TronBox Runtime Environment](https://hub.docker.com/r/tronbox/tre) |

## Prerequisites

Install Docker and the Docker Compose plugin from the official documentation:

- [Docker Engine on Linux](https://docs.docker.com/engine/install/)
- [Docker Desktop on macOS](https://docs.docker.com/desktop/setup/install/mac-install/)

A Mainnet FullNode requires at least 8 CPU cores and 16 GB of memory. Stable and production deployments require additional memory, SSD capacity, and network bandwidth. See the [Mainnet hardware requirements](README.md#hardware-requirements-for-mainnet) before deploying a node.

## Run a FullNode with Docker Compose

java-tron provides two independently maintained Docker workflows:

- Use [`docker/docker.sh`](docker/docker.md) for lightweight image build, pull, and single-node container lifecycle operations.
- Use [`tron-docker`](https://github.com/tronprotocol/tron-docker) for Docker Compose deployments, private networks, and dedicated image build and test tooling.

The workflows have separate commands, configuration, and defaults and are not interchangeable. The example below uses the `tron-docker` single-node Compose workflow.

> **CPU architecture:** Use a current checkout of [`tron-docker`](https://github.com/tronprotocol/tron-docker/tree/main/single_node) before starting the Compose example. Older copies of `docker-compose-quick-start.yml` included the JDK 8-only `-XX:+UseConcMarkSweepGC` option; the ARM64/aarch64 image uses JDK 17 and rejects that option. If it is present in your local Compose file, update the checkout or remove the option before starting the node. The current upstream quick-start file no longer includes this legacy collector option.

> **Storage and synchronization:** The default Compose file synchronizes a full Mainnet database from genesis. It does not configure a Lite FullNode or preload a data snapshot. Allocate approximately 3.5–4 TB of high-performance SSD storage for this mode. The approximately 200 GB storage tier applies only when using Lite FullNode data. To avoid synchronizing from genesis, configure a compatible [FullNode or Lite FullNode data snapshot](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#data-snapshot) for the selected network and java-tron version before starting.
>
> The default Compose file also does not mount the database on the host, so synchronized data remains in the container's writable layer and can be lost when the container is removed or recreated. Configure a persistent volume for `output-directory` before using this example for a long-running node.

```shell
git clone https://github.com/tronprotocol/tron-docker.git
cd tron-docker/single_node
docker compose -f docker-compose-quick-start.yml up -d
```

Check the synchronization log:

```shell
docker exec tron-node tail -f ./logs/tron.log
```

Check the HTTP API:

```shell
curl --request POST http://127.0.0.1:8090/wallet/getnowblock
```

The quick-start Compose file is intended for evaluation. Before operating a long-running or production node:

- Pin a released image tag instead of relying on `latest`.
- Persist the configuration, logs, and `output-directory` on the host.
- Set the container and JVM memory limits for the selected deployment tier.
- Publish both TCP and UDP for the P2P port.
- Restrict HTTP and gRPC access with host bindings, firewall rules, or a trusted proxy.
- Use a compatible configuration file and data snapshot for the selected network and java-tron version.

For production deployment, snapshot synchronization, JVM tuning, and upgrade procedures, follow the [java-tron deployment guide](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/).

## Build java-tron from source

Source builds require `unzip` and the JDK matching the CPU architecture: JDK 8 on x86_64/amd64 and JDK 17 on ARM64/aarch64. Follow [Building the Source Code](README.md#building-the-source-code) to build the current checkout.

To build a development image from the current working tree, run `bash docker/docker.sh --build --source local` from the repository root. This builds the distribution on the host and sends only a temporary distribution-only context to Docker. The legacy `--build` command without source options continues to build the remote `master` branch. See the [Docker shell guide](docker/docker.md#build-an-image) for source-selection options.

For release-oriented amd64 and arm64 image build and test tooling, use the Gradle Docker tooling in `tron-docker`.

## Run a Super Representative node

Do not use a shortened Quick Start command for a production Super Representative node. An SR requires additional hardware, JVM tuning, key protection, monitoring, backup, and upgrade planning.

Follow the [Starting a Block Production Node](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#starting-a-block-production-node) guide. Use an encrypted keystore and the production deployment's secret-management mechanism. Do not pass `--private-key` or `--password`: command arguments may be visible in process listings and, with Docker, are retained in container metadata.

## Create a private development network

For a multi-node private TRON network, use the [official private-network guide](https://tronprotocol.github.io/documentation-en/using_javatron/private_network/) or the [tron-docker private-network example](https://github.com/tronprotocol/tron-docker/tree/main/private_net).

For local smart-contract development and automated tests, run the TronBox Runtime Environment:

```shell
docker run --rm --name tron -it -p 127.0.0.1:9090:9090 tronbox/tre:dev
```

TRE includes funded test accounts and privileged development APIs. Use it only for local development or isolated CI, and never expose it to an untrusted network or use it with production keys or funds.
