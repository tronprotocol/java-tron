#!/usr/bin/python3

import os
from kafka.admin import KafkaAdminClient, NewTopic, ConfigResource
from kafka.errors import TopicAlreadyExistsError

replication_factor = os.environ.get('KAFKA_REPLICATION_FACTOR') or 2
num_partitions = os.environ.get('KAFKA_NUM_PARTITIONS') or 2
retention_sec = os.environ.get('KAFKA_RETENTION_SEC') or 60 * 60 * 24 * 20
bootstrap_servers = os.environ.get('KAFKA_SERVERS')
security_protocol = os.environ.get('KAFKA_SECURITY_PROTOCOL')

blocks_streaming_kafka_topic = os.environ.get('BLOCKS_STREAMING_KAFKA_TOPIC')
blocks_streaming_enable = os.environ.get('BLOCKS_STREAMING_ENABLE')
broadcasted_streaming_kafka_topic = os.environ.get('BROADCASTED_STREAMING_KAFKA_TOPIC')
broadcasted_streaming_enable = os.environ.get('BROADCASTED_STREAMING_ENABLE')

def create_topics():
    topics = [
        {
            'name': blocks_streaming_kafka_topic,
            'enable': blocks_streaming_enable,
        },
        {
            'name': broadcasted_streaming_kafka_topic,
            'enable': broadcasted_streaming_enable,
        },
    ] 

    client = KafkaAdminClient(
        bootstrap_servers=bootstrap_servers,
        security_protocol=security_protocol,
        ssl_keyfile='/app/config/ssl/client.key.pem',
        ssl_certfile='/app/config/ssl/client.cer.pem',
        ssl_cafile='/app/config/ssl/server.cer.pem',
        ssl_check_hostname=False,
        client_id='create-kafka-topics'
    )

    for topic in topics:
        name = topic['name']

        try:
            client.create_topics(new_topics=[NewTopic(name=name, num_partitions=int(num_partitions),
                                                      replication_factor=int(replication_factor))],
                                 validate_only=False)
            client.alter_configs(config_resources=[ConfigResource(resource_type='TOPIC', name=name, configs={
                "retention.ms": int(retention_sec) * 1000})])
            print(name + " created")
        except TopicAlreadyExistsError:
            print(name + " already exists")


def main():
    create_topics()

if __name__ == '__main__':
    main()


