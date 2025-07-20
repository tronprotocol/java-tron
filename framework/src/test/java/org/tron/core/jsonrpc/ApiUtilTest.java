package org.tron.core.jsonrpc;

import static org.tron.common.utils.Commons.decodeFromBase58Check;
import static org.tron.keystore.Wallet.generateRandomBytes;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.jsonrpc.JsonRpcApiUtil;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.BlockHeader.raw;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class ApiUtilTest {

  @Test
  public void testGetBlockID() {
    byte[] mockedHash = generateRandomBytes(128);
    // common parent block
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(
            raw.newBuilder().setParentHash(ByteString.copyFrom(mockedHash))
                .setNumber(0))).build());
    String blockIdStr = JsonRpcApiUtil.getBlockID(blockCapsule.getInstance());
    Assert.assertEquals(2 + 64, blockIdStr.length());
  }

  @Test
  public void testTriggerCallContract() {
    byte[] address = decodeFromBase58Check("TEPRbQxXQEpHpeEx8tK5xHVs7NWudAAZgu");
    //nile usdt
    byte[] contractAddress = decodeFromBase58Check("TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf");
    long callValue = 100;
    //transfer to address TVjsyZ7fYF3qLF6BQgPmTEZy1xrNNyVAAA with 10*10^6
    byte[] data = ByteArray.fromHexString("a9059cbb000000000000000000000000d8dd39e2dea27a4000"
        + "1884901735e3940829bb440000000000000000000000000000000000000000000000000000000000989680");
    long tokenValue = 10;
    String tokenId = "1000001";
    TriggerSmartContract triggerSmartContract = JsonRpcApiUtil.triggerCallContract(address,
        contractAddress, callValue, data, tokenValue, tokenId);
    Assert.assertNotNull(triggerSmartContract);
  }
}
