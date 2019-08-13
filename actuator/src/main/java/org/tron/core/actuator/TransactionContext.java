package org.tron.core.actuator;

import com.google.protobuf.Any;
import org.tron.core.store.StoreFactory;
import org.tron.protos.Protocol.Block;

public class TransactionContext {
    private Block block;
    private Any contract;
    private StoreFactory storeFactory;
}
