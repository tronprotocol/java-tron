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

package org.tron.overlay.kafka;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaTest {
  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testKafka() {
//        ReceiveSource source = new ReceiveSource();
//        source.addReceiveListener((Message message) -> {
//            if (message.getType() == Type.BLOCK) {
//                System.out.println(message.getMessage());
//            }
//        });
//
//        source.addReceiveListener((Message message) -> {
//            if (message.getType() == Type.TRANSACTION) {
//                System.out.println(message.getMessage());
//            }
//        });
//
//        Net net = new Kafka(source, Arrays.asList(TOPIC_BLOCK, TOPIC_TRANSACTION));
//
//        net.broadcast(new Message("hello block", Type.BLOCK));
//        net.broadcast(new Message("hello transaction", Type.TRANSACTION));
//
//        long startTime = System.currentTimeMillis();
//        long endTime = System.currentTimeMillis();
//
//        while (endTime - startTime < 50000) {
//            endTime = System.currentTimeMillis();
//        }
  }
}
