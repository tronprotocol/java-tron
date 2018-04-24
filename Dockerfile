FROM tronprotocol/tron-gradle

RUN set -o errexit -o nounset \
    && echo "git clone" \
    && git clone https://github.com/tronprotocol/java-tron.git \
    && cd java-tron \
    && gradle build

WORKDIR /java-tron

EXPOSE 18888