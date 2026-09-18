# Docker Shell Guide

java-tron supports containerized processes. Official versioned release images are published on Docker Hub. The mutable `latest` tag points to the latest published release; it does not represent the current head of `master`. The `docker.sh` helper simplifies common image and container lifecycle operations.

## Prerequisites

Install Docker 20.10.12 or later before using the helper.

`docker.sh` requires Bash. On Windows, use Docker Desktop with Linux containers and run the helper from [WSL 2](https://docs.docker.com/desktop/features/wsl/) with Docker integration enabled. It cannot be executed directly from PowerShell or Command Prompt.

## Quick Start

Obtain the helper from the java-tron repository, or download it independently:

```shell
$ wget https://raw.githubusercontent.com/tronprotocol/java-tron/develop/docker/docker.sh
```

### Pull the official image

Get the `tronprotocol/java-tron` image from Docker Hub. The image contains a Java runtime environment and the mainnet configuration file. The helper pulls `tronprotocol/java-tron:latest`. For long-running or reproducible deployments, use Docker directly to select a versioned tag or an `image@sha256:...` reference. See the available [Docker Hub tags](https://hub.docker.com/r/tronprotocol/java-tron/tags).

```shell
$ bash docker.sh --pull
```

### Run the service

Before running java-tron, make sure the required ports are available on the host. By default, HTTP and gRPC APIs are bound to the host loopback interface. Mainnet P2P remains available on all host interfaces:

- `127.0.0.1:8090`: used by the HTTP-based JSON API
- `127.0.0.1:50051`: used by the gRPC-based API
- `18888`: TCP and UDP on all host interfaces, used by the P2P protocol

The helper manages one container named `tronprotocol-java-tron` and creates it with Docker's `always` restart policy. If this container already exists, `--run` exits without changing it. Use `--start` to start a stopped container, or use `--rm` before `--run` to recreate it with new settings. The helper cannot run mainnet and private-network instances simultaneously; remove the existing container before switching networks. A manually stopped container remains stopped until it is manually restarted or the Docker daemon restarts. Use Docker directly when multiple instances, a custom container name, or a different restart policy is required.

#### Full node on the main network

```shell
$ bash docker.sh --run
```

The helper does not provide an option for setting JVM heap parameters. Nodes started this way use the JVM options bundled in the image and the JVM's automatically selected heap size. For production mainnet deployments that require explicit heap sizing or other JVM tuning, use the direct `docker run` example in the [quick-start guide](../quickstart.md#run-a-mainnet-fullnode).

The mainnet configuration is bundled in the image at `/java-tron/config.conf` and comes from the same java-tron revision used to build the image. `--net main` remains available as an explicit form.

Use `-p` to customize the port mapping. Supplying any custom `-p` replaces the complete default port set, so include both TCP and UDP mappings for P2P. For more parameters, see [Options](#options).

```shell
$ bash docker.sh --run --net main \
    -p 127.0.0.1:8080:8090 \
    -p 127.0.0.1:40051:50051 \
    -p 18888:18888 \
    -p 18888:18888/udp
```

#### Single-node private network

You can also run a single-node private network with the configuration maintained by `tron-deployment`. If `config/private_net_config.conf` does not exist in the current directory, the script downloads it automatically. An existing local configuration is reused so that local changes are preserved.

```shell
$ bash docker.sh --run --net private
```

Private mode starts FullNode with `--witness` so that the genesis witness produces blocks. By default, the helper publishes the following ports used by `private_net_config.conf`:

- `127.0.0.1:16667`: used by the HTTP-based JSON API
- `127.0.0.1:50051`: used by the gRPC-based API

The private configuration also enables JSON-RPC on container port `8545` and listens for P2P on container port `16666`, but the helper publishes neither port by default. To make JSON-RPC available on the host loopback interface, provide the complete custom port set because specifying any `-p` replaces all default mappings:

```shell
$ bash docker.sh --run --net private \
    -p 127.0.0.1:16667:16667 \
    -p 127.0.0.1:50051:50051 \
    -p 127.0.0.1:8545:8545
```

The downloaded configuration contains a publicly known development witness key and genesis accounts. Use it only for isolated local development. For a multi-node, shared, or security-sensitive private network, use the maintained [`tron-docker/private_net`](https://github.com/tronprotocol/tron-docker/tree/main/private_net) setup and replace its keys and configuration as appropriate.

To connect an intentionally configured helper-based node from another machine, provide the complete custom port set and include explicit P2P mappings such as `-p <host-interface-address>:16666:16666` and `-p <host-interface-address>:16666:16666/udp`. Before exposing P2P, replace the public development credentials and configure the peers and witness roles; publishing the ports alone does not create a multi-node private network.

Existing containers keep their original port mappings when restarted. After upgrading from a helper version that published private P2P by default, run `bash docker.sh --rm` and then create the private node again with `bash docker.sh --run --net private`.

To replace an existing local copy with the latest maintained configuration, explicitly request an update. This overwrites `config/private_net_config.conf`.

```shell
$ bash docker.sh --run --net private --update-config true
```

#### Configuration

Mainnet uses the configuration bundled in the image and never downloads another configuration. The `private` network option uses `config/private_net_config.conf` from the current directory, downloading it from `tron-deployment` only when it is missing or an update is explicitly requested. It also enables witness mode so that the single-node network can produce blocks.

Nile is intentionally not supported by this script because it may require features that are not yet available on the mainnet source revision. Follow the Nile-specific build instructions in the project README instead.

Alternatively, mount a configuration into the container and select it with `-c`:

```shell
$ bash docker.sh --run \
    -v /absolute/path/custom.conf:/java-tron/custom.conf:ro \
    -c /java-tron/custom.conf
```

### Data and log persistence

By default, the helper bind-mounts `output-directory` from the directory where `docker.sh` is executed to `/java-tron/output-directory` in the container. The blockchain database therefore remains on the host after the container is removed. Make sure that the current filesystem has sufficient space, or mount a dedicated data directory:

```shell
$ mkdir -p "$PWD/mainnet-data"
$ bash docker.sh --run --net main \
    -v "$PWD/mainnet-data:/java-tron/output-directory"
```

Do not reuse one database directory across different networks. Use separate directories for mainnet and private-network data.

Application logs are not persisted by default; they remain in the container writable layer and are deleted with the container. To retain logs after `--rm`, mount a host directory explicitly:

```shell
$ mkdir -p "$PWD/logs"
$ bash docker.sh --run --net main \
    -v "$PWD/logs:/java-tron/logs"
```

Adding a log or configuration volume does not disable the default database mount. The default is replaced only when a custom volume targets `/java-tron/output-directory`.

### View logs

Use `--log` to follow the java-tron service log:

```shell
$ bash docker.sh --log | grep 'PushBlock'
```

### Stop the service

Use `--stop` to stop the java-tron container:

```shell
$ bash docker.sh --stop
```

## Build Image

The Dockerfiles clone the remote java-tron repository and check out `master` at build time. They do not build the Java sources in the current checkout. The resulting image can differ from the published Docker Hub `latest` image.

The helper uses `tronprotocol/java-tron:latest` for `--pull`, `--build`, and `--run` and does not support selecting another image reference through command-line options or environment variables. After `--build`, the local `tronprotocol/java-tron:latest` tag points to the newly built `master` image, so subsequent `--run` commands use that build. Use Docker directly when a separate tag, digest, or image name is required.

Then build the image:

```shell
$ bash docker.sh --build
```

The script detects the Docker daemon architecture by default. You can also select the target architecture explicitly:

```shell
$ bash docker.sh --build amd64
$ bash docker.sh --build arm64
```

Building for an architecture different from the Docker daemon requires a builder with the corresponding emulation support. Docker Desktop provides this by default; standalone Docker Engine installations may require QEMU/binfmt configuration.

Docker may reuse cached layers, so rebuilding does not necessarily fetch the latest remote `master`. To fetch the source again, run one of the following commands from the java-tron repository root. The helper does not accept `--no-cache`; use Docker directly:

```shell
# amd64
docker build --no-cache --platform linux/amd64 \
  -f docker/Dockerfile -t tronprotocol/java-tron:latest docker

# arm64
docker build --no-cache --platform linux/arm64 \
  -f docker/arm64/Dockerfile -t tronprotocol/java-tron:latest docker
```

These commands rerun the build steps without deleting existing build caches.

When the script is used from a java-tron checkout, only the Dockerfile and build context are resolved relative to `docker.sh`, regardless of the current working directory. The current checkout's Java sources are not added to that context. If only `docker.sh` was downloaded, the required architecture-specific Dockerfile is downloaded into a temporary build context and removed after the build. Both paths build the remote `master` branch.

## Options

### Commands

- **`--build [amd64|arm64]`**: build `tronprotocol/java-tron:latest` from the remote `master` branch, optionally for the specified architecture
- **`--pull`**: download `tronprotocol/java-tron:latest` from Docker Hub
- **`--run`**: run `tronprotocol/java-tron:latest`
- **`--start`**: start the existing java-tron container
- **`--log`**: follow the java-tron log in the container
- **`--stop`**: stop the running container
- **`--rm`**: remove the container without removing the image

### Run options

The following options apply only to `--run`:

- **`-p`**: publish a container port using `-p hostPort:containerPort[/protocol]`; custom mappings replace all defaults
- **`-c`**: specify another java-tron configuration file in the container
- **`-v`**: bind mount a volume using `-v host-src:container-dest`; `host-src` must be an absolute path
- **`--net`**: select `main` or `private`; a missing private configuration is downloaded automatically
- **`--update-config`**: set to `true` with `--net private` to replace the local private configuration
