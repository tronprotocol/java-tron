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
package org.tron.peer;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.config.Configer;
import org.tron.core.*;
import org.tron.crypto.ECKey;
import org.tron.overlay.Net;
import org.tron.overlay.kafka.Kafka;
import org.tron.overlay.listener.ReceiveSource;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;
import org.tron.protos.core.TronBlock;
import org.tron.protos.core.TronTransaction;
import org.tron.utils.ByteArray;
import org.tron.wallet.Wallet;

import java.util.Arrays;

import static org.tron.core.Constant.TOPIC_BLOCK;
import static org.tron.core.Constant.TOPIC_TRANSACTION;

public class Peer {
    public final static String PEER_NORMAL = "normal";
    public final static String PEER_SERVER = "server";

    private static Peer INSTANCE = null;

    private String type;

    private Peer() {
        init();
        source.addReceiveListener((Message message) -> {
            if (message.getType() == Type.BLOCK) {
                TronBlock.Block block = null;
                try {
                    block = TronBlock.Block.parseFrom(ByteArray.fromHexString(message.getMessage()));
                    blockchain.receiveBlock(block, utxoSet);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        });
        source.addReceiveListener((Message message) -> {
            if (message.getType() == Type.TRANSACTION) {
                try {
                    TronTransaction.Transaction transaction = TronTransaction.Transaction.parseFrom(ByteArray
                            .fromHexString(message.getMessage()));
                    System.out.println(TransactionUtils.toPrintString(transaction));
                    PendingStateImpl pendingState = (PendingStateImpl) blockchain.getPendingState();
                    pendingState.addPendingTransaction(blockchain, transaction, net);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static Peer getInstance(String type) {
        if (INSTANCE == null) {
            INSTANCE = new Peer();
            INSTANCE.type = type;
        }

        return INSTANCE;
    }

    private final ECKey myKey = Configer.getMyKey();

    private Wallet wallet = null;

    private Blockchain blockchain = null;

    private UTXOSet utxoSet = null;

    private ReceiveSource source;

    private Net net;

    private void init() {
        initWallet();

        initBlockchain();

        initUTXOSet();

        initNet();
    }

    private void initWallet() {
        wallet = new Wallet();
        wallet.init(myKey);
    }

    private void initBlockchain() {
       blockchain = new Blockchain(ByteArray.toHexString(wallet.getAddress()));
    }

    private void initUTXOSet() {
        utxoSet = new UTXOSet();
        utxoSet.setBlockchain(blockchain);
        utxoSet.reindex();
    }

    private void initNet() {
        source = new ReceiveSource();

        net = new Kafka(source, Arrays.asList(TOPIC_BLOCK, TOPIC_TRANSACTION));
    }

    public ECKey getMyKey() {
        return myKey;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public UTXOSet getUTXOSet() {
        return utxoSet;
    }

    public void setUTXOSet(UTXOSet utxoSet) {
        this.utxoSet = utxoSet;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Net getNet() {
        return net;
    }

    public void setNet(Net net) {
        this.net = net;
    }
}
