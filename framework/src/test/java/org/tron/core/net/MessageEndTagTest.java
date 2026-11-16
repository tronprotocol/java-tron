package org.tron.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.protos.Protocol.PBFTMessage;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transactions;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class MessageEndTagTest extends BaseMethodTest {

  private static Transaction sampleTransaction() {
    return Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().addContract(Transaction.Contract.newBuilder()
            .setType(Transaction.Contract.ContractType.TriggerSmartContract)
            .setParameter(Any.pack(TriggerSmartContract.getDefaultInstance()))))
        .addSignature(ByteString.copyFrom(new byte[] {0x0c})).build();
  }

  private static List<byte[]> malformedMessages(byte[] canonical) {
    List<byte[]> messages = new ArrayList<>();
    for (byte[] suffix : new byte[][] {{0x0c}, {0x14}, {0x0c, 0x08, 0x01}}) {
      byte[] malformed = Arrays.copyOf(canonical, canonical.length + suffix.length);
      System.arraycopy(suffix, 0, malformed, canonical.length, suffix.length);
      messages.add(malformed);
    }
    return messages;
  }

  @Test
  public void transactionCapsuleChecksEndTagWithEitherFilterSetting() throws Exception {
    Transaction transaction = sampleTransaction();
    byte[] canonical = transaction.toByteArray();
    assertEquals(0x0c, canonical[canonical.length - 1]);
    for (int filter : new int[] {0, 1}) {
      chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(filter);
      assertEquals(transaction, new TransactionCapsule(canonical).getInstance());
      for (byte[] malformed : malformedMessages(canonical)) {
        assertThrows(BadItemException.class, () -> new TransactionCapsule(malformed));
      }
    }
  }

  @Test
  public void transactionCapsulePreservesUnknownFieldFiltering() throws Exception {
    Transaction transaction = sampleTransaction();
    Transaction withUnknown = transaction.toBuilder()
        .setUnknownFields(UnknownFieldSet.newBuilder().addField(100,
            UnknownFieldSet.Field.newBuilder().addVarint(7).build()).build())
        .build();
    for (int filter : new int[] {0, 1}) {
      chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(filter);
      TransactionCapsule capsule = new TransactionCapsule(withUnknown.toByteArray());
      assertEquals(filter == 0 ? withUnknown : transaction, capsule.getInstance());
    }
  }

  @Test
  public void transactionChecksEndTagWithEitherFilterSetting() throws Exception {
    Transaction transaction = sampleTransaction();
    byte[] canonical = transaction.toByteArray();
    // A legitimate 0x0c inside the signature must remain accepted.
    assertEquals(0x0c, canonical[canonical.length - 1]);
    for (int filter : new int[] {0, 1}) {
      chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(filter);
      assertEquals(transaction,
          new TransactionMessage(canonical).getTransactionCapsule().getInstance());
      for (byte[] malformed : malformedMessages(canonical)) {
        assertThrows(InvalidProtocolBufferException.class, () -> new TransactionMessage(malformed));
      }
    }
  }

  @Test
  public void transactionsCheckEndTagWithEitherFilterSetting() throws Exception {
    Transactions transactions = Transactions.newBuilder()
        .addTransactions(sampleTransaction()).build();
    byte[] canonical = transactions.toByteArray();
    assertEquals(0x0c, canonical[canonical.length - 1]);
    for (int filter : new int[] {0, 1}) {
      chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(filter);
      assertEquals(transactions, new TransactionsMessage(canonical).getTransactions());
      for (byte[] malformed : malformedMessages(canonical)) {
        assertThrows(InvalidProtocolBufferException.class,
            () -> new TransactionsMessage(malformed));
      }
    }
  }

  @Test
  public void pbftChecksEndTagWithEitherFilterSetting() throws Exception {
    PBFTMessage message = PBFTMessage.newBuilder()
        .setRawData(PBFTMessage.Raw.newBuilder().setViewN(1))
        .setSignature(ByteString.copyFrom(new byte[] {0x0c})).build();
    byte[] canonical = message.toByteArray();
    assertEquals(0x0c, canonical[canonical.length - 1]);
    for (int filter : new int[] {0, 1}) {
      chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(filter);
      assertEquals(message, new PbftMessage(canonical).getPbftMessage());
      for (byte[] malformed : malformedMessages(canonical)) {
        assertThrows(InvalidProtocolBufferException.class, () -> new PbftMessage(malformed));
      }
    }
  }
}
