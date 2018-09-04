package org.tron.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TVMTestUtils;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.protos.Protocol.AccountType;

@Slf4j

public class ChargeTest {

  private Manager dbManager;
  private TronApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_ChargeTest";
  private String OWNER_ADDRESS;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 100000000000000L);
    deposit.commit();
  }

  // pragma solidity ^0.4.16;
  //
  // contract subContract {
  //   constructor () payable {}
  // }
  //
  // contract TestOverflowContract {
  //
  //   function testOverflow() payable {
  //     subContract sc = (new subContract).value(10 ether)();
  //   }
  // }

  @Test
  public void testOverflow()
      throws ContractExeException, TransactionTraceException, ContractValidateException, ReceiptCheckErrException {
    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testOverflow";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testOverflow\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610100806100206000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680638040cac4146044575b600080fd5b604a604c565b005b6000678ac7230489e80000605d607f565b6040518091039082f0801580156077573d6000803e3d6000fd5b509050905050565b60405160468061008f833901905600608060405260358060116000396000f3006080604052600080fd00a165627a7a723058201738d6aa899dc00d4e99de944eb74d30a9ba1fcae37b99dc6299d95e992ca8b40029a165627a7a7230582068390137ba70dfc460810603eba8500b050ed3cd01e66f55ec07d387ec1cd2750029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 51293);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testOverflow()", "");
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 20000000000L, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof ArithmeticException);
  }

  // pragma solidity ^0.4.16;
  //
  // contract subContract {
  //   constructor () payable {}
  // }
  //
  // contract TestNegativeContract {
  //
  //   event logger(uint256);
  //   function testNegative() payable {
  //     int256 a = -1;
  //     logger(uint256(a));
  //     subContract sc = (new subContract).value(uint(a))();
  //   }
  // }

  @Test
  public void testNegative()
      throws ContractExeException, TransactionTraceException, ContractValidateException, ReceiptCheckErrException {
    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testNegative";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testNegative\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"logger\",\"type\":\"event\"}]";
    String code = "608060405234801561001057600080fd5b50610154806100206000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680638f7d8a1c146044575b600080fd5b604a604c565b005b6000807fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff91507f3a26492830c137b6cedfdd0e23db0e9c7c214e4fd1de32de8ceece1678b771b3826040518082815260200191505060405180910390a18160b060d3565b6040518091039082f08015801560ca573d6000803e3d6000fd5b50905090505050565b6040516046806100e3833901905600608060405260358060116000396000f3006080604052600080fd00a165627a7a72305820ef54aac72efff56dbe894e7218d009a87368bb70338bb385db5d3dec9927bc2c0029a165627a7a723058201620679ac2ae640d0a6c26e9cb4523e98eb0de8fff26975c5bb4c7fda1c98d720029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 68111);
    byte[] contractAddress = result.getContractAddress();

    /* ======================================CALL testNegative() with 0 callvalue ================================ */
    byte[] triggerData = TVMTestUtils.parseABI("testNegative()", "");
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof ArithmeticException);

    /* ======================================CALL testNegative() with -100 callvalue ================================ */
    triggerData = TVMTestUtils.parseABI("testNegative()", "");
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, -100, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof ArithmeticException);

    Assert.assertEquals(deposit.getBalance(address), 100000000000000L);

  }

  @Test
  @Ignore
  public void testFallback()
      throws ContractExeException, TransactionTraceException, ContractValidateException, ReceiptCheckErrException {
    // done in EnergyWhenSendAndTransferTest.java

  }

  // contract TestCallDepth {
  //
  //   function CallstackExploit(int256 counter) external {
  //     if (counter > 0) {
  //       this.CallstackExploit.gas(msg.gas - 2000)(counter - 1);
  //     } else {}
  //   }
  //
  //   function Call(int256 counter) {
  //     this.CallstackExploit(counter);
  //   }
  // }

  @Test
  public void testCallDepth()
      throws ContractExeException, TransactionTraceException, ContractValidateException, ReceiptCheckErrException {
    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testCallDepth";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"int256\"}],\"name\":\"CallstackExploit\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"int256\"}],\"name\":\"Call\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610174806100206000396000f3006080604052600436106100325763ffffffff60e060020a6000350416633a3c47188114610037578063eede0f0114610051575b600080fd5b34801561004357600080fd5b5061004f600435610069565b005b34801561005d57600080fd5b5061004f6004356100d7565b60008113156100d45730633a3c47186107d05a03600184036040518363ffffffff1660e060020a02815260040180828152602001915050600060405180830381600088803b1580156100ba57600080fd5b5087f11580156100ce573d6000803e3d6000fd5b50505050505b50565b3073ffffffffffffffffffffffffffffffffffffffff16633a3c4718826040518263ffffffff1660e060020a02815260040180828152602001915050600060405180830381600087803b15801561012d57600080fd5b505af1158015610141573d6000803e3d6000fd5b50505050505600a165627a7a72305820510367f4437b1af16931cacc744eb6f3102d72f0c369aa795a4dc49a7f90a3e90029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 74517);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    String params = "0000000000000000000000000000000000000000000000000000000000002710";
    // byte[] triggerData = TVMTestUtils.parseABI("CallstackExploit(int)", params);
    byte[] triggerData = TVMTestUtils.parseABI("Call(int256)", params);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 27743);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);

  }

  // contract subContract {
  //
  //   function CallstackExploit(uint256 counter) external {
  //     if (counter > 0) {
  //       this.CallstackExploit.gas(msg.gas - 2000)(counter - 1);
  //     } else {}
  //   }
  //
  //   function Call(uint256 counter) {
  //
  //     if (counter <= 20) {
  //       this.CallstackExploit(counter + 20);
  //     }
  //     else {
  //       for (uint256 i = 0; i < counter; i++) {
  //         this.Call(i - 20);
  //       }
  //     }
  //   }
  // }
  //
  // contract TestCallDepthAndWidthContract {
  //
  //   subContract sc = new subContract();
  //
  //   function CallstackExploit(uint256 counter) external {
  //     if (counter > 0) {
  //       this.CallstackExploit.gas(msg.gas - 2000)(counter - 1);
  //     } else {}
  //   }
  //
  //   function Call(uint256 counter) {
  //
  //     for (uint256 i = 0; i < counter; i++) {
  //       this.CallstackExploit(i + 20);
  //       sc.Call(i + 10);
  //     }
  //
  //   }
  // }

  @Test
  public void testCallDepthAndWidth()
      throws ContractExeException, TransactionTraceException, ContractValidateException, ReceiptCheckErrException {
    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testCallDepthAndWidth";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"uint256\"}],\"name\":\"CallstackExploit\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"uint256\"}],\"name\":\"Call\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405261000c61005b565b604051809103906000f080158015610028573d6000803e3d6000fd5b5060008054600160a060020a031916600160a060020a039290921691909117905534801561005557600080fd5b5061006b565b604051610265806102c683390190565b61024c8061007a6000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166370ed9d828114610050578063f84df1931461006a575b600080fd5b34801561005c57600080fd5b50610068600435610082565b005b34801561007657600080fd5b50610068600435610109565b600081111561010657306370ed9d826107d05a03600184036040518363ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600088803b1580156100ec57600080fd5b5087f1158015610100573d6000803e3d6000fd5b50505050505b50565b60005b8181101561021c57604080517f70ed9d82000000000000000000000000000000000000000000000000000000008152601483016004820152905130916370ed9d8291602480830192600092919082900301818387803b15801561016e57600080fd5b505af1158015610182573d6000803e3d6000fd5b505060008054604080517ff84df193000000000000000000000000000000000000000000000000000000008152600a87016004820152905173ffffffffffffffffffffffffffffffffffffffff909216945063f84df1939350602480820193929182900301818387803b1580156101f857600080fd5b505af115801561020c573d6000803e3d6000fd5b50506001909201915061010c9050565b50505600a165627a7a72305820ad701f54dc539d976cc2af0443d5d190dbe727ce2e24d66f3e2390dfd79859640029608060405234801561001057600080fd5b50610245806100206000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166370ed9d828114610050578063f84df1931461006a575b600080fd5b34801561005c57600080fd5b50610068600435610082565b005b34801561007657600080fd5b50610068600435610109565b600081111561010657306370ed9d826107d05a03600184036040518363ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600088803b1580156100ec57600080fd5b5087f1158015610100573d6000803e3d6000fd5b50505050505b50565b60006014821161018a57604080517f70ed9d82000000000000000000000000000000000000000000000000000000008152601484016004820152905130916370ed9d8291602480830192600092919082900301818387803b15801561016d57600080fd5b505af1158015610181573d6000803e3d6000fd5b50505050610215565b5060005b8181101561021557604080517ff84df193000000000000000000000000000000000000000000000000000000008152601319830160048201529051309163f84df19391602480830192600092919082900301818387803b1580156101f157600080fd5b505af1158015610205573d6000803e3d6000fd5b50506001909201915061018e9050565b50505600a165627a7a72305820a9e7e1401001d6c131ebf4727fbcedede08d16416dc0447cef60e0b9516c6a260029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 286450);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    String params = "000000000000000000000000000000000000000000000000000000000000000a";
    // byte[] triggerData = TVMTestUtils.parseABI("CallstackExploit(int)", params);
    byte[] triggerData = TVMTestUtils.parseABI("Call(uint256)", params);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 243698);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);

  }

  @Test
  public void testCreateDepthAndWidth()
      throws ContractExeException, TransactionTraceException, ContractValidateException, ReceiptCheckErrException {
    long value = 0;
    long feeLimit = 90000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testCallDepthAndWidth";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"uint256\"}],\"name\":\"testCreate\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b506103f0806100206000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663b505dee58114610045575b600080fd5b34801561005157600080fd5b5061005d60043561005f565b005b6000805b828210156101255761007361012a565b604051809103906000f08015801561008f573d6000803e3d6000fd5b5090508073ffffffffffffffffffffffffffffffffffffffff1663da6d107a836040518263ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600087803b15801561010157600080fd5b505af1158015610115573d6000803e3d6000fd5b5050600190930192506100639050565b505050565b60405161028a8061013b833901905600608060405234801561001057600080fd5b5061026a806100206000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663da6d107a8114610045575b600080fd5b34801561005157600080fd5b5061005d60043561005f565b005b60008082111561010f573063da6d107a6107d05a03600185036040518363ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600088803b1580156100ca57600080fd5b5087f11580156100de573d6000803e3d6000fd5b50505050506100eb61013b565b604051809103906000f080158015610107573d6000803e3d6000fd5b509050610137565b61011761013b565b604051809103906000f080158015610133573d6000803e3d6000fd5b5090505b5050565b60405160f48061014b8339019056006080604052348015600f57600080fd5b506000805b6064821015604a5760226050565b604051809103906000f080158015603d573d6000803e3d6000fd5b5060019092019190506014565b5050605f565b6040516052806100a283390190565b60358061006d6000396000f3006080604052600080fd00a165627a7a723058203565a8abc553526f8113ab8a3f432963d88cee07cafce0ebfc61173d3797b84700296080604052348015600f57600080fd5b50603580601d6000396000f3006080604052600080fd00a165627a7a723058204855bba321c7dee00dfa91caa8926cf07c38c541a11ba36d3b2a4687acaa909c0029a165627a7a7230582093af601a9196cffc9bf82bcae83557d7f5aedeec639129c27826f38c1e2a2ea00029a165627a7a7230582071d51c39c93b0aba5baeacea0b2bd5ca5342d028bb834046eca92975a3517a4c0029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 201839);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    String params = "0000000000000000000000000000000000000000000000000000000000000001";
    // byte[] triggerData = TVMTestUtils.parseABI("CallstackExploit(int)", params);
    byte[] triggerData = TVMTestUtils.parseABI("testCreate(uint256)", params);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 4481164);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);

  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }
}
