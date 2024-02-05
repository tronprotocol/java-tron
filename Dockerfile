FROM openjdk:8-jdk AS builder

WORKDIR /builder

COPY . .

RUN ./gradlew build -x test

FROM openjdk:8-jre AS runner

WORKDIR /app

COPY --from=builder \
    builder/build/libs/FullNode.jar \
    /app/jar/

COPY ./entrypoint.sh ./docker-healthcheck.sh /app/

RUN apt-get update && \
    apt-get install -y --no-install-recommends jq=1.6-2.1 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    chmod +x /app/entrypoint.sh ./docker-healthcheck.sh

HEALTHCHECK --interval=1m --timeout=10s CMD [ "./docker-healthcheck.sh" ]

ENTRYPOINT ["./entrypoint.sh"]
