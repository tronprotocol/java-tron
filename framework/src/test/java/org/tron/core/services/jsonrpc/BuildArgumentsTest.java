package org.tron.core.services.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidParamsException;
import org.tron.core.exception.jsonrpc.JsonRpcInvalidRequestException;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.protos.Protocol;

public class BuildArgumentsTest extends BaseTest {

  @Resource
  private Wallet wallet;

  private BuildArguments buildArguments;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
  }

  @Before
  public void initBuildArgs() {
    buildArguments = new BuildArguments(
        "0x0000000000000000000000000000000000000000",
        "0x0000000000000000000000000000000000000001", "0x10", "0.01", "0x100",
        "", "", "0", 9L, 10000L, "", 10L,
        2000L, "args", 1, "", true);
  }


  @Test
  public void testBuildArgument() {
    CallArguments callArguments = new CallArguments(
        "0x0000000000000000000000000000000000000000",
        "0x0000000000000000000000000000000000000001", "0x10", "0.01", "0x100",
        "", "", "0");
    BuildArguments args = new BuildArguments(callArguments);
    Assert.assertEquals("0x0000000000000000000000000000000000000000", args.getFrom());
    Assert.assertEquals("0x0000000000000000000000000000000000000001", args.getTo());
    Assert.assertEquals("0x10", args.getGas());
    Assert.assertEquals("0.01", args.getGasPrice());
  }

  @Test
  public void testGetContractType()
      throws JsonRpcInvalidRequestException, JsonRpcInvalidParamsException {
    Protocol.Transaction.Contract.ContractType contractType =
        buildArguments.getContractType(wallet);
    Assert.assertEquals(Protocol.Transaction.Contract.ContractType.TransferContract, contractType);
  }

  @Test
  public void testParseValue() throws JsonRpcInvalidParamsException {
    long value = buildArguments.parseValue();
    Assert.assertEquals(256L, value);
  }

  @Test
  public void testParseGas() throws JsonRpcInvalidParamsException {
    long gas = buildArguments.parseGas();
    Assert.assertEquals(16L, gas);
  }

  @Test
  public void resolveData_inputOnly_returnsInput() throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setInput("0xdeadbeef");
    Assert.assertEquals("0xdeadbeef", args.resolveData());
  }

  @Test
  public void resolveData_dataOnly_returnsData() throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setData("0xcafebabe");
    Assert.assertEquals("0xcafebabe", args.resolveData());
  }

  @Test
  public void resolveData_bothPresentSame_returnsValue() throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setData("0xfeedface");
    args.setInput("0xfeedface");
    Assert.assertEquals("0xfeedface", args.resolveData());
  }

  /** Pins that "0x" on both sides decodes to []==[] and is not a conflict. */
  @Test
  public void resolveData_bothZeroX_returnsZeroX() throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setInput("0x");
    args.setData("0x");
    Assert.assertEquals("0x", args.resolveData());
  }

  @Test
  public void resolveData_inputZeroXOnly_returnsZeroX() throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setInput("0x");
    Assert.assertEquals("0x", args.resolveData());
  }

  @Test
  public void resolveData_dataZeroXOnly_returnsZeroX() throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setData("0x");
    Assert.assertEquals("0x", args.resolveData());
  }

  @Test
  public void resolveData_caseDifference_returnsInput()
      throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setInput("0xDEADbeef");
    args.setData("0xdeadbeef");
    Assert.assertEquals("0xDEADbeef", args.resolveData());
  }

  /**
   * Pins geth-equivalent semantics: empty string is presence with
   * empty bytes, so paired with non-empty data the byte values differ
   * and the build path raises the geth setDefaults conflict at the
   * {@code getContractType()} entry point.
   */
  @Test
  public void getContractType_inputEmptyDataNonEmpty_throwsConflict() {
    BuildArguments args = new BuildArguments();
    args.setFrom("0x0000000000000000000000000000000000000001");
    args.setInput("");
    args.setData("0xdeadbeef");
    Assert.assertThrows(JsonRpcInvalidParamsException.class,
        () -> args.getContractType(wallet));
  }

  /**
   * Wording matches go-ethereum's setDefaults so existing tooling can
   * detect the error string.
   */
  @Test
  public void getContractType_inputAndDataConflict_throwsInvalidParams() {
    BuildArguments args = new BuildArguments();
    args.setFrom("0x0000000000000000000000000000000000000001");
    args.setInput("0xdeadbeef");
    args.setData("0xcafebabe");

    JsonRpcInvalidParamsException ex = Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> args.getContractType(wallet));
    Assert.assertTrue(
        "error message should match go-ethereum's wording: " + ex.getMessage(),
        ex.getMessage().contains("both \"data\" and \"input\" are set and not equal"));
  }

  @Test
  public void getContractType_inputZeroXDataNonEmpty_throwsConflict() {
    BuildArguments args = new BuildArguments();
    args.setFrom("0x0000000000000000000000000000000000000001");
    args.setInput("0x");
    args.setData("0xdeadbeef");

    Assert.assertThrows(
        JsonRpcInvalidParamsException.class,
        () -> args.getContractType(wallet));
  }

  @Test
  public void getContractType_inputAndDataEqual_succeeds()
      throws JsonRpcInvalidRequestException, JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setFrom("0x0000000000000000000000000000000000000001");
    args.setInput("0xdeadbeef");
    args.setData("0xdeadbeef");
    Assert.assertEquals(
        Protocol.Transaction.Contract.ContractType.CreateSmartContract,
        args.getContractType(wallet));
  }

  /** Reproduces issue #6517 contract-creation symptom on the build path. */
  @Test
  public void getContractType_createSmartContractViaInput_succeeds()
      throws JsonRpcInvalidRequestException, JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setFrom("0x0000000000000000000000000000000000000001");
    args.setInput("0xdeadbeef");
    Assert.assertEquals(
        Protocol.Transaction.Contract.ContractType.CreateSmartContract,
        args.getContractType(wallet));
  }

  @Test
  public void copyConstructor_preservesBothInputAndData() {
    CallArguments src = new CallArguments();
    src.setFrom("0x0000000000000000000000000000000000000001");
    src.setData("0xcafebabe");
    src.setInput("0xdeadbeef");

    BuildArguments copy = new BuildArguments(src);
    Assert.assertEquals("0xcafebabe", copy.getData());
    Assert.assertEquals("0xdeadbeef", copy.getInput());
  }

  @Test
  public void copyConstructor_propagatesConflictToBuildPath() {
    CallArguments src = new CallArguments();
    src.setData("0xcafebabe");
    src.setInput("0xdeadbeef");

    BuildArguments copy = new BuildArguments(src);
    Assert.assertThrows(JsonRpcInvalidParamsException.class,
        () -> copy.getContractType(wallet));
  }

  @Test
  public void deserializeWithInputField_succeedsAndResolvesToInput() throws Exception {
    String json = "{\"from\":\"0x0000000000000000000000000000000000000001\","
        + "\"to\":\"0x0000000000000000000000000000000000000002\","
        + "\"input\":\"0xdeadbeef\"}";
    BuildArguments args = new ObjectMapper().readValue(json, BuildArguments.class);
    Assert.assertEquals("0xdeadbeef", args.resolveData());
    Assert.assertEquals("0xdeadbeef", args.getInput());
    Assert.assertNull(args.getData());
  }

  /**
   * Regression guard: a future {@code getXxx} rename would expose
   * {@code resolveData} as a wire property and risk throwing during
   * serialisation.
   */
  @Test
  public void jacksonSerialize_doesNotExposeResolveDataOrThrowOnConflict()
      throws Exception {
    BuildArguments args = new BuildArguments();
    args.setInput("0xdeadbeef");
    args.setData("0xcafebabe");   // conflicting bytes, would throw if resolveData() were invoked
    String json = new ObjectMapper().writeValueAsString(args);
    Assert.assertFalse("should not leak resolveData: " + json,
        json.contains("resolveData"));
  }

  /** Same guarantee for FastJSON, which also discovers bean getters. */
  @Test
  public void fastjsonSerialize_doesNotExposeResolveDataOrThrowOnConflict() {
    BuildArguments args = new BuildArguments();
    args.setInput("0xdeadbeef");
    args.setData("0xcafebabe");
    String json = com.alibaba.fastjson.JSON.toJSONString(args);
    Assert.assertFalse("should not leak resolveData: " + json,
        json.contains("resolveData"));
  }

  /** Validates the loser field too, not only the precedence winner. */
  @Test
  public void resolveData_inputValidDataMalformed_throwsInvalidParams() {
    BuildArguments args = new BuildArguments();
    args.setInput("0xdeadbeef");
    args.setData("0xzz");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_inputMalformedDataValid_throwsInvalidParams() {
    BuildArguments args = new BuildArguments();
    args.setInput("0xzz");
    args.setData("0xdeadbeef");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_inputMalformedDataAbsent_throwsInvalidParams() {
    BuildArguments args = new BuildArguments();
    args.setInput("0xzz");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_dataMalformedInputAbsent_throwsInvalidParams() {
    BuildArguments args = new BuildArguments();
    args.setData("0xzz");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  /**
   * {@code input} is the new spec-aligned field: missing {@code 0x} prefix
   * is rejected per the execution-apis BYTES schema.
   */
  @Test
  public void resolveData_inputNoPrefix_throwsInvalidParams() {
    BuildArguments args = new BuildArguments();
    args.setInput("deadbeef");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_inputOddLength_throwsInvalidParams() {
    BuildArguments args = new BuildArguments();
    args.setInput("0x123");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  /**
   * {@code data} is the legacy field: bare hex (no {@code 0x} prefix)
   * stays accepted for backward compatibility with existing callers
   * (e.g. BuildTransactionTest.testCreateSmartContract).
   */
  @Test
  public void resolveData_dataNoPrefix_acceptedForBackwardCompat()
      throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setData("deadbeef");
    Assert.assertEquals("deadbeef", args.resolveData());
  }

  @Test
  public void resolveData_dataOddLength_acceptedForBackwardCompat()
      throws JsonRpcInvalidParamsException {
    BuildArguments args = new BuildArguments();
    args.setData("0x123");
    Assert.assertEquals("0x123", args.resolveData());
  }

}
