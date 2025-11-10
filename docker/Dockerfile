# Multi-stage build for java-tron
# Stage 1: Build stage with full JDK and build tools
FROM eclipse-temurin:8-jdk-jammy AS builder

ARG GIT_BRANCH=staking-release
ARG GIT_REPO=https://github.com/jasonyic/java-tron.git

ENV BUILD_DIR="/build"

# Install build dependencies
RUN apt-get update && apt-get install -y \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Clone and build java-tron
RUN set -o errexit -o nounset \
    && echo "Cloning java-tron from ${GIT_REPO}" \
    && git clone ${GIT_REPO} ${BUILD_DIR} \
    && cd ${BUILD_DIR} \
    && echo "Building from branch/tag: ${GIT_BRANCH}" \
    && git checkout ${GIT_BRANCH} \
    && echo "Starting Gradle build..." \
    && ./gradlew build -x test \
    && echo "Build completed successfully"

# Extract the distribution
RUN cd ${BUILD_DIR}/build/distributions \
    && unzip java-tron-*.zip \
    && mv java-tron-* /java-tron-dist


# Stage 2: Final runtime image with only JRE
FROM eclipse-temurin:8-jre-jammy

LABEL maintainer="TRON Foundation"
LABEL description="TRON Full Node"

ENV BASE_DIR="/java-tron"
ENV JAVA_OPTS="-Xmx6g -XX:+UseConcMarkSweepGC"

# Install runtime dependencies only
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p ${BASE_DIR}

# Copy the built application from builder stage
COPY --from=builder /java-tron-dist ${BASE_DIR}

# Download mainnet config
RUN wget -O ${BASE_DIR}/config/main_net_config.conf \
    https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/main_net_config.conf

# Copy docker entrypoint
COPY java-tron/docker/docker-entrypoint.sh ${BASE_DIR}/bin/
RUN chmod +x ${BASE_DIR}/bin/docker-entrypoint.sh

WORKDIR ${BASE_DIR}

ENTRYPOINT ["./bin/docker-entrypoint.sh"]
