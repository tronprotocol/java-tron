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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.gossip.LocalMember;
import org.junit.Test;
import org.tron.common.overlay.gossip.LocalNode;
import org.tron.core.BlockUtils;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.protos.core.TronBlock;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class GossipTest {
    private static LocalNode standNode = LocalNode.getInstance();
    private Semaphore lock = new Semaphore(0);

    @Test
    public void testGossipBroadcast() throws InterruptedException {
        standNode.getGossipManager().registerSharedDataSubscriber((key, oldValue, newValue) -> {
            byte[] newValueBytes = null;
            try {
                newValueBytes = newValue.toString().getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            TronBlock.Block block = null;

            try {
                block = TronBlock.Block.parseFrom(newValueBytes);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }

            System.out.println(BlockUtils.toPrintString(block));
        });

        Message message = new BlockMessage(BlockUtils.newBlock(null, ByteString.copyFrom(new byte[]{1}), ByteString
                .copyFrom(new byte[]{2}), 3L));
        standNode.broadcast(message);
        lock.tryAcquire(10, TimeUnit.SECONDS);
    }

    @Test
    public void testGossipGetLiveMembers() {
        List<LocalMember> memberList = standNode.getLiveMembers();

        for (LocalMember l : memberList) {
            System.out.println(l);
        }
    }

    @Test
    public void testGossipGetDeadMembers() {
        List<LocalMember> memberList = standNode.getDeadMembers();

        for (LocalMember l : memberList) {
            System.out.println(l);
        }
    }
}
