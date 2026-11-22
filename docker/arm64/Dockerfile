FROM ubuntu:24.04
ARG VERSION="dev"
ARG OPENJDK_VERSION="17.0.21.1+2-1~24.04.1"
ENV NO_PROXY_CACHE="-o Acquire::BrokenProxy=true -o Acquire::http::No-Cache=true -o Acquire::http::Pipeline-Depth=0"

ENV TMP_DIR="/tron-build"
ENV BASE_DIR="/java-tron"
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-arm64"
ENV PATH=$JAVA_HOME/bin:$PATH

# Apply Ubuntu updates to packages inherited from the base image before installing build tools.
RUN apt-get --error-on=any update $NO_PROXY_CACHE \
  && DEBIAN_FRONTEND=noninteractive apt-get --quiet --yes --no-install-recommends --with-new-pkgs upgrade \
  && apt-get --quiet --yes --no-install-recommends install ca-certificates git libtcmalloc-minimal4 \
       "openjdk-17-jre-headless=$OPENJDK_VERSION" \
  && echo "git clone" \
  && mkdir -p $TMP_DIR \
  && cd $TMP_DIR \
  && git clone https://github.com/tronprotocol/java-tron.git \
  && cd java-tron \
  && git checkout master \
  && ./gradlew clean build -x test -x check --no-daemon \
  && cd build/distributions \
  && java -m jdk.jartool/sun.tools.jar.Main xf java-tron-1.0.0.zip \
  && mv java-tron-1.0.0 "$BASE_DIR" \
  && chmod 755 "$BASE_DIR/bin/FullNode" "$BASE_DIR/bin/FullNode.bat" \
  && cp ../../framework/src/main/resources/config.conf "$BASE_DIR/config.conf" \
  && cd / \
  && rm -rf $TMP_DIR \
  && rm -rf ~/.gradle \
  && apt-get --quiet --yes purge --auto-remove git \
  # Clean apt cache
  && apt-get clean \
  && rm -rf /var/cache/apt/archives/* /var/cache/apt/archives/partial/* \
  && rm -rf /var/lib/apt/lists/*

ENV LD_PRELOAD="/usr/lib/aarch64-linux-gnu/libtcmalloc_minimal.so.4"
ENV TCMALLOC_RELEASE_RATE=10

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
