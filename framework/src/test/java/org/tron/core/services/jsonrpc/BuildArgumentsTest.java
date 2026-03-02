package org.tron.core.services.jsonrpc;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.exception.JsonRpcInvalidRequestException;
import org.tron.core.services.jsonrpc.types.BuildArguments;
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.protos.Protocol;

public class BuildArgumentsTest extends BaseTest {

  @Resource
  private Wallet wallet;

  private BuildArguments buildArguments;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Before
  public void initBuildArgs() {
    buildArguments = new BuildArguments(
        "0x0000000000000000000000000000000000000000",
        "0x0000000000000000000000000000000000000001","0x10","0.01","0x100",
        "","0",9L,10000L,"",10L,
        2000L,"args",1,"",true);
  }


  @Test
  public void testBuildArgument() {
    CallArguments callArguments = new CallArguments(
        "0x0000000000000000000000000000000000000000",
        "0x0000000000000000000000000000000000000001","0x10","0.01","0x100",
        "","0");
    BuildArguments buildArguments = new BuildArguments(callArguments);
    Assert.assertEquals(buildArguments.getFrom(),
        "0x0000000000000000000000000000000000000000");
    Assert.assertEquals(buildArguments.getTo(),
        "0x0000000000000000000000000000000000000001");
    Assert.assertEquals(buildArguments.getGas(), "0x10");
    Assert.assertEquals(buildArguments.getGasPrice(), "0.01");
  }

  @Test
  public void testGetContractType()
      throws JsonRpcInvalidRequestException, JsonRpcInvalidParamsException {
    Protocol.Transaction.Contract.ContractType contractType =
        buildArguments.getContractType(wallet);
    Assert.assertEquals(contractType, Protocol.Transaction.Contract.ContractType.TransferContract);
  }

  @Test
  public void testParseValue() throws JsonRpcInvalidParamsException {
    long value = buildArguments.parseValue();
    Assert.assertEquals(value, 256L);
  }

  @Test
  public void testParseGas() throws JsonRpcInvalidParamsException {
    long gas = buildArguments.parseGas();
    Assert.assertEquals(gas, 16L);
  }

}
