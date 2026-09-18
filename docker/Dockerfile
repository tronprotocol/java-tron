FROM ubuntu:24.04
ARG VERSION="dev"
ENV NO_PROXY_CACHE="-o Acquire::BrokenProxy=true -o Acquire::http::No-Cache=true -o Acquire::http::Pipeline-Depth=0"

ENV TMP_DIR="/tron-build"
ENV OPENJDK8_URL="https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/eclipse"
ENV ADOPTIUM_SIGNING_FINGERPRINT="3B04D753C9050D9A5D343F39843C48A565F8F04B"
ENV JDK_DIR="/usr/local/openjdk-8"
ENV BASE_DIR="/java-tron"

# Update and install dependencies without using any cache
RUN apt-get update $NO_PROXY_CACHE  && \
  apt-get --quiet --yes install git 7zip curl jq libtcmalloc-minimal4 gnupg dirmngr ca-certificates && \
  cd /usr/local \
  && FETCH_URL="$(curl -fsS -w "%{redirect_url}" -o /dev/null "$OPENJDK8_URL")" \
  && JDK_TAR="$(curl -fsSL -w "%{filename_effective}" -O "$FETCH_URL")" \
  && curl -fsSLo "$JDK_TAR.sig" "$FETCH_URL.sig" \
  && GNUPGHOME="$(mktemp -d)" \
  && export GNUPGHOME \
  && gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys "$ADOPTIUM_SIGNING_FINGERPRINT" \
  && gpg --batch --verify "$JDK_TAR.sig" "$JDK_TAR" \
  && rm -rf "$GNUPGHOME" "$JDK_TAR.sig" \
  && mkdir -p "$JDK_DIR" \
  && tar -zxf "$JDK_TAR" -C "$JDK_DIR" --strip-components=1 \
  && rm "$JDK_TAR" \
  && export JAVA_HOME=$JDK_DIR \
  && export CLASSPATH=$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar \
  && export PATH=$PATH:$JAVA_HOME/bin \
  && echo "git clone" \
  && mkdir -p $TMP_DIR \
  && cd $TMP_DIR \
  && git clone https://github.com/tronprotocol/java-tron.git \
  && cd java-tron \
  && git checkout master \
  && ./gradlew clean build -x test -x check --no-daemon \
  && cd build/distributions \
  && 7z x -y java-tron-1.0.0.zip  \
  && cp $TMP_DIR/java-tron/framework/src/main/resources/config.conf java-tron-1.0.0/config.conf \
  && mv java-tron-1.0.0 $BASE_DIR \
  && rm -rf $TMP_DIR \
  && rm -rf ~/.gradle \
  && mv $JDK_DIR/jre /usr/local \
  && rm -rf $JDK_DIR \
  # Clean apt cache
  && apt-get clean \
  && rm -rf /var/cache/apt/archives/* /var/cache/apt/archives/partial/* \
  && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME="/usr/local/jre"
ENV PATH=$PATH:$JAVA_HOME/bin
ENV LD_PRELOAD="/usr/lib/x86_64-linux-gnu/libtcmalloc_minimal.so.4"
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
