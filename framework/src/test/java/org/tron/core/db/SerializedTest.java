package org.tron.core.db;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;

public class SerializedTest {


  @Test
  public void SerializedSize() throws InvalidProtocolBufferException {
    // block header raw
    Protocol.BlockHeader.raw.Builder blockHeaderRawBuild = Protocol.BlockHeader.raw.newBuilder();
    Protocol.BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
            .setNumber(47377682)
            .setParentHash(ByteString.copyFrom(ByteArray.fromHexString(
                    "0000000002d2ed11cac713f84349dde3c1a4af38b8480fdb72c6c41a315abf25")))
            .setTimestamp(1672734363000L)
            .setVersion(26)
            .setWitnessAddress(ByteString.copyFrom(ByteArray.fromHexString(
                    "41af619f8ce75a9e95a19e851bebe63e89fcb1826e")))
            .setTxTrieRoot(ByteString.copyFrom(ByteArray.fromHexString(
                    "8b006c280022d325a1eb6a64f0b9ca6ce9e23bcc380fb07d58be397219f935b3")))
            .build();

    // block header
    Protocol.BlockHeader.Builder blockHeaderBuild = Protocol.BlockHeader.newBuilder();
    Protocol.BlockHeader blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw)
            .setWitnessSignature(ByteString.copyFrom(ByteArray.fromHexString(
                    "aae20ba4216a2797fd4ece92e9b0748a30bf547a58c03ba8e1b9c818ea8f24b12afd4da"
                            + "d6f6d246428d641d695d204d5749af93340da90d0f52b0436dd1853ae00")))
            .build();

    // block
    Protocol.Block.Builder blockBuild = Protocol.Block.newBuilder();

    blockBuild.setBlockHeader(blockHeader).build();
    long current = blockBuild.build().getSerializedSize();

    byte[] data = new byte[512 * 1_0240];

    for (int i = 0; i < 512 * 1_0240; i++) {
      data[i] = (byte) i;
    }
    Protocol.Transaction large = Protocol.Transaction.newBuilder().setRawData(
            Protocol.Transaction.raw.newBuilder().setData(
                    ByteString.copyFrom(data)).build()).build();
    Protocol.Transaction t = Protocol.Transaction.parseFrom(ByteString.copyFrom(
            ByteArray.fromHexString(
            "0aab010a080000018576b86dec220d3136373237333430373633393640ecdbe1b5d7305222544e66"
                    + "584555517359584355507641474e7448476b716d636e797050783947566e6a5a65080112610a"
                    + "2d747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572"
                    + "436f6e747261637412300a1541f08012b4881c320eb40b80f1228731898824e09d121541878e"
                    + "5427dec577ec5cb0bbcc6056837cb1926f2d1801124135eda8606f37f9d80625533832830853"
                    + "8b413ba3594a53d810deb3c657aa567e6e3fc52854737dc68f40d69e0e78e67df8edc508de43"
                    + "38c56d5acafb2797eec6002a021801")));
    Protocol.Transaction small = Protocol.Transaction.newBuilder().setRawData(
            Protocol.Transaction.raw.newBuilder().setData(
                    ByteString.copyFrom("1".getBytes(StandardCharsets.UTF_8))).build()).build();


    Protocol.Transaction min = Protocol.Transaction.getDefaultInstance();
    int c = 300;
    List<Protocol.Transaction> l = new ArrayList<>();
    l.add(large);
    l.add(t);
    l.add(small);
    l.add(min);
    Random r = new Random();
    for (int i = 0; i < c; i++) {
      Protocol.Transaction transaction = l.get(r.nextInt(l.size()));
      current += CodedOutputStream.computeMessageSize(1, transaction);
      blockBuild.addTransactions(transaction);
    }
    long aft = blockBuild.build().getSerializedSize();
    Assert.assertEquals(aft, current);

    Protocol.Account.Builder account = Protocol.Account.newBuilder();
    long base = account.build().getSerializedSize();
    Protocol.Vote vote = Protocol.Vote.newBuilder().setVoteAddress(ByteString.copyFrom("1".getBytes(
            StandardCharsets.UTF_8))).build();
    account.addVotes(vote);
    long v1 = vote.getSerializedSize();
    long a1 = account.build().getSerializedSize();
    Assert.assertEquals(a1 - base,
            CodedOutputStream.computeTagSize(5)
                    + CodedOutputStream.computeInt64SizeNoTag(v1) + v1);
    Protocol.Vote vote2 = Protocol.Vote.newBuilder().setVoteAddress(
            ByteString.copyFrom("2".getBytes(StandardCharsets.UTF_8))).build();
    account.addVotes(vote2);
    long v2 = vote2.getSerializedSize();
    long a2 = account.build().getSerializedSize();
    Assert.assertEquals(a2 - a1,
            CodedOutputStream.computeTagSize(5)
                    + CodedOutputStream.computeInt64SizeNoTag(v2) + v2);
    Protocol.Account.Frozen frozen = Protocol.Account.Frozen.newBuilder()
            .setFrozenBalance(1).build();
    long currentSize = a2;
    long cc = 0;
    for (int i = 0; i < 1000; i++) {
      if ((currentSize += CodedOutputStream.computeMessageSize(7, frozen)) > 1600) {
        break;
      }
      account.addFrozen(frozen);
      cc = i + 1;
    }

    long a3 = account.build().getSerializedSize();
    Assert.assertEquals((a3 - a2) / cc,
            CodedOutputStream.computeTagSize(7)
                    + CodedOutputStream.computeInt64SizeNoTag(frozen.getSerializedSize())
                    + frozen.getSerializedSize());

    Protocol.Permission p = Protocol.Permission.newBuilder().build();
    long p1 = p.getSerializedSize();
    account.addActivePermission(p);
    long a4 = account.build().getSerializedSize();
    Assert.assertEquals(a4 - a3, CodedOutputStream.computeTagSize(33)
                    + CodedOutputStream.computeInt64SizeNoTag(p1) + p1);
  }
}
