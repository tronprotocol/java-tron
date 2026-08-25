# syntax=docker/dockerfile:1

FROM ubuntu:24.04 AS java-base

ENV NO_PROXY_CACHE="-o Acquire::BrokenProxy=true -o Acquire::http::No-Cache=true -o Acquire::http::Pipeline-Depth=0"
ENV OPENJDK8_URL="https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/eclipse"
ENV ADOPTIUM_SIGNING_FINGERPRINT="3B04D753C9050D9A5D343F39843C48A565F8F04B"
ENV JAVA_HOME="/usr/local/openjdk-8"
ENV CLASSPATH="$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar"
ENV PATH="$PATH:$JAVA_HOME/bin"

RUN apt-get update $NO_PROXY_CACHE \
  && apt-get --quiet --yes install --no-install-recommends curl gnupg dirmngr ca-certificates \
  && apt-get clean \
  && rm -rf /var/cache/apt/archives/* /var/cache/apt/archives/partial/* \
  && rm -rf /var/lib/apt/lists/*

RUN cd /usr/local \
  && FETCH_URL="$(curl -fsS -w "%{redirect_url}" -o /dev/null "$OPENJDK8_URL")" \
  && JDK_TAR="$(curl -fsSL -w "%{filename_effective}" -O "$FETCH_URL")" \
  && curl -fsSLo "$JDK_TAR.sig" "$FETCH_URL.sig" \
  && GNUPGHOME="$(mktemp -d)" \
  && export GNUPGHOME \
  && gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys "$ADOPTIUM_SIGNING_FINGERPRINT" \
  && gpg --batch --verify "$JDK_TAR.sig" "$JDK_TAR" \
  && rm -rf "$GNUPGHOME" "$JDK_TAR.sig" \
  && mkdir -p "$JAVA_HOME" \
  && tar -zxf "$JDK_TAR" -C "$JAVA_HOME" --strip-components=1 \
  && rm "$JDK_TAR"

FROM java-base AS remote-builder
ARG SOURCE_REPOSITORY="https://github.com/tronprotocol/java-tron.git"
ARG SOURCE_REF="master"

ENV TMP_DIR="/tron-build"

RUN apt-get update $NO_PROXY_CACHE \
  && apt-get --quiet --yes install --no-install-recommends git 7zip \
  && apt-get clean \
  && rm -rf /var/cache/apt/archives/* /var/cache/apt/archives/partial/* \
  && rm -rf /var/lib/apt/lists/*

RUN git clone --depth 1 --branch "$SOURCE_REF" -- "$SOURCE_REPOSITORY" "$TMP_DIR/java-tron" \
  && cd "$TMP_DIR/java-tron" \
  && ./gradlew :framework:distZip -x test -x check --no-daemon \
  && cd framework/build/distributions \
  && 7z x -y java-tron-1.0.0.zip \
  && mv java-tron-1.0.0 /java-tron \
  && cp "$TMP_DIR/java-tron/framework/src/main/resources/config.conf" /java-tron/config.conf

# docker.sh creates this minimal context from a distribution built on the host.
FROM scratch AS local-distribution
COPY java-tron /java-tron

FROM ubuntu:24.04 AS runtime-base
ARG VERSION="dev"

ENV NO_PROXY_CACHE="-o Acquire::BrokenProxy=true -o Acquire::http::No-Cache=true -o Acquire::http::Pipeline-Depth=0"
ENV BASE_DIR="/java-tron"
ENV JAVA_HOME="/usr/local/jre"
ENV PATH="$PATH:$JAVA_HOME/bin"
ENV LD_PRELOAD="/usr/lib/x86_64-linux-gnu/libtcmalloc_minimal.so.4"
ENV TCMALLOC_RELEASE_RATE=10

RUN apt-get update $NO_PROXY_CACHE \
  && apt-get --quiet --yes install --no-install-recommends ca-certificates libtcmalloc-minimal4 zlib1g \
  && apt-get clean \
  && rm -rf /var/cache/apt/archives/* /var/cache/apt/archives/partial/* \
  && rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 10001 tron \
  && useradd --uid 10001 --gid 10001 --no-create-home --home-dir "$BASE_DIR" \
    --shell /usr/sbin/nologin tron \
  && mkdir -p "$BASE_DIR"

# Keep in sync with docker/arm64/Dockerfile. Defined here so docker.sh can
# build from a Dockerfile-only context.
COPY <<'LOCKDOWN' /usr/local/sbin/lockdown-java-tron
#!/bin/sh
set -eu

base=/java-tron
config="$base/config.conf"
launcher="$base/bin/FullNode"
vm_options="$base/bin/java-tron.vmoptions"

if [ ! -d "$base/bin" ] || [ ! -f "$launcher" ] || [ -L "$launcher" ] \
  || [ ! -x "$launcher" ] || [ ! -f "$vm_options" ] \
  || [ ! -f "$config" ] || [ -L "$config" ]; then
  echo "lockdown-java-tron: application files are missing under $base" >&2
  exit 1
fi

chown -R root:root "$base"
chmod 755 "$base"
chmod 0644 "$config"
mkdir -p "$base/output-directory" "$base/logs"
chown 10001:10001 "$base/output-directory" "$base/logs"
chmod 700 "$base/output-directory" "$base/logs"

# Keep Docker-created diagnostics private even when the caller's host umask is
# permissive. This changes only the launcher copied into the container image.
sed -i '2i umask 077' "$launcher"

sed -i \
  -e 's|^-Xloggc:./gc.log|-Xloggc:/java-tron/logs/gc.log|' \
  -e 's|:file=gc.log:|:file=/java-tron/logs/gc.log:|' \
  "$vm_options"

grep -Fqx -- '-XX:+HeapDumpOnOutOfMemoryError' "$vm_options" \
  || echo '-XX:+HeapDumpOnOutOfMemoryError' >> "$vm_options"
grep -q -- '-XX:HeapDumpPath=' "$vm_options" \
  || echo '-XX:HeapDumpPath=/java-tron/logs' >> "$vm_options"
grep -q -- '-XX:ErrorFile=' "$vm_options" \
  || echo '-XX:ErrorFile=/java-tron/logs/hs_err_pid%p.log' >> "$vm_options"
LOCKDOWN
RUN chmod 0755 /usr/local/sbin/lockdown-java-tron

COPY --from=java-base /usr/local/openjdk-8/jre /usr/local/jre

WORKDIR $BASE_DIR

ENTRYPOINT ["./bin/FullNode"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Java-TRON" \
      org.label-schema.description="TRON protocol" \
      org.label-schema.url="https://tron.network/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/tronprotocol/java-tron.git" \
      org.label-schema.vendor="TRON protocol" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

FROM runtime-base AS local
COPY --from=local-distribution /java-tron /java-tron
RUN /usr/local/sbin/lockdown-java-tron
USER 10001:10001

# Keep remote last so a plain BuildKit build retains the historical behavior.
FROM runtime-base AS remote
COPY --from=remote-builder /java-tron /java-tron
RUN /usr/local/sbin/lockdown-java-tron
USER 10001:10001
