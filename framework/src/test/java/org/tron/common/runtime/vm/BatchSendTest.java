package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.crypto.ECKey;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.Utils;
import org.tron.common.utils.client.utils.AbiUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class BatchSendTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String TRANSFER_TO;
  private static Runtime runtime;
  private static RepositoryImpl repository;
  private static final AccountCapsule ownerCapsule;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath(), "--debug"}, Constant.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    TRANSFER_TO = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

    ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("owner"),
            AccountType.AssetIssue);

    ownerCapsule.setBalance(1000_1000_1000L);


  }

  @Before
  public void before() {
    repository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    repository.createAccount(Hex.decode(TRANSFER_TO), AccountType.Normal);
    repository.addBalance(Hex.decode(TRANSFER_TO), 10);
    repository.commit();

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveAllowMultiSign(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);
  }

  /**
   * pragma solidity ^0.5.4;
   * contract TestBatchSendTo { constructor() public payable{}
   * function depositIn() public payable{}
   * function batchSendTo (address  payable to1 ,address  payable to2 ,address  payable to3, uint256
   * m1,uint256 m2,uint256 m3) public { to1.send(m1 ); to2.send(m2 ); to3.send(m3 ); }
   * }
   */
  @Test
  public void TransferTokenTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    //  1. Deploy*/
    byte[] contractAddress = deployTransferContract();
    repository.commit();
    Assert.assertEquals(1000,
        dbManager.getAccountStore().get(contractAddress).getBalance());

    String selectorStr = "batchSendTo(address,address,address,uint256,uint256,uint256)";
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    ECKey ecKey3 = new ECKey(Utils.getRandom());

    List<Object> params = new ArrayList<>();
    logger.info("show db key: {}", StringUtil.createDbKey(ByteString.copyFrom("test".getBytes())));
    params.add(StringUtil.encode58Check(ecKey1.getAddress()));
    params.add(StringUtil.encode58Check(ecKey2.getAddress()));
    params.add(StringUtil.encode58Check(ecKey3.getAddress()));
    params.add(100);
    params.add(1100);
    params.add(200);
    byte[] input = Hex.decode(AbiUtil
        .parseMethod(selectorStr, params));

    //  2. Test trigger with tokenValue and tokenId, also test internal transaction
    // transferToken function */
    long triggerCallValue = 0;
    long feeLimit = 100000000;
    long tokenValue = 0;
    Transaction transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            triggerCallValue, feeLimit, tokenValue, 0);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);
    Assert.assertNull(runtime.getRuntimeError());
    //send success, create account
    Assert.assertEquals(100,
        dbManager.getAccountStore().get(ecKey1.getAddress()).getBalance());
    //send failed, do not create account
    Assert.assertNull(dbManager.getAccountStore().get(ecKey2.getAddress()));
    //send success, create account
    Assert.assertEquals(200,
        dbManager.getAccountStore().get(ecKey3.getAddress()).getBalance());

  }

  private byte[] deployTransferContract()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "TestTransferTo";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[]";
    String code = "608060405261019c806100136000396000f3fe608060405260043610610045577c0100000000000"
        + "00000000000000000000000000000000000000000000060003504632a205edf811461004a5780634cd2270c"
        + "146100c8575b600080fd5b34801561005657600080fd5b50d3801561006357600080fd5b50d2801561007057"
        + "600080fd5b506100c6600480360360c081101561008757600080fd5b5073ffffffffffffffffffffffffffff"
        + "ffffffffffff813581169160208101358216916040820135169060608101359060808101359060a001356100"
        + "d0565b005b6100c661016e565b60405173ffffffffffffffffffffffffffffffffffffffff87169084156108"
        + "fc029085906000818181858888f1505060405173ffffffffffffffffffffffffffffffffffffffff89169350"
        + "85156108fc0292508591506000818181858888f1505060405173ffffffffffffffffffffffffffffffffffff"
        + "ffff8816935084156108fc0292508491506000818181858888f15050505050505050505050565b56fea16562"
        + "7a7a72305820cc2d598d1b3f968bbdc7825ce83d22dad48192f4bf95bda7f9e4ddf61669ba830029";

    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    long tokenValue = 0;
    long tokenId = 0;

    return TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, tokenValue, tokenId,
            repository, null);
  }
}
