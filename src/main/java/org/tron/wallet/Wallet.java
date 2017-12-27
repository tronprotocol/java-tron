package org.tron.wallet;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.UTXOProvider;
import org.tron.crypto.ECKey;
import org.tron.protos.core.TronTXOutput;
import org.tron.utils.ByteArray;
import org.tron.utils.Threading;
import org.tron.utils.Utils;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

public class Wallet {
    private static final Logger logger = LoggerFactory.getLogger("wallet");

    protected final ReentrantLock lock = Threading.lock("wallet");

    @Nullable
    private volatile UTXOProvider vUTXOProvider;

    protected final HashSet<TronTXOutput> myUnspents = Sets.newHashSet();

    private ECKey ecKey;
    private byte[] address;

    /**
     * get a new wallet key
     */
    public void init() {
        this.ecKey = new ECKey(Utils.getRandom());
        address = this.ecKey.getAddress();
    }

    /**
     * get a wallet by the key
     *
     * @param ecKey keypair
     */
    public void init(ECKey ecKey) {
        this.ecKey = ecKey;
        address = this.ecKey.getAddress();

        logger.info("wallet address: {}", ByteArray.toHexString(address));
    }

    public ECKey getEcKey() {
        return ecKey;
    }

    public void setEcKey(ECKey ecKey) {
        this.ecKey = ecKey;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }


    public enum BalanceType {

        /**
         * Balance calculated assuming all pending transactions are in fact included into the best chain by miners.
         * This includes the value of immature coinbase transactions.
         */
        ESTIMATED,

        /**
         * Balance that could be safely used to create new spends, if we had all the needed private keys. This is
         * whatever the default coin selector would make available, which by default means transaction outputs with at
         * least 1 confirmation and pending transactions created by our own wallet which have been propagated across
         * the network. Whether we <i>actually</i> have the private keys or not is irrelevant for this balance type.
         */
        AVALIABLE,

        /**
         * Same as ESTIMATED but only for outputs we have the private keys for and can sign ourselves.
         */
        ESTIMATED_EPENDABLE,

        /**
         * Same as AVAILABLE but only for outputs we have the private keys for and can sign ourselves.
         */
        AVAILABLE_SPENDABLE
    }

//    public Coin getBalance() {
//        return getBalance(BalanceType.AVALIABLE);
//    }
//
//    public Coin getBalance(BalanceType balanceType) {
//        lock.lock();
//        try {
//            if (balanceType == BalanceType.AVALIABLE || balanceType == BalanceType.AVAILABLE_SPENDABLE) {
//                List<TronTXOutput> candidates;
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    public List<TronTXOutput> calculateAllSpendCandidates(boolean excludeImmatureCoinbases, boolean excludeUnsignable) {
//        lock.lock();
//        try {
//            List<TronTXOutput> candidates;
//            if (vUTXOProvider == null) {
//                candidates = new ArrayList<>(myUnspents.size());
//                for (TronTXOutput output : myUnspents) {
//
//                }
//            }
//
//        } finally {
//            lock.unlock();
//        }
//    }

    /**
     * Return true if this wallet has at least one of the private keys needed to sign for this scriptPubKey.
     * Return false if the form of the script is not know or if the script is  OP_RETURN
     */
//    public boolean canSignFor(Script script) {
//        if(script.isSentToRawPubKey()){
//
//        }
//    }
}
