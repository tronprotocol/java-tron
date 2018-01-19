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

public class ConsumerProperty {
  private final static String DEFAULT_GROUP_ID = Configer.getGNPK();
  private final static String DEFAULT_ENABLE_AUTO_COMMIT = "true";
  private final static String DEFAULT_AUTO_COMMIT_INTERVAL_MS = "1000";
  private final static String DEFAULT_SESSION_TIMEOUT_MS = "30000";
  private final static String DEFAULT_KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
  private final static String DEFAULT_VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";

  private String bootstrapServers;
  private String groupID;
  private String enableAutoCommit;
  private String autoCommitIntervalMS;
  private String sessionTimeoutMS;
  private String keyDeserializer;
  private String valueDeserializer;

  public ConsumerProperty() {
  }

  public ConsumerProperty(
      String bootstrapServers,
      String groupID,
      String enableAutoCommit,
      String autoCommitIntervalMS,
      String sessionTimeoutMS,
      String keyDeserializer,
      String valueDeserializer
  ) {
    this.bootstrapServers = bootstrapServers;
    this.groupID = groupID;
    this.enableAutoCommit = enableAutoCommit;
    this.autoCommitIntervalMS = autoCommitIntervalMS;
    this.sessionTimeoutMS = sessionTimeoutMS;
    this.keyDeserializer = keyDeserializer;
    this.valueDeserializer = valueDeserializer;
  }

  public static ConsumerProperty getDefault() {
    Config config = Configer.getConf();
    String bootstrapServers = config.getString(KAFKA_HOST) + config.getString(KAFKA_PORT);

    return new ConsumerProperty(
        bootstrapServers,
        DEFAULT_GROUP_ID,
        DEFAULT_ENABLE_AUTO_COMMIT,
        DEFAULT_AUTO_COMMIT_INTERVAL_MS,
        DEFAULT_SESSION_TIMEOUT_MS,
        DEFAULT_KEY_DESERIALIZER,
        DEFAULT_VALUE_DESERIALIZER
    );
  }

  public static String getDefaultGroupId() {
    return DEFAULT_GROUP_ID;
  }

  public static String getDefaultEnableAutoCommit() {
    return DEFAULT_ENABLE_AUTO_COMMIT;
  }

  public static String getDefaultAutoCommitIntervalMs() {
    return DEFAULT_AUTO_COMMIT_INTERVAL_MS;
  }

  public static String getDefaultSessionTimeoutMs() {
    return DEFAULT_SESSION_TIMEOUT_MS;
  }

  public static String getDefaultKeyDeserializer() {
    return DEFAULT_KEY_DESERIALIZER;
  }

  public static String getDefaultValueDeserializer() {
    return DEFAULT_VALUE_DESERIALIZER;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public String getGroupID() {
    return groupID;
  }

  public void setGroupID(String groupID) {
    this.groupID = groupID;
  }

  public String getEnableAutoCommit() {
    return enableAutoCommit;
  }

  public void setEnableAutoCommit(String enableAutoCommit) {
    this.enableAutoCommit = enableAutoCommit;
  }

  public String getAutoCommitIntervalMS() {
    return autoCommitIntervalMS;
  }

  public void setAutoCommitIntervalMS(String autoCommitIntervalMS) {
    this.autoCommitIntervalMS = autoCommitIntervalMS;
  }

  public String getSessionTimeoutMS() {
    return sessionTimeoutMS;
  }

  public void setSessionTimeoutMS(String sessionTimeoutMS) {
    this.sessionTimeoutMS = sessionTimeoutMS;
  }

  public String getKeyDeserializer() {
    return keyDeserializer;
  }

  public void setKeyDeserializer(String keyDeserializer) {
    this.keyDeserializer = keyDeserializer;
  }

  public String getValueDeserializer() {
    return valueDeserializer;
  }

  public void setValueDeserializer(String valueDeserializer) {
    this.valueDeserializer = valueDeserializer;
  }
}
