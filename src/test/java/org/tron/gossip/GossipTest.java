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

package org.tron.gossip;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.overlay.example.LocalNode;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.Type;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class GossipTest {
    private final static String CLUSTER = "mycluster";
    private static LocalNode standNode = null;
    private Semaphore lock = new Semaphore(0);
    private Object sharedData = null;

    @BeforeClass
    public static void init() {
        standNode = new LocalNode(CLUSTER, "udp://localhost:10000", "0");
    }

    @Test
    public void testGossipBroadcast() throws InterruptedException {
        standNode.getGossipManager().registerSharedDataSubscriber((key, oldValue, newValue) -> {
            if (key.equals("block")) {
                sharedData = newValue;
            }
        });

        Message message = new Message("test", Type.BLOCK);
        standNode.broadcast(message);
        lock.tryAcquire(10, TimeUnit.SECONDS);
        Assert.assertEquals("test", sharedData);
    }
}
