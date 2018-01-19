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

import static org.tron.core.Constant.TOPIC_BLOCK;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.Type;

public class ConsumerWorker implements Runnable {

  private ConsumerRecord<String, String> record;
  private Kafka kafka;

  public ConsumerWorker(ConsumerRecord<String, String> record, Kafka kafka) {
    this.record = record;
    this.kafka = kafka;
  }

  @Override
  public void run() {
    Message message = new Message();
    message.setType(record.topic().equals(TOPIC_BLOCK) ? Type.BLOCK : Type.TRANSACTION);
    message.setMessage(record.value());

    kafka.deliver(message);
  }
}
