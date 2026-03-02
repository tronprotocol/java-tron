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
import org.tron.core.services.jsonrpc.types.CallArguments;
import org.tron.protos.Protocol;

public class CallArgumentsTest extends BaseTest {

  @Resource
  private Wallet wallet;

  private CallArguments callArguments;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Before
  public void init() {
    callArguments = new CallArguments("0x0000000000000000000000000000000000000000",
            "0x0000000000000000000000000000000000000001","0x10","0.01","0x100",
        "","0");
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

}
