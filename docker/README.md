# Java-TRON Docker Setup

Modern, optimized Docker setup for java-tron with multi-stage builds.

## Improvements Over Legacy Dockerfile

### 🚀 Modern Base Image
- **Old**: CentOS 7 (EOL)
- **New**: Ubuntu 22.04 LTS (Jammy) with Eclipse Temurin JDK/JRE

### 📦 Multi-Stage Build
- **Build Stage**: Full JDK + build tools (discarded after build)
- **Runtime Stage**: Only JRE + application
- **Result**: ~70% smaller final image

### ✨ Additional Features
- Flexible branch/tag building
- Health checks
- Proper port exposure
- Volume management
- Environment variable configuration
- Logging configuration

## Quick Start

### Build from Master Branch

```bash
docker build -t java-tron:latest ./docker/
```

### Build from Specific Branch

```bash
docker build \
  --build-arg GIT_BRANCH=fix-p2p \
  -t java-tron:fix-p2p \
  ./docker/
```

### Build from Your Fork

```bash
docker build \
  --build-arg GIT_REPO=https://github.com/yourusername/java-tron.git \
  --build-arg GIT_BRANCH=develop \
  -t java-tron:custom \
  ./docker/
```

## Using Docker Compose

### Start Full Node

```bash
cd docker
docker-compose up -d
```

### Build from Custom Branch

```bash
GIT_BRANCH=fix-p2p docker-compose up --build -d
```

### View Logs

```bash
docker-compose logs -f
```

### Stop Node

```bash
docker-compose down
```

### Stop and Remove Data

```bash
docker-compose down -v
```

## Configuration

### Environment Variables

- `JAVA_OPTS`: JVM options (default: `-Xmx6g -XX:+UseConcMarkSweepGC`)
- `GIT_BRANCH`: Branch/tag to build from (default: `master`)
- `GIT_REPO`: Git repository URL (default: `https://github.com/jasonyic/java-tron.git`)

### Custom Configuration File

Mount your own config file:

```yaml
volumes:
  - ./my-config.conf:/java-tron/config/config.conf:ro
```

Then run:

```bash
docker run -v ./my-config.conf:/java-tron/config/config.conf:ro \
  java-tron:latest -c /java-tron/config/config.conf
```

## Build Arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `GIT_BRANCH` | `master` | Git branch, tag, or commit hash to build |
| `GIT_REPO` | `https://github.com/jasonyic/java-tron.git` | Git repository URL |

## Exposed Ports

| Port | Service | Description |
|------|---------|-------------|
| 8090 | HTTP API | RESTful API endpoint |
| 50051 | gRPC | gRPC API endpoint |
| 18888 | P2P | Peer-to-peer network port |

## Volumes

- `/java-tron/output-directory` - Blockchain data storage

## Health Check

The container includes a health check that queries the node status every 30 seconds:

```bash
curl -f http://localhost:8090/wallet/getnowblock
```

Check health status:

```bash
docker inspect --format='{{.State.Health.Status}}' java-tron-node
```

## Advanced Usage

### Run as Witness

```bash
docker run -d \
  -p 8090:8090 -p 50051:50051 -p 18888:18888 \
  -v tron-data:/java-tron/output-directory \
  java-tron:latest \
  --witness \
  -c /java-tron/config/config.conf
```

### Adjust Memory

```bash
docker run -d \
  -e JAVA_OPTS="-Xmx12g -XX:+UseG1GC" \
  java-tron:latest
```

### Interactive Shell

```bash
docker run -it --entrypoint /bin/bash java-tron:latest
```

## Size Comparison

| Build Type | Size | Notes |
|------------|------|-------|
| Legacy Single-Stage | ~1.2 GB | Includes build tools, git, full JDK |
| New Multi-Stage | ~350 MB | Only JRE + application |
| **Savings** | **~70%** | ⚡ Faster pulls, less storage |

## Troubleshooting

### Check Logs

```bash
docker logs java-tron-node
```

### Check Health

```bash
docker exec java-tron-node curl http://localhost:8090/wallet/getnowblock
```

### Enter Container

```bash
docker exec -it java-tron-node /bin/bash
```

### Rebuild Without Cache

```bash
docker build --no-cache -t java-tron:latest ./docker/
```

## Production Recommendations

1. **Use specific tags**: Don't use `latest` in production
2. **Set resource limits**: Use `--memory` and `--cpus` flags
3. **Mount volumes**: Persist blockchain data
4. **Monitor health**: Use health check endpoints
5. **Configure logging**: Set up log rotation
6. **Network security**: Restrict port access appropriately

## Example Production Setup

```yaml
version: '3.8'

services:
  java-tron:
    image: java-tron:v4.7.0
    container_name: java-tron-production
    restart: always
    ports:
      - "8090:8090"
      - "50051:50051"
      - "18888:18888"
    volumes:
      - /data/tron:/java-tron/output-directory
      - ./config-production.conf:/java-tron/config/config.conf:ro
    environment:
      - JAVA_OPTS=-Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    deploy:
      resources:
        limits:
          cpus: '8'
          memory: 20G
        reservations:
          cpus: '4'
          memory: 16G
    logging:
      driver: "json-file"
      options:
        max-size: "200m"
        max-file: "5"
```

## License

Same as java-tron project license.

