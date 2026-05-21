FROM arm64v8/eclipse-temurin:17

ENV TMP_DIR="/tron-build"
ENV BASE_DIR="/java-tron"

RUN set -o errexit -o nounset \
    && apt-get update \
    && apt-get -y install git p7zip-full wget libtcmalloc-minimal4 \
    && echo "git clone" \
    && mkdir -p $TMP_DIR \
    && cd $TMP_DIR \
    && git clone https://github.com/tronprotocol/java-tron.git \
    && cd java-tron \
    && git checkout master \
    && ./gradlew clean build -x test -x check --no-daemon \
    && cd build/distributions \
    && 7za x -y java-tron-1.0.0.zip  \
    && mv java-tron-1.0.0 $BASE_DIR \
    && rm -rf $TMP_DIR \
    && rm -rf ~/.gradle \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV LD_PRELOAD="/usr/lib/aarch64-linux-gnu/libtcmalloc_minimal.so.4"
ENV TCMALLOC_RELEASE_RATE=10

RUN wget -P $BASE_DIR/config https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/main_net_config.conf

COPY docker-entrypoint.sh $BASE_DIR/bin

WORKDIR $BASE_DIR

ENTRYPOINT ["./bin/docker-entrypoint.sh"]