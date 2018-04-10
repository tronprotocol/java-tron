FROM tronprotocol/tron-gradle

RUN set -o errexit -o nounset \
#  Download and build java-tron
    && echo "git clone" \
    && git clone https://github.com/tronprotocol/java-tron.git \
    && cd java-tron \
    && gradle build

# Change work directory
WORKDIR /java-tron

# open port 18888
EXPOSE 18888

