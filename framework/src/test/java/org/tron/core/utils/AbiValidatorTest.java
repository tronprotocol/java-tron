package org.tron.core.utils;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.EntryType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.Param;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType;

public class AbiValidatorTest {

  @Test
  public void emptyAbiIsAccepted() throws Exception {
    AbiValidator.validate(null);
    AbiValidator.validate(ABI.getDefaultInstance());
  }

  @Test
  public void canonicalAbiIsAccepted() throws Exception {
    ABI abi = ABI.newBuilder()
        .addEntrys(function("transfer",
            param("to", "address"), param("value", "uint256"))
            .toBuilder().addOutputs(param("", "bool")).build())
        .addEntrys(event("Transfer", false,
            indexedParam("from", "address"),
            indexedParam("to", "address"),
            param("value", "uint256")))
        .addEntrys(anonymousEvent("",
            param("data", "bytes32")))
        .addEntrys(constructor(param("owner", "address")))
        .addEntrys(fallback())
        .addEntrys(receive())
        .addEntrys(function("mix",
            param("a", "uint8"), param("b", "int256"), param("c", "bytes1"),
            param("d", "bytes32"), param("e", "tuple"), param("f", "address[]"),
            param("g", "uint256[3]"), param("h", "trcToken")))
        .build();
    AbiValidator.validate(abi);
  }

  @Test
  public void shorthandUintIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(
        function("foo", param("x", "uint"))).build();
    expect(abi, "shorthand uint/int");
  }

  @Test
  public void shorthandIntIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(
        function("foo", param("x", "int"))).build();
    expect(abi, "shorthand uint/int");
  }

  @Test
  public void invalidIntWidthIsRejected() {
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "uint7"))).build(), "integer width");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "uint257"))).build(), "integer width");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "uint12"))).build(), "integer width");
  }

  @Test
  public void invalidBytesNIsRejected() {
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "bytes0"))).build(), "bytesN size");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "bytes33"))).build(), "bytesN size");
  }

  @Test
  public void fixedFamilyIsRejected() {
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "fixed"))).build(), "fixed/ufixed");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "ufixed"))).build(), "fixed/ufixed");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "fixed128x18"))).build(), "fixed/ufixed");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "ufixed256x80"))).build(), "fixed/ufixed");
  }

  @Test
  public void malformedArrayIsRejected() {
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "uint256[0]"))).build(), "array size must be positive");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "address[] storage"))).build(), "malformed array brackets");
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "uint256[a]"))).build(), "malformed array brackets");
  }

  @Test
  public void unknownBaseTypeIsRejected() {
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", "money"))).build(), "unknown base type");
  }

  @Test
  public void emptyTypeIsRejected() {
    expect(ABI.newBuilder().addEntrys(
        function("foo", param("x", ""))).build(), "type must not be empty");
  }

  @Test
  public void duplicateConstructorIsRejected() {
    ABI abi = ABI.newBuilder()
        .addEntrys(constructor())
        .addEntrys(constructor())
        .build();
    expect(abi, "only one constructor");
  }

  @Test
  public void duplicateFallbackIsRejected() {
    ABI abi = ABI.newBuilder()
        .addEntrys(fallback())
        .addEntrys(fallback())
        .build();
    expect(abi, "only one fallback");
  }

  @Test
  public void duplicateReceiveIsRejected() {
    ABI abi = ABI.newBuilder()
        .addEntrys(receive())
        .addEntrys(receive())
        .build();
    expect(abi, "only one receive");
  }

  @Test
  public void receiveMustBePayable() {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Receive)
        .setStateMutability(StateMutabilityType.Nonpayable)).build();
    expect(abi, "must be payable");
  }

  @Test
  public void legacyPayableFlagSatisfiesReceive() throws Exception {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Receive)
        .setPayable(true)).build();
    AbiValidator.validate(abi);
  }

  @Test
  public void fallbackWithIoIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Fallback)
        .addInputs(param("x", "uint256"))).build();
    expect(abi, "fallback function must not have inputs or outputs");
  }

  @Test
  public void receiveWithIoIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Receive)
        .setStateMutability(StateMutabilityType.Payable)
        .addOutputs(param("x", "uint256"))).build();
    expect(abi, "receive function must not have inputs or outputs");
  }

  @Test
  public void unknownEntryTypeIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.UnknownEntryType)).build();
    expect(abi, "unknown entry type");
  }

  @Test
  public void functionMissingNameIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Function)).build();
    expect(abi, "function must have a name");
  }

  @Test
  public void nonAnonymousEventMissingNameIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Event)).build();
    expect(abi, "non-anonymous event must have a name");
  }

  @Test
  public void anonymousEventMayOmitName() throws Exception {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Event)
        .setAnonymous(true)).build();
    AbiValidator.validate(abi);
  }

  @Test
  public void errorMissingNameIsRejected() {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Error)).build();
    expect(abi, "error must have a name");
  }

  @Test
  public void namedErrorIsAccepted() throws Exception {
    ABI abi = ABI.newBuilder().addEntrys(Entry.newBuilder()
        .setType(EntryType.Error)
        .setName("InsufficientBalance")
        .addInputs(param("required", "uint256"))).build();
    AbiValidator.validate(abi);
  }

  @Test
  public void errorMessageCarriesFieldPath() {
    ABI abi = ABI.newBuilder()
        .addEntrys(function("a", param("x", "address")))
        .addEntrys(function("b", param("x", "address"), param("y", "uint")))
        .build();
    ContractValidateException e = assertThrows(ContractValidateException.class,
        () -> AbiValidator.validate(abi));
    String msg = e.getMessage();
    org.junit.Assert.assertTrue(msg, msg.contains("entry #1"));
    org.junit.Assert.assertTrue(msg, msg.contains("inputs[1]"));
    org.junit.Assert.assertTrue(msg, msg.contains("'uint'"));
  }

  // helpers

  private static void expect(ABI abi, String snippet) {
    ContractValidateException e = assertThrows(ContractValidateException.class,
        () -> AbiValidator.validate(abi));
    org.junit.Assert.assertTrue(
        "expected message to contain '" + snippet + "', got: " + e.getMessage(),
        e.getMessage().contains(snippet));
  }

  private static Param param(String name, String type) {
    return Param.newBuilder().setName(name).setType(type).build();
  }

  private static Param indexedParam(String name, String type) {
    return Param.newBuilder().setName(name).setType(type).setIndexed(true).build();
  }

  private static Entry function(String name, Param... inputs) {
    Entry.Builder b = Entry.newBuilder().setType(EntryType.Function).setName(name);
    for (Param p : inputs) {
      b.addInputs(p);
    }
    return b.build();
  }

  private static Entry event(String name, boolean anonymous, Param... inputs) {
    Entry.Builder b = Entry.newBuilder().setType(EntryType.Event).setName(name)
        .setAnonymous(anonymous);
    for (Param p : inputs) {
      b.addInputs(p);
    }
    return b.build();
  }

  private static Entry anonymousEvent(String name, Param... inputs) {
    return event(name, true, inputs);
  }

  private static Entry constructor(Param... inputs) {
    Entry.Builder b = Entry.newBuilder().setType(EntryType.Constructor);
    for (Param p : inputs) {
      b.addInputs(p);
    }
    return b.build();
  }

  private static Entry fallback() {
    return Entry.newBuilder().setType(EntryType.Fallback).build();
  }

  private static Entry receive() {
    return Entry.newBuilder().setType(EntryType.Receive)
        .setStateMutability(StateMutabilityType.Payable).build();
  }
}
