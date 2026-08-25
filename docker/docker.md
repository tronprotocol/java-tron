# Docker Shell Guide

This guide covers the `docker.sh` workflow maintained in the java-tron repository. The Bash helper can build an image locally, pull the `tronprotocol/java-tron` image from Docker Hub, and operate a single FullNode container.

For Docker Compose deployments, multi-node private networks, and dedicated image build and test tooling, use the [tron-docker repository](https://github.com/tronprotocol/tron-docker). The two workflows are maintained independently; their commands, configuration, and defaults are not interchangeable.

## Prerequisites

- Docker Engine 23.0 or later, with BuildKit and the Buildx plugin available
- Bash
- `curl` or `wget` when configuration files or Dockerfiles need to be downloaded
- For `--source local` only: `unzip` and the architecture-specific JDK used by java-tron (JDK 8 on x86_64/amd64 or JDK 17 on arm64/aarch64)

Do not invoke the script with `sh`. The script uses Bash-specific syntax.

## Quick start

Use `docker/docker.sh` from a java-tron checkout, or download it separately:

```shell
wget https://raw.githubusercontent.com/tronprotocol/java-tron/master/docker/docker.sh
```

The standalone download follows the stable `master` workflow. To test changes from `develop` or an uncommitted working tree, use `docker/docker.sh` from the corresponding java-tron checkout instead. All examples below assume that `docker.sh` is in the current directory.

### Build the default image

The image contains the java-tron distribution and a Java runtime. It also bakes in `/java-tron/config.conf` from the same source checkout used to build the distribution: the selected remote ref for remote builds, or the local checkout for local builds. `docker.sh --run` passes that file with `-c` for Mainnet. A plain `docker run` without `-c` reads the same file directly.

The helper intentionally does not select the mutable `tronprotocol/java-tron:latest` tag. The image published under that tag when this fixed non-root runtime contract was introduced predates the contract and is incompatible. Build a compatible image from the remote java-tron `master` branch before the first default run:

```shell
bash docker.sh --build
```

This produces `tronprotocol/java-tron:local`. To use a compatible image from a registry instead, select it explicitly for both pull and run; prefer an immutable version tag or digest for production:

```shell
bash docker.sh --pull --image registry.example.com/java-tron:VERSION
bash docker.sh --run --image registry.example.com/java-tron:VERSION --net main
```

### Run a FullNode

`docker.sh` publishes the following ports by default:

- `127.0.0.1:8090:8090/tcp`: HTTP JSON API, accessible only from the Docker host
- `127.0.0.1:50051:50051/tcp`: gRPC API, accessible only from the Docker host
- `18888:18888/tcp` and `18888:18888/udp`: P2P communication, accessible through the host network interfaces

HTTP and gRPC are bound to loopback by default to prevent accidental network or public exposure. P2P remains externally reachable so that the node can communicate with peers.

> **Storage and synchronization:** The default Mainnet configuration starts a full FullNode. It does not enable Lite FullNode mode or preload a data snapshot. When `output-directory` under the selected data directory is empty, the node synchronizes the complete Mainnet database from genesis; allocate approximately 3.5–4 TB of high-performance SSD storage for this mode. The script persists this database on the host, but the host filesystem must still have sufficient capacity. To reduce initial synchronization time or use the Lite FullNode storage tier, import and configure a compatible [FullNode or Lite FullNode data snapshot](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#data-snapshot) for the selected network and java-tron version before starting the node.

Run a Mainnet FullNode:

```shell
bash docker.sh --run --net main
```

The helper manages one container named `tronprotocol-java-tron` by default. Use `--container-name` to choose another name; `--start`, `--stop`, `--log`, and `--rm` accept the same option. Running `--run` again while that container exists returns an error; use `--start` to reuse a stopped container, or `--rm` before creating a replacement. If the default local image does not exist, `--run` exits with an error instead of implicitly pulling or building; run `--build` first, or select a compatible published image explicitly with `--image`.

Use repeatable `-p` options to change individual mappings. Defaults are retained for container ports and protocols that are not explicitly mapped, so the following command changes only the HTTP and gRPC host ports:

```shell
bash docker.sh --run --net main \
    -p 127.0.0.1:8080:8090 \
    -p 127.0.0.1:40051:50051
```

To provide a network-accessible API, explicitly replace the relevant loopback mapping. For example, the following publishes HTTP on all IPv4 interfaces:

```shell
bash docker.sh --run --net main -p 0.0.0.0:8090:8090
```

Only expose HTTP or gRPC after restricting access with a firewall, trusted reverse proxy, or equivalent network controls.

### Nile Testnet nodes

Since Nile Testnet may incorporate features not yet available on the Mainnet, it may require code that is not included in this java-tron checkout or its images. The `docker.sh` helper in this repository therefore does not provide a Nile network mode.

For a Nile Docker deployment, follow the [tron-docker](https://github.com/tronprotocol/tron-docker) instructions and select the image appropriate for the current Nile release.

Run a private-network FullNode:

```shell
bash docker.sh --run --net private
```

## Configuration

The script selects network configuration as follows:

- `main`: uses `/java-tron/config.conf` directly from the selected image, so no host-side Mainnet configuration is created
- `private`: tron-deployment `master` [`private_net_config.conf`](https://github.com/tronprotocol/tron-deployment/blob/master/private_net_config.conf)

The private-network configuration is stored in the host data directory. An existing local copy is retained by default; a missing or empty file is downloaded from its maintained source.

The private-network template can change independently of a previously built image. Verify that a downloaded configuration is compatible with the image version before using it.

Use `--update-config true` to explicitly refresh the private-network local copy before creating the container. Mainnet always uses the configuration in the selected image, so this option has no effect with `--net main`:

```shell
bash docker.sh --run --net private --update-config true
```

Use `-c` to select a custom configuration file. The value must be a path inside the container, so mount the host file with `-v`:

```shell
bash docker.sh --run --net main \
    -v /absolute/path/custom.conf:/java-tron/custom.conf:ro \
    -c /java-tron/custom.conf
```

## FullNode arguments

Use `--` to end `docker.sh` option parsing and pass remaining arguments through to FullNode. `docker.sh` keeps those argument boundaries when it calls `docker run`, and the generated `bin/FullNode` script forwards each one to Java without word-splitting, including values that contain spaces. For example, to explicitly keep P2P enabled:

```shell
bash docker.sh --run --net main -- --p2p-disable false
```

Options before `--` configure the Docker helper; options after it become `./bin/FullNode` arguments. Use the helper's `-c` option for the configuration path instead of passing a second `-c` after `--`. The start script still consumes a `-jvm '{...}'` pair before invoking Java; every other FullNode argument is forwarded intact.

Witness options such as `-w` and `--witness-address` can also be passed after `--`. Do not pass `--private-key` or `--password`: command arguments may be visible in process listings and are retained in Docker container metadata. This helper does not by itself provide the secret delivery, key protection, monitoring, backup, and upgrade procedures required for a production Super Representative deployment. Follow the [Starting a Block Production Node](https://tronprotocol.github.io/documentation-en/using_javatron/installing_javatron/#starting-a-block-production-node) guide, use an encrypted keystore, and provide its password through the production deployment's secret-management mechanism.

By default, the script mounts persistent runtime directories relative to the shell's current working directory when `docker.sh` is invoked, not relative to the script file. Mainnet uses the configuration baked into the image and mounts only:

```text
./output-directory -> /java-tron/output-directory
./logs             -> /java-tron/logs
```

Private mode mounts the same two runtime directories and additionally mounts:

```text
./config -> /java-tron/config (read-only)
```

The private-network configuration mount is read-only because FullNode only needs to read it. This prevents the container from persisting configuration changes onto the host. By default, Mainnet does not create or mount a host `config` directory. Additional `-v` options retain the defaults for the selected mode. A custom mount replaces a default only when it uses the same container destination; callers that explicitly replace the configuration mount control its access mode.

The image runs FullNode as the non-root `tron` account with fixed UID and GID `10001:10001`. Application files under `/java-tron` (`bin/`, `lib/`, `java-tron.vmoptions`, and the baked-in `config.conf`) stay root-owned and are not writable by that user. Only `/java-tron/output-directory` and `/java-tron/logs` belong to `10001:10001`; their built-in modes are `0700`. The Docker image sets `umask 077` before FullNode starts, so newly created runtime files and directories default to `0600` and `0700`. The image enables heap dumps on out-of-memory errors and points JVM GC logs, heap dumps, and `hs_err` files at `/java-tron/logs`; the packaged launcher and `java-tron.vmoptions` used outside Docker are unchanged. An OOM heap dump can approach the configured maximum heap size, so provision and monitor the logs volume accordingly.

For new or empty default `output-directory` and `logs` mounts, `docker.sh` sets the mount-point mode to `0700` and initializes its ownership with a minimal Docker Official Image pinned by digest; it does not execute the selected java-tron workload image with root privileges. The helper runs without networking, with a read-only root filesystem, and with only `CAP_CHOWN`, then `docker.sh` verifies from inside a restricted container that runtime identity `10001:10001` can write to the mounts. The pinned helper is pulled on first use if it is not already available locally.

Existing non-empty runtime directories are never silently chmodded. If the host user can inspect one whose root mode permits access by group or other users, `docker.sh` preserves the mode and prints a warning with a `chmod 0700` remediation command. If the host user cannot inspect the directory, the helper requires mode `0700` because it cannot safely distinguish an empty private directory from an existing exposed data tree. Stop the node before changing that mode. Custom writable mounts supplied with `-v` are not modified or inspected for confidentiality; their ownership and permissions remain the caller's responsibility.

Automatic initialization is supported by rootful Docker without user-namespace remapping and by rootless Docker. Rootless ownership appears on the host as the subordinate UID and GID selected by the Docker daemon. Rootful Docker with `userns-remap` is different: remapped container root generally cannot change ownership of a directory just created by the invoking host user. `docker.sh` detects that mode and refuses automatic initialization before running the ownership helper. Pre-create the directories with the mapped host UID and GID as described below. For non-empty mounts, the preflight check covers the mount point and its direct children only; it does not traverse the complete database tree. FullNode is started with `no-new-privileges`. Custom writable mounts supplied with `-v` must already be accessible to container UID and GID `10001:10001` through the active Docker user-namespace mapping.

Data written by an older root-based image may require a one-time ownership migration. Stop the node before changing ownership. The following command applies only to rootful Docker without user-namespace remapping; adjust the paths for the selected data directory:

```shell
sudo chown -R 10001:10001 \
  /var/lib/java-tron/output-directory \
  /var/lib/java-tron/logs
```

For rootless Docker or `userns-remap`, migrate the same paths to the host UID and GID that the active Docker daemon maps from container `10001:10001`; do not use literal host IDs `10001:10001` unless that is the daemon's actual mapping. For a rootful daemon configured with the default `dockremap` user, the following example derives the mapped IDs from the first subordinate ranges and provisions new empty directories. Replace `dockremap` if `userns-remap` names a different account, and confirm the daemon's mapping before applying ownership changes:

```shell
remap_user=dockremap
subuid_start=$(awk -F: -v user="$remap_user" '$1 == user { print $2; exit }' /etc/subuid)
subgid_start=$(awk -F: -v user="$remap_user" '$1 == user { print $2; exit }' /etc/subgid)
test -n "$subuid_start" && test -n "$subgid_start"
mapped_uid=$((subuid_start + 10001))
mapped_gid=$((subgid_start + 10001))
sudo install -d -m 0700 -o "$mapped_uid" -g "$mapped_gid" \
  /var/lib/java-tron/output-directory \
  /var/lib/java-tron/logs
```

The helper deliberately does not recursively inspect or change a non-empty directory because scanning a multi-terabyte database during every startup would be slow and unexpected. A partially migrated tree can therefore pass the shallow preflight check but fail later when FullNode reaches a deeper file. Run the appropriate one-time recursive ownership migration for data created by a root-based image. When the shallow check detects a problem, the helper prints guidance for both namespace cases. Direct `docker run` users must prepare writable mounts themselves and should also specify `--security-opt no-new-privileges`.

Use `--data-dir` to keep the default host runtime directories in an explicit location, preferably outside the source checkout. Relative values are resolved against the invocation directory:

```shell
bash docker.sh --run --net main --data-dir /var/lib/java-tron
```

For this Mainnet command, the helper creates and mounts `/var/lib/java-tron/output-directory` and `/var/lib/java-tron/logs`; it does not create `/var/lib/java-tron/config`. Using `--net private` with the same data directory additionally creates `/var/lib/java-tron/config` and mounts it read-only at `/java-tron/config`. The default data directory is the current working directory, so a Mainnet `--run` from a checkout writes `output-directory/` and `logs/` into that tree, while private mode also writes `config/`. Use `--data-dir` to keep those runtime files out of the Git worktree. Persisting `logs` also keeps `tron.log` available after the container is removed.

The data directory itself may be a symbolic link, and `docker.sh` resolves it to its physical directory before creating mounts. The selected path's existing parents, the resolved directory, and each of its ancestors must be owned by root or by the user running `docker.sh`, and none may be writable by their group or by other users. The helper enforces this before passing any managed path to `docker run`; use `chown` and `chmod go-w` to correct an unsafe path. The managed `output-directory` and `logs` paths, plus `config` in private mode, must be real directories and must not be symbolic links. To place the node data on another disk, point `--data-dir` at that disk or at a data-directory link instead of linking an individual managed path.

## Memory and JVM options

These memory defaults come from `docker.sh`, not from the image or the packaged `java-tron.vmoptions` file. A plain `docker run` or `bin/FullNode` invocation without the helper still uses the JVM ergonomics default of about 25% of visible memory.

`docker.sh` applies a minimum helper profile: a `16g` container memory limit, a 2 GB initial heap, and a maximum heap of up to 60% of that container limit:

```text
-Xms2g -XX:MaxRAMPercentage=60.0
```

JDK 8 images also receive `-XX:MaxDirectMemorySize=1g`. JDK 17 images already include that option in `java-tron.vmoptions`, so the helper does not add it again. The script inspects the image architecture to decide this, not the host `uname`.

This is a minimum **memory** profile for lower-load deployments; it does not enable Lite FullNode mode or reduce the database storage requirement described above. For stable Mainnet operation, use at least `32g`; Super Representative nodes require at least `64g`. See the [Mainnet hardware requirements](../README.md#hardware-requirements-for-mainnet) for the complete deployment tiers.

Use `--memory` to change the container memory limit. When the default helper JVM options are retained, the maximum heap scales with this limit:

```shell
bash docker.sh --run --net main --memory 32g
```

To replace the helper JVM options entirely, use `--jvm-opts`. The replacement is not merged with the defaults, so include every option you need:

```shell
bash docker.sh --run --net main --memory 32g \
    --jvm-opts "-Xms4g -Xmx18g -XX:MaxDirectMemorySize=2g"
```

The packaged `java-tron.vmoptions` file remains active. It contains architecture- and JDK-specific garbage collector settings, so do not use `--jvm-opts` to copy or switch GC options between JDK 8 and JDK 17 deployments.

Environment variables for custom wrapper scripts or derived images can be passed with repeatable `-e` or `--env` options. `MY_VARIABLE` is only a placeholder. JVM option environment variables (`JAVA_OPTS`, `FULL_NODE_OPTS`, `JAVA_TOOL_OPTIONS`, `_JAVA_OPTIONS`, and `JDK_JAVA_OPTIONS`) are rejected because they can replace or bypass the helper profile. Use `--jvm-opts` instead.

```shell
bash docker.sh --run --net main -e "MY_VARIABLE=value"
```

## Container lifecycle

View the java-tron log:

```shell
bash docker.sh --log
```

For example, filter block-processing messages with:

```shell
bash docker.sh --log | grep 'PushBlock'
```

Stop and restart the container:

```shell
bash docker.sh --stop
bash docker.sh --start
```

Remove the container without deleting the image or persisted host data:

```shell
bash docker.sh --rm
```

The lifecycle commands return a non-zero status when the target container does not exist, the container cannot be queried, or the underlying Docker operation fails. This allows service managers and automation scripts to detect failures reliably.

## Build an image

`--build` selects `Dockerfile` on x86_64/amd64 and `arm64/Dockerfile` on arm64/aarch64. For a remote or standalone `--build`, a missing Dockerfile is downloaded from the java-tron `master` branch. `--source local` uses the Dockerfile from the same checkout and fails if that file is not present.

For backward compatibility, `--build` without source options clones and compiles the remote java-tron `master` branch. Local working-tree changes are not included:

```shell
bash docker.sh --build
```

Use `--source-ref` to build another remote branch or tag. `--source-repository` can select another public Git repository:

```shell
bash docker.sh --build \
    --source remote \
    --source-ref develop
```

Each helper-driven remote build pulls refreshed base-image metadata and invalidates the `remote-builder` stage, so a moved branch such as `master` is cloned and rebuilt instead of being silently reused from a previous Docker layer. The selected ref is still a remote Git trust input rather than cryptographically pinned provenance; use a controlled repository and release ref for distributable images.

From a java-tron checkout, use `--source local` to compile the current working tree, including uncommitted changes:

```shell
bash docker/docker.sh --build --source local
```

In local mode, `docker.sh` runs the Gradle `:framework:distZip` task on the host, uses the architecture-specific Dockerfile and Mainnet configuration from the same checkout, and extracts the resulting distribution into a private staging directory. The build fails instead of downloading a Dockerfile or configuration when the checkout does not contain the expected file. It also fails closed if the distribution contains symbolic links, special files, or paths outside the supported `bin` launchers and flat `lib/*.jar` layout. The generated context includes a matching allowlist `.dockerignore` as a second boundary, so only those runtime files, the checked configuration, and the local Dockerfile are available to the Docker daemon. Node databases, logs, wallets, node identities, environment files, keys, keystores, and other unexpected distribution content are rejected rather than exported or baked into the image.

The checkout's `framework/src/main/resources/config.conf` is copied verbatim into the local image as the world-readable `/java-tron/config.conf`. Before building, `docker.sh` rejects a non-empty plaintext `localwitness` list, `event.subscribe.dbconfig`, `node.dns.dnsPrivate`, or `node.dns.accessKeySecret` value so signing keys and service credentials are not accidentally baked into an image layer. Clear those settings before building and provide sensitive signing, database, or DNS configuration through a protected runtime bind mount. This targeted check does not prove that every custom configuration field is free of sensitive data, so review `config.conf` before distributing the image.

The two architecture-specific Dockerfiles each provide `local` and `remote` BuildKit targets. `docker.sh` selects the appropriate target and supplies its required minimal context. A plain Dockerfile build defaults to the historical `remote` target, but direct `local` target builds require callers to stage the distribution as `java-tron/` first.

`--build` and `--run` use `tronprotocol/java-tron:local` by default. This keeps the default run path aligned with the helper-built non-root image and avoids silently selecting a legacy published image. `--pull` has no implicit image and requires `--image` or `JAVA_TRON_IMAGE`:

```shell
bash docker.sh --build
bash docker.sh --run --net main
```

Override the image for `--pull`, `--build`, or `--run` with `--image NAME[:TAG]` or the `JAVA_TRON_IMAGE` environment variable:

```shell
bash docker.sh --build --source local --image java-tron:dev
bash docker.sh --run --image java-tron:dev --net main
JAVA_TRON_IMAGE=java-tron:ci-amd64 bash docker.sh --run --net private
```

## Options

| Option | Description |
| --- | --- |
| `-h`, `--help` | Show command usage without requiring a Docker daemon. |
| `--build` | Build an image for the host architecture. Defaults to remote `master` source for backward compatibility. Tags `tronprotocol/java-tron:local` unless `--image` is set. |
| `--source local\|remote` | Select a host-built distribution or a remote source build for `--build`. Default: `remote`. |
| `--source-ref REF` | Select the remote branch or tag for `--build`. Default: `master`. |
| `--source-repository URL` | Select the public remote Git repository for `--build`. |
| `--export-context PATH` | With `--build --source local`, prepare a new minimal build context at `PATH` without building an image. The destination must not already exist. Intended for external BuildKit frontends such as CI. |
| `--image NAME[:TAG]` | Select the image for `--pull`, `--build`, or `--run`. `JAVA_TRON_IMAGE` sets the same value. |
| `--pull` | Pull an explicitly selected image. Requires `--image NAME[:TAG]` or `JAVA_TRON_IMAGE`; the pulled image must declare runtime UID:GID `10001:10001`. |
| `--run` | Create and start a container. Default: `tronprotocol/java-tron:local`. Use `--image` to select a compatible published image. |
| `--container-name NAME` | Container name for `--run`, `--start`, `--stop`, `--log`, and `--rm`. Default: `tronprotocol-java-tron`. |
| `--start` | Start the existing stopped container. |
| `--log` | Follow the java-tron log in the container. |
| `--stop` | Stop the running container. |
| `--rm` | Remove the container without removing the image or host data. |
| `-p [HOST_IP:]HOST_PORT:CONTAINER_PORT[/PROTOCOL]` | Publish a container port. Repeat to customize multiple mappings. |
| `-c CONTAINER_PATH` | Use a configuration file at the specified path inside the container. |
| `-v HOST_PATH:CONTAINER_PATH[:OPTIONS]` | Add or replace a bind mount. The host path should be absolute. Writable custom mounts must be accessible to container UID:GID `10001:10001` through the active Docker user-namespace mapping. In private mode, a replacement for the default `/java-tron/config` mount uses the caller's access mode instead of read-only mode. |
| `-e NAME=VALUE`, `--env NAME=VALUE` | Set a container environment variable. This option can be repeated. JVM option environment variables are rejected; use `--jvm-opts`. |
| `--net main\|private` | Select the Mainnet or private-network configuration. Since Nile may incorporate features not yet available on the Mainnet, use the separate [nile-testnet](https://github.com/tron-nile-testnet/nile-testnet) codebase and follow [tron-docker](https://github.com/tronprotocol/tron-docker). |
| `--update-config true\|false` | Refresh the private-network configuration before creating the container. Default: `false`; a missing or empty file is still downloaded. This option has no effect with `--net main`. |
| `--data-dir PATH` | Store the default runtime data under this host path. Mainnet uses only `output-directory` and `logs`; private mode also uses `config`. Default: invocation directory. The managed `config` mount is read-only in the container. |
| `--memory LIMIT` | Set the container memory limit. Default: `16g`. |
| `--jvm-opts "OPTIONS"` | Replace the JVM options supplied by `docker.sh`. The image itself does not set these defaults. |
| `-- FULLNODE_ARGS...` | Pass all remaining arguments unchanged to FullNode. |
