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
package org.tron.core.net.node;

import com.google.protobuf.ByteString;

import org.junit.Test;
import org.tron.core.BlockUtils;
import org.tron.core.TransactionUtils;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransationMessage;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class NodeTest {
    private Semaphore lock = new Semaphore(0);

    @Test
    public void testNode() throws InterruptedException {
        Node node = new Node();
        node.setNodeDelegate(new NodeImpl());
        node.start();

        lock.tryAcquire(1, TimeUnit.SECONDS);
        Message messageBlock = new BlockMessage(BlockUtils.newBlock(null, ByteString.copyFrom(new byte[]{1}),
                ByteString
                        .copyFrom(new byte[]{2}), 3L));

        node.broadcast(messageBlock);
        lock.tryAcquire(1, TimeUnit.SECONDS);

        Message messageTransaction = new TransationMessage(TransactionUtils.newCoinbaseTransaction("12", "", 0));

        node.broadcast(messageTransaction);
        lock.tryAcquire(1, TimeUnit.SECONDS);
    }
}
