package org.tron.core.admission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.admission.TransactionAdmissionException.Reason;
import org.tron.core.utils.TransactionRegister;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;

public class TransactionAdmissionGuardTest {

  private static final int LD = WireFormat.WIRETYPE_LENGTH_DELIMITED;
  private static final int VARINT = WireFormat.WIRETYPE_VARINT;
  private static final int VOTE = ContractType.VoteWitnessContract.getNumber();
  private static final int TRANSFER = ContractType.TransferContract.getNumber();

  @BeforeClass
  public static void registerActuators() {
    TransactionRegister.registerActuator();
  }

  // ---------------------------------------------------------------- outer envelope floods

  @Test
  public void everyOuterFloodShapeIsCountedPerOccurrence() {
    assertEquals(1000, countTx(repeat(bytes(0x2a, 0x00), 1000)));            // ret
    assertEquals(1000, countTx(repeat(bytes(0x12, 0x00), 1000)));            // empty signature
    assertEquals(1001, countTx(ld(1, repeat(bytes(0x5a, 0x00), 1000))));     // raw.contract
    assertEquals(1001, countTx(ld(1, repeat(bytes(0x4a, 0x00), 1000))));     // raw.auths
    assertEquals(1000, countTx(repeat(bytes(0x30, 0xff, 0x01), 1000)));      // same unknown field
    assertEquals(1000, countTx(distinctUnknownFields(100, 1000)));           // distinct unknown
  }

  @Test
  public void nestedMessagesAreRecursed() {
    // Result.orderDetails (26) inside one ret
    assertEquals(501, countTx(ld(5, repeat(bytes(0xd2, 0x01, 0x00), 500))));
    // Result.cancel_unfreezeV2_amount (28) map entries carrying unknown fields
    byte[] entry = ld(28, repeat(bytes(0x30, 0xff, 0x01), 10));
    assertEquals(1 + 50 * 11, countTx(ld(5, repeat(entry, 50))));
    // raw.auths -> authority.account -> AccountId carrying unknown fields
    assertEquals(103, countTx(ld(1, ld(9, ld(1, repeat(bytes(0x30, 0xff, 0x01), 100))))));
    // unknown fields inside Contract.parameter itself (outside value)
    assertEquals(103, countTx(ld(1, ld(11, ld(2, repeat(bytes(0x30, 0xff, 0x01), 100))))));
  }

  @Test
  public void budgetAllowsExactlyTheCount() {
    assertExactBudget(repeat(bytes(0x2a, 0x00), 1000));
    assertExactBudget(ld(1, repeat(bytes(0x5a, 0x00), 1000)));
    assertExactBudget(distinctUnknownFields(100, 1000));
    assertExactBudget(voteTx(VOTE, "protocol.VoteWitnessContract", 300));
  }

  // ---------------------------------------------------------------- Any.value payload

  @Test
  public void shieldedPayloadIsRecursedLikeGetOwnerUnpacksIt() {
    int shielded = ContractType.ShieldedTransferContract.getNumber();
    byte[] spends = repeat(bytes(0x1a, 0x00), 300);                    // spend_description
    byte[] tx = txWithContract(contract(shielded, "protocol.ShieldedTransferContract", spends));
    assertEquals(countTx(voteTx(VOTE, "protocol.VoteWitnessContract", 300)), countTx(tx));
    assertExactBudget(tx);
  }

  @Test
  public void contractPayloadIsScannedWithItsContractType() throws Exception {
    byte[] tx = voteTx(VOTE, "protocol.VoteWitnessContract", 1000);
    // raw, contract, type, parameter, type_url, value, then 1000 votes
    assertEquals(1006, countTx(tx));
    assertEquals(1000, unpack(tx, VoteWitnessContract.class).getVotesCount());

    byte[] actives = repeat(bytes(0x22, 0x00), 500);
    byte[] permissionTx = txWithContract(contract(
        ContractType.AccountPermissionUpdateContract.getNumber(),
        "protocol.AccountPermissionUpdateContract", actives));
    assertEquals(506, countTx(permissionTx));
    assertEquals(500, unpack(permissionTx, AccountPermissionUpdateContract.class)
        .getActivesCount());

    // CreateSmartContract.new_contract(2) -> SmartContract.abi(3) -> ABI.entrys(1)
    byte[] create = ld(2, ld(3, repeat(bytes(0x0a, 0x00), 500)));
    byte[] createTx = txWithContract(contract(
        ContractType.CreateSmartContract.getNumber(), "protocol.CreateSmartContract", create));
    assertEquals(508, countTx(createTx));
    assertEquals(500, unpack(createTx, CreateSmartContract.class)
        .getNewContract().getAbi().getEntrysCount());
  }

  @Test
  public void mergedParameterUsesOnlyTheEffectiveValue() throws Exception {
    // First parameter: type_url + malformed value; second parameter: only a valid value.
    // The merged Any keeps the first type_url and the second value, and unpacks fine.
    byte[] valid = TransferContract.newBuilder().setAmount(1).build().toByteArray();
    byte[] contract = cat(
        varintField(1, TRANSFER),
        ld(2, cat(ld(1, url("protocol.TransferContract")), ld(2, bytes(0x07)))),
        ld(2, ld(2, valid)));
    byte[] tx = txWithContract(contract);
    TransactionAdmissionGuard.checkTransaction(tx, 1000);
    assertEquals(1, unpack(tx, TransferContract.class).getAmount());

    // A flood in an overwritten value is a leaf; only the effective value is recursed.
    byte[] overwritten = cat(
        varintField(1, VOTE),
        ld(2, cat(ld(1, url("protocol.VoteWitnessContract")),
            ld(2, repeat(bytes(0x12, 0x00), 1000)))),
        ld(2, ld(2, repeat(bytes(0x12, 0x00), 2))));
    byte[] overwrittenTx = txWithContract(overwritten);
    // raw, contract, type, p1, url, v1, p2, v2, then 2 votes
    assertEquals(10, countTx(overwrittenTx));
    assertEquals(2, unpack(overwrittenTx, VoteWitnessContract.class).getVotesCount());
  }

  @Test
  public void explicitEmptyOverridesFollowTheParser() throws Exception {
    byte[] flood = repeat(bytes(0x12, 0x00), 100);
    byte[] firstParameter = ld(2, cat(ld(1, url("protocol.VoteWitnessContract")), ld(2, flood)));

    byte[] emptyValue = txWithContract(cat(varintField(1, VOTE), firstParameter,
        ld(2, bytes(0x12, 0x00))));
    assertEquals(8, countTx(emptyValue));
    assertTrue(Transaction.parseFrom(emptyValue).getRawData().getContract(0)
        .getParameter().getValue().isEmpty());

    byte[] emptyTypeUrl = txWithContract(cat(varintField(1, VOTE), firstParameter,
        ld(2, bytes(0x0a, 0x00))));
    assertEquals(8, countTx(emptyTypeUrl));
    assertEquals("", Transaction.parseFrom(emptyTypeUrl).getRawData().getContract(0)
        .getParameter().getTypeUrl());
  }

  @Test
  public void missingContractTypeDefaultsToZero() throws Exception {
    byte[] flood = repeat(bytes(0x30, 0xff, 0x01), 100);
    byte[] tx = txWithContract(ld(2, cat(
        ld(1, url("protocol.AccountCreateContract")), ld(2, flood))));
    assertEquals(ContractType.AccountCreateContract,
        Transaction.parseFrom(tx).getRawData().getContract(0).getType());
    // raw, contract, parameter, type_url, value, then 100 unknown fields in the payload
    assertEquals(105, countTx(tx));
    unpack(tx, AccountCreateContract.class);

    // Without '/', Any.is never matches, the payload is never unpacked and stays a leaf.
    byte[] noSlash = txWithContract(ld(2, cat(
        ld(1, "protocol.AccountCreateContract".getBytes()), ld(2, flood))));
    assertEquals(5, countTx(noSlash));
    try {
      unpack(noSlash, AccountCreateContract.class);
      fail("type url without '/' must not unpack");
    } catch (InvalidProtocolBufferException expected) {
      // expected
    }
  }

  @Test
  public void unknownContractTypeIsALeafAndDoesNotThrow() throws Exception {
    byte[] tx = voteTx(9999, "protocol.VoteWitnessContract", 100);
    assertEquals(ContractType.UNRECOGNIZED,
        Transaction.parseFrom(tx).getRawData().getContract(0).getType());
    assertEquals(6, countTx(tx));
  }

  @Test
  public void lastContractTypeWinsAndWrongWireTypeIsIgnored() throws Exception {
    byte[] parameter = ld(2, cat(ld(1, url("protocol.VoteWitnessContract")),
        ld(2, repeat(bytes(0x12, 0x00), 100))));

    // Last type is TransferContract: type_url does not match, payload stays a leaf.
    byte[] voteThenTransfer = txWithContract(cat(
        varintField(1, VOTE), varintField(1, TRANSFER), parameter));
    assertEquals(7, countTx(voteThenTransfer));

    byte[] transferThenVote = txWithContract(cat(
        varintField(1, TRANSFER), varintField(1, VOTE), parameter));
    assertEquals(107, countTx(transferThenVote));

    // Field 1 with the length-delimited wire type is an unknown field: type stays VOTE.
    byte[] wrongWireType = txWithContract(cat(varintField(1, VOTE), ld(1, new byte[0]),
        parameter));
    assertEquals(ContractType.VoteWitnessContract,
        Transaction.parseFrom(wrongWireType).getRawData().getContract(0).getType());
    // raw, contract, type, field 1 as length-delimited, parameter, type_url, value, 100 votes
    assertEquals(107, countTx(wrongWireType));

    // Parameter written before type.
    byte[] parameterFirst = txWithContract(cat(parameter, varintField(1, VOTE)));
    assertEquals(106, countTx(parameterFirst));
  }

  @Test
  public void contractsShareTheRootBudget() {
    byte[] one = contract(VOTE, "protocol.VoteWitnessContract", repeat(bytes(0x12, 0x00), 100));
    byte[] tx = ld(1, repeat(ld(11, one), 3));
    assertEquals(1 + 3 * 105, countTx(tx));
    assertExactBudget(tx);
  }

  @Test
  public void firstPassStopsAtTheRemainingBudget() {
    // 10000 unknown fields directly inside Contract are seen first by the first pass.
    byte[] tx = txWithContract(repeat(bytes(0x30, 0xff, 0x01), 10000));
    assertEquals(Reason.OVER_BUDGET,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, 100)));
  }

  @Test
  public void contractsOfDifferentTypesAreCountedExactly() throws Exception {
    byte[][] contracts = {
        contract(VOTE, "protocol.VoteWitnessContract", repeat(bytes(0x12, 0x00), 3)),
        contract(ContractType.AccountPermissionUpdateContract.getNumber(),
            "protocol.AccountPermissionUpdateContract", repeat(bytes(0x22, 0x00), 7)),
        contract(VOTE, "protocol.VoteWitnessContract", repeat(bytes(0x12, 0x00), 11))
    };
    long expected = 1 + 4;                 // raw_data and the four ret entries
    ByteArrayOutputStream raw = new ByteArrayOutputStream();
    for (byte[] contract : contracts) {
      expected += countTx(txWithContract(contract)) - 1;
      byte[] entry = ld(11, contract);
      raw.write(entry, 0, entry.length);
    }
    byte[] tx = cat(ld(1, raw.toByteArray()), repeat(bytes(0x2a, 0x00), 4));
    // A wrong payload offset in a later contract would leave its payload uncounted.
    assertEquals(expected, countTx(tx));
    assertExactBudget(tx);
    assertEquals(contracts.length, Transaction.parseFrom(tx).getRawData().getContractCount());
  }

  @Test
  public void malformedPayloadOfALaterContractIsFound() throws Exception {
    byte[] tx = ld(1, cat(
        ld(11, contract(VOTE, "protocol.VoteWitnessContract", repeat(bytes(0x12, 0x00), 10))),
        ld(11, contract(VOTE, "protocol.VoteWitnessContract", bytes(0x07)))));
    assertEquals(Reason.MALFORMED,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, 1000)));
    Transaction.raw raw = Transaction.parseFrom(tx).getRawData();
    raw.getContract(0).getParameter().unpack(VoteWitnessContract.class);
    try {
      raw.getContract(1).getParameter().unpack(VoteWitnessContract.class);
      fail("payload of the second contract must fail to unpack");
    } catch (InvalidProtocolBufferException expected) {
      // expected
    }
  }

  @Test
  public void protobufLookingBytesInsideBytesFieldsStayLeaves() throws Exception {
    // Tags of ret, contract, auths and a nested message, as opaque content.
    byte[] fake = repeat(bytes(0x2a, 0x00, 0x5a, 0x00, 0x4a, 0x00, 0x0a, 0x02, 0x2a, 0x00), 500);
    // raw.data (10), two signatures (2), Contract.provider (3) and ContractName (4)
    byte[] tx = cat(
        ld(1, cat(ld(10, fake), ld(11, cat(varintField(1, TRANSFER), ld(3, fake), ld(4, fake))))),
        ld(2, fake), ld(2, fake));
    // raw, data, contract, type, provider, ContractName, signature, signature
    assertEquals(8, countTx(tx));
    Transaction parsed = Transaction.parseFrom(tx);
    assertEquals(fake.length, parsed.getRawData().getData().size());
    assertEquals(0, parsed.getRetCount());
  }

  // ---------------------------------------------------------------- depth and recursive types

  @Test
  public void depthLimitAppliesToRecursiveTypes() {
    Descriptor recursive = DescriptorProto.getDescriptor();   // nested_type (3) is recursive
    TransactionAdmissionGuard.check(nested(TransactionAdmissionGuard.MAX_DEPTH - 1), recursive,
        1000);
    assertEquals(Reason.TOO_DEEP, reason(() -> TransactionAdmissionGuard.check(
        nested(TransactionAdmissionGuard.MAX_DEPTH), recursive, 1000)));
  }

  @Test(timeout = 60_000)
  public void descriptorCacheIsConsistentUnderConcurrentFirstUse() throws Exception {
    Descriptor fresh = FileDescriptorProto.getDescriptor();
    byte[] payload = FileDescriptorProto.newBuilder()
        .addMessageType(DescriptorProto.newBuilder().setName("a")
            .addNestedType(DescriptorProto.newBuilder().setName("b")))
        .build().toByteArray();
    ExecutorService pool = Executors.newFixedThreadPool(16);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Long>> results = new ArrayList<>();
      for (int i = 0; i < 64; i++) {
        results.add(pool.submit((Callable<Long>) () -> {
          start.await();
          return TransactionAdmissionGuard.count(payload, fresh);
        }));
      }
      start.countDown();
      long expected = TransactionAdmissionGuard.count(payload, fresh);
      for (Future<Long> result : results) {
        assertEquals(expected, result.get(10, TimeUnit.SECONDS).longValue());
      }
    } finally {
      pool.shutdownNow();
    }
  }

  // ---------------------------------------------------------------- boundaries and malformed

  @Test
  public void invalidLengthsAreMalformedNotInternalErrors() {
    // negative length on a nested message
    assertMalformedLikeParser(cat(tag(1, LD), varint(-1)));
    // length beyond the array
    assertMalformedLikeParser(cat(tag(1, LD), varint(100), bytes(0x08, 0x01)));
    // inside the array but beyond the parent: raw is 4 bytes, the contract claims 6
    assertMalformedLikeParser(bytes(0x0a, 0x04, 0x5a, 0x06, 0x08, 0x01, 0x2a, 0x00));
    // first pass, parameter with a negative length
    assertMalformedLikeParser(txWithContract(cat(varintField(1, VOTE), tag(2, LD),
        varint(-1))));
    // first pass, parameter beyond the contract: contract is 4 bytes, parameter claims 10
    assertMalformedLikeParser(cat(ld(1, cat(bytes(0x5a, 0x04), tag(2, LD), varint(10),
        bytes(0x0a, 0x00))), bytes(0x2a, 0x00)));
  }

  @Test
  public void malformedOuterInputIsRejectedLikeTheParser() {
    assertMalformedLikeParser(bytes(0x00));                 // tag zero
    assertMalformedLikeParser(bytes(0x08, 0x80));           // truncated varint
    assertMalformedLikeParser(bytes(0x2a, 0x05, 0x00));     // truncated message
  }

  @Test
  public void malformedEffectivePayloadIsRejectedWhileOuterParseSucceeds() throws Exception {
    byte[] tx = txWithContract(contract(VOTE, "protocol.VoteWitnessContract", bytes(0x07)));
    assertEquals(Reason.MALFORMED,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, 1000)));
    Transaction.parseFrom(tx);
    try {
      unpack(tx, VoteWitnessContract.class);
      fail("payload must fail to unpack");
    } catch (InvalidProtocolBufferException expected) {
      // expected
    }
  }

  @Test
  public void groupsAreRejectedAlthoughTheParserAcceptsThem() throws Exception {
    byte[] tx = bytes(0x0b, 0x0c);   // field 1 start group, field 1 end group
    Transaction.parseFrom(tx);
    assertEquals(Reason.MALFORMED,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, 1000)));
  }

  @Test
  public void exceptionCarriesNoStackTrace() {
    try {
      TransactionAdmissionGuard.checkTransaction(repeat(bytes(0x2a, 0x00), 10), 5);
      fail();
    } catch (TransactionAdmissionException e) {
      assertEquals(0, e.getStackTrace().length);
    }
  }

  // ---------------------------------------------------------------- legitimate corpus

  @Test
  public void legitimateTransactionsPassAndStillParse() throws Exception {
    List<Transaction> corpus = new ArrayList<>();
    corpus.add(signed(ContractType.TransferContract, TransferContract.newBuilder()
        .setOwnerAddress(address(1)).setToAddress(address(2)).setAmount(1_000_000).build(), 1)
        .toBuilder().addRet(Transaction.Result.newBuilder()
            .setContractRet(Transaction.Result.contractResult.SUCCESS)).build());

    VoteWitnessContract.Builder votes = VoteWitnessContract.newBuilder()
        .setOwnerAddress(address(1));
    for (int i = 0; i < 30; i++) {
      votes.addVotes(VoteWitnessContract.Vote.newBuilder()
          .setVoteAddress(address(10 + i)).setVoteCount(i + 1));
    }
    corpus.add(signed(ContractType.VoteWitnessContract, votes.build(), 1));

    AccountPermissionUpdateContract.Builder permissions = AccountPermissionUpdateContract
        .newBuilder().setOwnerAddress(address(1)).setOwner(permission(5));
    for (int i = 0; i < 8; i++) {
      permissions.addActives(permission(5));
    }
    corpus.add(signed(ContractType.AccountPermissionUpdateContract, permissions.build(), 5));

    ABI.Builder abi = ABI.newBuilder();
    for (int i = 0; i < 50; i++) {
      abi.addEntrys(ABI.Entry.newBuilder().setName("f" + i).setType(ABI.Entry.EntryType.Function)
          .addInputs(ABI.Entry.Param.newBuilder().setName("a").setType("uint256"))
          .addOutputs(ABI.Entry.Param.newBuilder().setName("b").setType("address")));
    }
    corpus.add(signed(ContractType.CreateSmartContract, CreateSmartContract.newBuilder()
        .setOwnerAddress(address(1)).setNewContract(SmartContract.newBuilder()
            .setName("c").setAbi(abi).setBytecode(ByteString.copyFrom(new byte[20_000])))
        .build(), 1));

    corpus.add(signed(ContractType.TriggerSmartContract, TriggerSmartContract.newBuilder()
        .setOwnerAddress(address(1)).setContractAddress(address(3))
        .setData(ByteString.copyFrom(new byte[10_000])).build(), 1));

    for (Transaction tx : corpus) {
      byte[] bytes = tx.toByteArray();
      TransactionAdmissionGuard.checkTransaction(bytes, 10_000);
      Transaction.Contract contract = Transaction.parseFrom(bytes).getRawData().getContract(0);
      contract.getParameter().unpack(TransactionFactory.getContract(contract.getType()));
    }
  }

  // A packed repeated scalar is one occurrence however many elements it holds. None is
  // reachable today; a schema change that adds one must revisit the budget.
  @Test
  public void noPackedRepeatedScalarIsReachable() {
    Deque<Descriptor> pending = new ArrayDeque<>();
    pending.add(Transaction.getDescriptor());
    pending.add(ShieldedTransferContract.getDescriptor());
    for (ContractType type : ContractType.values()) {
      if (type == ContractType.UNRECOGNIZED) {
        continue;
      }
      Class<? extends GeneratedMessageV3> clazz = TransactionFactory.getContract(type);
      if (clazz != null) {
        pending.add(Internal.getDefaultInstance(clazz).getDescriptorForType());
      }
    }
    Set<Descriptor> seen = new HashSet<>();
    List<String> packable = new ArrayList<>();
    while (!pending.isEmpty()) {
      Descriptor descriptor = pending.poll();
      if (!seen.add(descriptor)) {
        continue;
      }
      for (FieldDescriptor field : descriptor.getFields()) {
        if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
          pending.add(field.getMessageType());
        } else if (field.isPackable()) {
          packable.add(field.getFullName());
        }
      }
    }
    assertTrue(seen.size() > 50);
    assertEquals(new ArrayList<String>(), packable);
  }

  @Test
  public void lastTypeUrlDecidesThePayload() throws Exception {
    byte[] flood = repeat(bytes(0x12, 0x00), 1000);
    byte[] contract = cat(varintField(1, VOTE),
        ld(2, cat(ld(1, url("protocol.Bogus")), ld(1, url("protocol.VoteWitnessContract")),
            ld(2, flood))));
    byte[] tx = txWithContract(contract);
    assertEquals(1000, unpack(tx, VoteWitnessContract.class).getVotesCount());
    assertEquals(Reason.OVER_BUDGET,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, 500)));
  }

  @Test
  public void typeUrlIsMatchedLikeAnyIs() throws Exception {
    String name = "protocol.VoteWitnessContract";
    String[] expanded = {
        "type.googleapis.com/x/" + name,
        "a/b/c/" + name,
        "/" + name,
    };
    String[] leaves = {
        name,
        "type.googleapis.com/" + name + "/x",
        "type.googleapis.com/" + name + "/",
        "type.googleapis.com/" + name.substring(0, name.length() - 1),
        "type.googleapis.com/" + name + "X",
        "type.googleapis.com/x" + name,
        "type.googleapis.com/protocol.votewitnesscontract",
    };
    for (String typeUrl : expanded) {
      byte[] tx = voteTxWithTypeUrl(typeUrl, 100);
      assertTrue(typeUrl, Transaction.parseFrom(tx).getRawData().getContract(0).getParameter()
          .is(VoteWitnessContract.class));
      assertEquals(typeUrl, 106, countTx(tx));
    }
    for (String typeUrl : leaves) {
      byte[] tx = voteTxWithTypeUrl(typeUrl, 100);
      assertFalse(typeUrl, Transaction.parseFrom(tx).getRawData().getContract(0).getParameter()
          .is(VoteWitnessContract.class));
      assertEquals(typeUrl, 6, countTx(tx));
    }
  }

  @Test
  public void firstPassStopsAtTheBudgetBeforeLaterBytes() {
    // The truncated varint at the end is only reached if the first pass ignores the budget.
    byte[] tx = txWithContract(cat(repeat(bytes(0x30, 0xff, 0x01), 10000), bytes(0x30, 0x80)));
    assertEquals(Reason.OVER_BUDGET,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, 100)));
  }

  @Test
  public void productionBudgetIsPinned() {
    assertEquals(50_000, TransactionAdmissionGuard.MAX_TX_OCCURRENCES);
  }

  @Test
  public void largeValidAbiDeployIsAcceptedUpToTheBudgetAndRejectedBeyond() throws Exception {
    int budget = TransactionAdmissionGuard.MAX_TX_OCCURRENCES;
    // Largest ABI of ordinary entries that still fits the budget.
    int low = 0;
    int high = budget;
    while (low < high) {
      int mid = (low + high + 1) >>> 1;
      if (countTx(deployWithAbi(mid).toByteArray()) <= budget) {
        low = mid;
      } else {
        high = mid - 1;
      }
    }
    byte[] within = deployWithAbi(low).toByteArray();
    byte[] beyond = deployWithAbi(low + 1).toByteArray();
    assertTrue(countTx(within) <= budget);
    assertTrue(countTx(beyond) > budget);

    TransactionAdmissionGuard.checkTransaction(within, budget);
    assertEquals(low, unpack(within, CreateSmartContract.class)
        .getNewContract().getAbi().getEntrysCount());
    // Policy: a deploy whose ABI is beyond the budget is refused at the API.
    assertEquals(Reason.OVER_BUDGET,
        reason(() -> TransactionAdmissionGuard.checkTransaction(beyond, budget)));
  }

  // ---------------------------------------------------------------- helpers

  private static long countTx(byte[] tx) {
    return TransactionAdmissionGuard.countTransaction(tx);
  }

  private static void assertExactBudget(byte[] tx) {
    long count = countTx(tx);
    TransactionAdmissionGuard.checkTransaction(tx, (int) count);
    assertEquals(Reason.OVER_BUDGET,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, (int) count - 1)));
  }

  private static void assertMalformedLikeParser(byte[] tx) {
    assertEquals(Reason.MALFORMED,
        reason(() -> TransactionAdmissionGuard.checkTransaction(tx, 1000)));
    try {
      Transaction.parseFrom(tx);
      fail("parser must reject it too");
    } catch (InvalidProtocolBufferException expected) {
      // expected
    }
  }

  private static Reason reason(Runnable action) {
    try {
      action.run();
    } catch (TransactionAdmissionException e) {
      return e.getReason();
    }
    fail("expected TransactionAdmissionException");
    return null;
  }

  private static <T extends com.google.protobuf.Message> T unpack(byte[] tx, Class<T> clazz)
      throws InvalidProtocolBufferException {
    return Transaction.parseFrom(tx).getRawData().getContract(0).getParameter().unpack(clazz);
  }

  private static byte[] voteTx(int type, String typeName, int votes) {
    return txWithContract(contract(type, typeName, repeat(bytes(0x12, 0x00), votes)));
  }

  private static byte[] voteTxWithTypeUrl(String typeUrl, int votes) {
    return txWithContract(cat(varintField(1, VOTE), ld(2, cat(ld(1, typeUrl.getBytes()),
        ld(2, repeat(bytes(0x12, 0x00), votes))))));
  }

  private static byte[] contract(int type, String typeName, byte[] value) {
    return cat(varintField(1, type), ld(2, cat(ld(1, url(typeName)), ld(2, value))));
  }

  private static byte[] txWithContract(byte[] contract) {
    return ld(1, ld(11, contract));
  }

  private static byte[] url(String typeName) {
    return ("type.googleapis.com/" + typeName).getBytes();
  }

  private static byte[] nested(int levels) {
    byte[] body = new byte[0];
    for (int i = 0; i < levels; i++) {
      body = ld(3, body);
    }
    return body;
  }

  private static byte[] distinctUnknownFields(int firstNumber, int count) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int i = 0; i < count; i++) {
      byte[] field = varintField(firstNumber + i, 200);
      out.write(field, 0, field.length);
    }
    return out.toByteArray();
  }

  private static Transaction signed(ContractType type, com.google.protobuf.Message contract,
      int signatures) {
    Transaction.Builder builder = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .setRefBlockBytes(ByteString.copyFrom(new byte[2]))
        .setRefBlockHash(ByteString.copyFrom(new byte[8]))
        .setExpiration(1_800_000_000_000L).setTimestamp(1_700_000_000_000L)
        .addContract(Transaction.Contract.newBuilder().setType(type)
            .setParameter(Any.pack(contract))));
    for (int i = 0; i < signatures; i++) {
      builder.addSignature(ByteString.copyFrom(new byte[65]));
    }
    return builder.build();
  }

  private static Transaction deployWithAbi(int entries) {
    ABI.Builder abi = ABI.newBuilder();
    for (int i = 0; i < entries; i++) {
      abi.addEntrys(ABI.Entry.newBuilder().setName("f" + i).setType(ABI.Entry.EntryType.Function)
          .addInputs(ABI.Entry.Param.newBuilder().setName("a").setType("uint256"))
          .addOutputs(ABI.Entry.Param.newBuilder().setName("b").setType("bool")));
    }
    return signed(ContractType.CreateSmartContract, CreateSmartContract.newBuilder()
        .setOwnerAddress(address(1)).setNewContract(SmartContract.newBuilder()
            .setName("c").setAbi(abi).setBytecode(ByteString.copyFrom(new byte[1_000])))
        .build(), 1);
  }

  private static Permission permission(int keys) {
    Permission.Builder permission = Permission.newBuilder().setPermissionName("p")
        .setThreshold(keys);
    for (int i = 0; i < keys; i++) {
      permission.addKeys(Key.newBuilder().setAddress(address(100 + i)).setWeight(1));
    }
    return permission.build();
  }

  private static ByteString address(int seed) {
    byte[] address = new byte[21];
    address[0] = 0x41;
    address[20] = (byte) seed;
    return ByteString.copyFrom(address);
  }

  private static byte[] varintField(int field, long value) {
    return cat(tag(field, VARINT), varint(value));
  }

  private static byte[] ld(int field, byte[] body) {
    return cat(tag(field, LD), varint(body.length), body);
  }

  private static byte[] tag(int field, int wireType) {
    return varint((((long) field) << 3) | wireType);
  }

  private static byte[] varint(long value) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    while ((value & ~0x7FL) != 0) {
      out.write((int) ((value & 0x7F) | 0x80));
      value >>>= 7;
    }
    out.write((int) value);
    return out.toByteArray();
  }

  private static byte[] repeat(byte[] unit, int times) {
    byte[] out = new byte[unit.length * times];
    for (int i = 0; i < times; i++) {
      System.arraycopy(unit, 0, out, i * unit.length, unit.length);
    }
    return out;
  }

  private static byte[] cat(byte[]... parts) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      out.write(part, 0, part.length);
    }
    return out.toByteArray();
  }

  private static byte[] bytes(int... values) {
    byte[] out = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = (byte) values[i];
    }
    return out;
  }
}
