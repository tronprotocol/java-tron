FROM rockylinux:8

ENV TMP_DIR="/tron-build"
ENV BASE_DIR="/java-tron"

RUN set -o errexit -o nounset \
    && dnf -y install git wget unzip java-1.8.0-openjdk-devel \
    && echo "git clone" \
    && mkdir -p $TMP_DIR \
    && cd $TMP_DIR \
    && git clone https://github.com/tronprotocol/java-tron.git \
    && cd java-tron \
    && git checkout master \
    && ./gradlew build -x test \
    && cd build/distributions \
    && unzip -q java-tron-1.0.0.zip  \
    && mv java-tron-1.0.0 $BASE_DIR \
    && rm -rf $TMP_DIR \
    && rm -rf ~/.gradle \
    && dnf -y remove java-1.8.0-openjdk-devel \
    && dnf clean all

RUN wget -P $BASE_DIR/config https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/main_net_config.conf

COPY docker-entrypoint.sh $BASE_DIR/bin

WORKDIR $BASE_DIR

ENTRYPOINT ["./bin/docker-entrypoint.sh"]
