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
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.protos.Protocol;

public class CallArgumentsTest extends BaseTest {

  @Resource
  private Wallet wallet;

  private CallArguments callArguments;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
  }

  @Before
  public void init() {
    callArguments = new CallArguments(
        "0x0000000000000000000000000000000000000000",
        "0x0000000000000000000000000000000000000001",
        "0x10", "0.01", "0x100",
        "", "", "0");
  }

  @Test
  public void testGetContractType()
      throws JsonRpcInvalidRequestException, JsonRpcInvalidParamsException {
    Protocol.Transaction.Contract.ContractType contractType = callArguments.getContractType(wallet);
    Assert.assertEquals(Protocol.Transaction.Contract.ContractType.TransferContract, contractType);
  }

  @Test
  public void testParseValue() throws JsonRpcInvalidParamsException {
    long value = callArguments.parseValue();
    Assert.assertEquals(256L, value);
  }

  @Test
  public void resolveData_inputOnly_returnsInput() throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setInput("0xdeadbeef");
    Assert.assertEquals("0xdeadbeef", args.resolveData());
  }

  @Test
  public void resolveData_dataOnly_returnsData() throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setData("0xcafebabe");
    Assert.assertEquals("0xcafebabe", args.resolveData());
  }

  @Test
  public void resolveData_bothPresentSame_returnsValue() throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setData("0xfeedface");
    args.setInput("0xfeedface");
    Assert.assertEquals("0xfeedface", args.resolveData());
  }

  @Test
  public void resolveData_bothPresentDifferent_inputWinsNoError()
      throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setData("0xcafebabe");
    args.setInput("0xdeadbeef");
    Assert.assertEquals("0xdeadbeef", args.resolveData());
  }

  @Test
  public void resolveData_inputIsZeroX_dataNonEmpty_returnsInput()
      throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setInput("0x");
    args.setData("0xdeadbeef");
    Assert.assertEquals("0x", args.resolveData());
  }

  @Test
  public void resolveData_dataIsZeroX_inputNonEmpty_returnsInput()
      throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setInput("0xdeadbeef");
    args.setData("0x");
    Assert.assertEquals("0xdeadbeef", args.resolveData());
  }

  @Test
  public void resolveData_caseDifference_returnsInput()
      throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setInput("0xDEADbeef");
    args.setData("0xdeadbeef");
    Assert.assertEquals("0xDEADbeef", args.resolveData());
  }

  /** Pins geth-equivalent semantics: "" is presence, wins over data by precedence. */
  @Test
  public void resolveData_inputEmpty_dataNonEmpty_inputWinsAsEmpty()
      throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setInput("");
    args.setData("0xdeadbeef");
    Assert.assertEquals("", args.resolveData());
  }

  @Test
  public void resolveData_neitherPresent_returnsNull()
      throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    Assert.assertNull(args.resolveData());
  }

  /** Validates the loser field too, not only the precedence winner. */
  @Test
  public void resolveData_inputValidDataMalformed_throwsInvalidParams() {
    CallArguments args = new CallArguments();
    args.setInput("0xdeadbeef");
    args.setData("0xzz");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_inputMalformedDataValid_throwsInvalidParams() {
    CallArguments args = new CallArguments();
    args.setInput("0xzz");
    args.setData("0xdeadbeef");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_inputMalformedDataAbsent_throwsInvalidParams() {
    CallArguments args = new CallArguments();
    args.setInput("0xzz");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_dataMalformedInputAbsent_throwsInvalidParams() {
    CallArguments args = new CallArguments();
    args.setData("0xzz");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  /**
   * {@code input} is the new spec-aligned field: missing {@code 0x} prefix
   * is rejected per the execution-apis BYTES schema.
   */
  @Test
  public void resolveData_inputNoPrefix_throwsInvalidParams() {
    CallArguments args = new CallArguments();
    args.setInput("deadbeef");
    Assert.assertThrows(JsonRpcInvalidParamsException.class, args::resolveData);
  }

  @Test
  public void resolveData_inputOddLength_throwsInvalidParams() {
    CallArguments args = new CallArguments();
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
    CallArguments args = new CallArguments();
    args.setData("deadbeef");
    Assert.assertEquals("deadbeef", args.resolveData());
  }

  @Test
  public void resolveData_dataOddLength_acceptedForBackwardCompat()
      throws JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setData("0x123");
    Assert.assertEquals("0x123", args.resolveData());
  }

  /** Reproduces issue #6517 contract-creation symptom. */
  @Test
  public void getContractType_createSmartContractViaInput_succeeds()
      throws JsonRpcInvalidRequestException, JsonRpcInvalidParamsException {
    CallArguments args = new CallArguments();
    args.setFrom("0x0000000000000000000000000000000000000001");
    args.setInput("0xdeadbeef");
    Assert.assertEquals(
        Protocol.Transaction.Contract.ContractType.CreateSmartContract,
        args.getContractType(wallet));
  }

  /** Reproduces issue #6517 Jackson parse-error symptom. */
  @Test
  public void deserializeWithInputField_succeedsAndResolvesToInput() throws Exception {
    String json = "{\"from\":\"0x0000000000000000000000000000000000000001\","
        + "\"to\":\"0x0000000000000000000000000000000000000002\","
        + "\"input\":\"0xdeadbeef\"}";
    CallArguments args = new ObjectMapper().readValue(json, CallArguments.class);
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
    CallArguments args = new CallArguments();
    args.setInput("0xdeadbeef");
    args.setData("0xcafebabe");   // would throw conflict in build path
    String json = new ObjectMapper().writeValueAsString(args);
    Assert.assertFalse("should not leak resolveData: " + json,
        json.contains("resolveData"));
  }

  /** Same guarantee for FastJSON, which also discovers bean getters. */
  @Test
  public void fastjsonSerialize_doesNotExposeResolveDataOrThrowOnConflict() {
    CallArguments args = new CallArguments();
    args.setInput("0xdeadbeef");
    args.setData("0xcafebabe");
    String json = com.alibaba.fastjson.JSON.toJSONString(args);
    Assert.assertFalse("should not leak resolveData: " + json,
        json.contains("resolveData"));
  }

}
