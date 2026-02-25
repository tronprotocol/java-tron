package org.tron.core.jsonrpc;

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
    // Use raw hex bytes to avoid dependency on CommonParameter initialization
    // TEPRbQxXQEpHpeEx8tK5xHVs7NWudAAZgu
    byte[] address = ByteArray.fromHexString(
        "413074ff6d53db268d23bd6013ec5497c8b13400ff");
    // TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf (nile usdt)
    byte[] contractAddress = ByteArray.fromHexString(
        "41eca9bc828a3005b9a3b909f2cc5c2a54794de05f");
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
