FROM frolvlad/alpine-oraclejdk8:slim

# keeping everything updated
RUN apk -U upgrade && \
    apk add --no-cache --update bash curl wget \
        tar tzdata iputils unzip findutils git gettext gdb lsof patch \
        libcurl libxml2 libxslt openssl-dev zlib-dev \
        make automake gcc g++ binutils-gold linux-headers paxctl libgcc libstdc++ \
        python gnupg ncurses-libs ca-certificates && \
    update-ca-certificates --fresh && \
    rm -rf /var/cache/apk/*

# home folder
ENV HOME=/opt/java-tron
RUN	mkdir -p ${HOME} && \
    adduser -s /bin/sh -u 1001 -G root -h ${HOME} -S -D default && \
    chown -R 1001:0 ${HOME}

COPY ../build/libs/FullNode.jar ${HOME}/FullNode.jar
COPY main_net_config.conf ${HOME}/main_net_config.conf

VOLUME ["${HOME}/output-directory"]

COPY start.sh ${HOME}/start.sh
RUN	chmod +x ${HOME}/start.sh


WORKDIR ${HOME}
USER 1001

EXPOSE 8090
EXPOSE 50051
EXPOSE 18888
EXPOSE 10001/udp
EXPOSE 18888/udp

CMD ${HOME}/start.sh