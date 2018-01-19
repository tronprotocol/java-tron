/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.overlay.kafka;

import static org.tron.common.overlay.kafka.Kafka.KAFKA_HOST;
import static org.tron.common.overlay.kafka.Kafka.KAFKA_PORT;

import com.typesafe.config.Config;
import org.tron.core.config.Configer;

public class ProducerProperty {
  private final static String DEFAULT_BOOTSTRAP_SERVERS = "127.0.0.1:9092";
  private final static String DEFAULT_ACKS = "all";
  private final static String DEFAULT_RETRIES = "0";
  private final static String DEFAULT_BATCH_SIZE = "16384";
  private final static String DEFAULT_LINGER_MS = "1";
  private final static String DEFAULT_BUFFER_MEMORY = "33554432";
  private final static String DEFAULT_KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
  private final static String DEFAULT_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

  private String bootstrapServers;
  private String acks;
  private String retries;
  private String batchSize;
  private String lingerMS;
  private String bufferMemory;
  private String keySerializer;
  private String valueSerializer;

  public ProducerProperty() {
  }

  public ProducerProperty(
      String bootstrapServers,
      String acks,
      String retries,
      String batchSize,
      String lingerMS,
      String bufferMemory,
      String keySerializer,
      String valueSerializer) {
    this.bootstrapServers = bootstrapServers;
    this.acks = acks;
    this.retries = retries;
    this.batchSize = batchSize;
    this.lingerMS = lingerMS;
    this.bufferMemory = bufferMemory;
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
  }

  public static ProducerProperty getDefault() {
    Config config = Configer.getConf();
    String bootstrapServers = config.getString(KAFKA_HOST) + config.getString(KAFKA_PORT);

    if (bootstrapServers.isEmpty()) {
      bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
    }

    return new ProducerProperty(
        bootstrapServers,
        DEFAULT_ACKS,
        DEFAULT_RETRIES,
        DEFAULT_BATCH_SIZE,
        DEFAULT_LINGER_MS,
        DEFAULT_BUFFER_MEMORY,
        DEFAULT_KEY_SERIALIZER,
        DEFAULT_VALUE_SERIALIZER
    );
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public String getAcks() {
    return acks;
  }

  public void setAcks(String acks) {
    this.acks = acks;
  }

  public String getRetries() {
    return retries;
  }

  public void setRetries(String retries) {
    this.retries = retries;
  }

  public String getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(String batchSize) {
    this.batchSize = batchSize;
  }

  public String getLingerMS() {
    return lingerMS;
  }

  public void setLingerMS(String lingerMS) {
    this.lingerMS = lingerMS;
  }

  public String getBufferMemory() {
    return bufferMemory;
  }

  public void setBufferMemory(String bufferMemory) {
    this.bufferMemory = bufferMemory;
  }

  public String getKeySerializer() {
    return keySerializer;
  }

  public void setKeySerializer(String keySerializer) {
    this.keySerializer = keySerializer;
  }

  public String getValueSerializer() {
    return valueSerializer;
  }

  public void setValueSerializer(String valueSerializer) {
    this.valueSerializer = valueSerializer;
  }
}
