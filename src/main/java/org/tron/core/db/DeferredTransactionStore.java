package org.tron.core.db;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DeferredTransactionCapsule;

import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "DB")
@Component
public class DeferredTransactionStore extends TronStoreWithRevoking<DeferredTransactionCapsule>  {
    @Autowired
    private DeferredTransactionStore(@Value("deferred_transaction") String dbName) {
        super(dbName);
    }

    public void put(DeferredTransactionCapsule deferredTransactionCapsule){
        byte[] trxId = deferredTransactionCapsule.getTransactionId().toByteArray();
        super.put(trxId, deferredTransactionCapsule);
    }

    public DeferredTransactionCapsule getByTransactionId(ByteString transactionId){
        DeferredTransactionCapsule deferredTransactionCapsule = null;

        try{
            byte[] key = transactionId.toByteArray();
            byte[] value = revokingDB.getUnchecked(key);
            if (ArrayUtils.isEmpty(value)) {
                return null;
            }

            deferredTransactionCapsule = new DeferredTransactionCapsule(value);
        }
        catch (Exception e){
            logger.error("{}", e);
        }

        return deferredTransactionCapsule;
    }

    public List<DeferredTransactionCapsule> getScheduledTransaction(){
        List<DeferredTransactionCapsule> deferredTransactionList = new ArrayList<>();
        return deferredTransactionList;
    }

}
