package org.tron.common.runtime;

import com.google.protobuf.ByteString;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.Wallet;
import org.tron.core.actuator.VMActuator;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionContext;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.state.WorldStateCallBack;
import org.tron.core.state.trie.TrieImpl2;
import org.tron.core.store.StoreFactory;
import org.tron.protos.Protocol;

public class VmStateTestUtil {

  private static String OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";

  public static ProgramResult runConstantCall(
      ChainBaseManager chainBaseManager, WorldStateCallBack worldStateCallBack, Protocol.Transaction tx)
      throws ContractExeException, ContractValidateException {
    BlockCapsule block = mockBlock(chainBaseManager, worldStateCallBack);
    TransactionCapsule trxCap = new TransactionCapsule(tx);
    TransactionContext context = new TransactionContext(block, trxCap,
        StoreFactory.getInstance(), true, false);
    VMActuator vmActuator = new VMActuator(true);

    vmActuator.validate(context);
    vmActuator.execute(context);
    return context.getProgramResult();
  }

  public static BlockCapsule mockBlock(ChainBaseManager chainBaseManager, WorldStateCallBack worldStateCallBack) {
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1000);
    TrieImpl2 trie = flushTrie(worldStateCallBack);
    BlockCapsule blockCap = new BlockCapsule(Protocol.Block.newBuilder().build());
    Protocol.BlockHeader.raw blockHeaderRaw =
        blockCap.getInstance().getBlockHeader().getRawData().toBuilder()
            .setNumber(100)
            .setTimestamp(System.currentTimeMillis())
            .setWitnessAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .build();
    Protocol.BlockHeader blockHeader = blockCap.getInstance().getBlockHeader().toBuilder()
        .setRawData(blockHeaderRaw)
        .setArchiveRoot(ByteString.copyFrom(trie.getRootHash()))
        .build();
    blockCap = new BlockCapsule(
        blockCap.getInstance().toBuilder().setBlockHeader(blockHeader).build());
    blockCap.generatedByMyself = true;
    return blockCap;
  }

  public static TrieImpl2 flushTrie(WorldStateCallBack worldStateCallBack) {
    worldStateCallBack.clear();
    TrieImpl2 trie = worldStateCallBack.getTrie();
    trie.commit();
    trie.flush();
    return trie;
  }
}
