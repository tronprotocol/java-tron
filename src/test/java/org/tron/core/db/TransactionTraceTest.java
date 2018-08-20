/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.db;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.OutOfSlotTimeException;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.raw;

@RunWith(Parameterized.class)
public class TransactionTraceTest {

  public static final long totalBalance = 1000_0000_000_000L;
  public static final int deployStorageDelta = 680000;
  private static String dbPath = "output_TransactionTrace_test";
  private static String dbDirectory = "db_TransactionTrace_test";
  private static String indexDirectory = "index_TransactionTrace_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static StorageMarket storageMarket;
  private static ByteString ownerAddress = ByteString.copyFrom(ByteArray.fromInt(1));
  private static ByteString contractAddress = ByteString.copyFrom(ByteArray.fromInt(2));

  private long energyUsage;
  private long storageUsage;
  /*
   * DeployContract tracetestContract [{"constant":false,"inputs":[{"name":"accountId","type":"uint256"}],"name":"getVoters","outputs":[{"name":"","type":"uint256"}],"payable":false,"stateMutability":"nonpayable","type":"function"},{"constant":true,"inputs":[{"name":"","type":"uint256"}],"name":"voters","outputs":[{"name":"","type":"uint256"}],"payable":false,"stateMutability":"view","type":"function"},{"constant":false,"inputs":[{"name":"vote","type":"uint256"}],"name":"addVoters","outputs":[],"payable":false,"stateMutability":"nonpayable","type":"function"},{"inputs":[],"payable":false,"stateMutability":"nonpayable","type":"constructor"}] 608060405234801561001057600080fd5b5060015b620186a0811015610038576000818152602081905260409020819055600a01610014565b5061010b806100486000396000f30060806040526004361060525763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166386b646f281146057578063da58c7d914607e578063eb91a5ff146093575b600080fd5b348015606257600080fd5b50606c60043560aa565b60408051918252519081900360200190f35b348015608957600080fd5b50606c60043560bc565b348015609e57600080fd5b5060a860043560ce565b005b60009081526020819052604090205490565b60006020819052908152604090205481565b6000818152602081905260409020555600a165627a7a72305820f9935f89890e51bcf3ea98fa4841c91ac5957a197d99eeb7879a775b30ee9a2d0029   1000000000000 100
   * */
  private String trxByte = "0a80050a0231ca220844c8b91d4d5d7e5f40e0f19aecd32c5ad904081e12d4040a30747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e437265617465536d617274436f6e7472616374129f040a15411bd09e9a1bf949b3d08b56f85ad3e3e3905763c81285040a15411bd09e9a1bf949b3d08b56f85ad3e3e3905763c81a80010a301a09676574566f74657273221412096163636f756e7449641a0775696e743235362a091a0775696e74323536300240030a2410011a06766f7465727322091a0775696e743235362a091a0775696e74323536300240020a201a09616464566f74657273220f1204766f74651a0775696e74323536300240030a043001400322d302608060405234801561001057600080fd5b5060015b620186a0811015610038576000818152602081905260409020819055600a01610014565b5061010b806100486000396000f30060806040526004361060525763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166386b646f281146057578063da58c7d914607e578063eb91a5ff146093575b600080fd5b348015606257600080fd5b50606c60043560aa565b60408051918252519081900360200190f35b348015608957600080fd5b50606c60043560bc565b348015609e57600080fd5b5060a860043560ce565b005b60009081526020819052604090205490565b60006020819052908152604090205481565b6000818152602081905260409020555600a165627a7a72305820f9935f89890e51bcf3ea98fa4841c91ac5957a197d99eeb7879a775b30ee9a2d002930643a11747261636574657374436f6e747261637470d7b297ecd32c900180a094a58d1d124165e6fe033d9ee0369c298f7ef263eab2ebf33a63e20c6fad38cf64e0f0a4f8fa0c562e6beafbd43a841ff9058e7a09c88381636db68a9ce17f4529d66f00111e00";
  private static String OwnerAddress = "TCWHANtDDdkZCTo2T2peyEq3Eg9c2XB7ut";

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w",
            "--debug"
        },
        "config-test-mainnet.conf"
    );
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  public TransactionTraceTest(long energyUsage, long storageUsage) {
    this.energyUsage = energyUsage;
    this.storageUsage = storageUsage;
  }

  /**
   * resourceUsage prepare data for testing.
   */
  @Parameters
  public static Collection resourceUsage() {
    return Arrays.asList(new Object[][]{

        {0, 0},
        // {6, 1000},
        // {7, 1000},
        // {10, 999},
        // {13, 1000},
        // {14, 1000},
        // {20, 1000},
        // {10, 1000},
        // {10, 1001}

    });
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    storageMarket = new StorageMarket(dbManager);
    //init energy
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(10_000_000L);
    //init storage
    dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(
        128L * 1024 * 1024 * 1024);
    dbManager.getDynamicPropertiesStore().saveTotalStoragePool(100_000_000_000000L);
    dbManager.getDynamicPropertiesStore().saveTotalStorageTax(0);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);

  }

  @Test
  public void testUseFee() throws InvalidProtocolBufferException {
    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(OwnerAddress), accountCapsule);
    Transaction transaction = Transaction.parseFrom(ByteArray.fromHexString(trxByte));
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    TransactionTrace trace = new TransactionTrace(transactionCapsule, dbManager);
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    Runtime runtime = new Runtime(trace, null, deposit,
        new ProgramInvokeFactoryImpl());
    try {
      trace.exec(runtime);
      trace.pay();
      Assert.assertEquals(0, trace.getReceipt().getEnergyUsage());
      Assert.assertEquals(49503930, trace.getReceipt().getEnergyFee());
      Assert.assertEquals(deployStorageDelta, trace.getReceipt().getStorageDelta());
      Assert.assertEquals(494800000, trace.getReceipt().getStorageFee());
      accountCapsule = dbManager.getAccountStore().get(accountCapsule.getAddress().toByteArray());
      Assert.assertEquals(totalBalance,
          trace.getReceipt().getStorageFee() + trace.getReceipt().getEnergyFee() + accountCapsule
              .getBalance());
    } catch (ContractExeException e) {
      e.printStackTrace();
    } catch (ContractValidateException e) {
      e.printStackTrace();
    } catch (OutOfSlotTimeException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testUseUsage() throws InvalidProtocolBufferException {

    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);

    accountCapsule.setFrozenForEnergy(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(OwnerAddress), accountCapsule);
    storageMarket.buyStorage(accountCapsule, 1000_000L);
    Transaction transaction = Transaction.parseFrom(ByteArray.fromHexString(trxByte));
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    TransactionTrace trace = new TransactionTrace(transactionCapsule, dbManager);
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    Runtime runtime = new Runtime(trace, null, deposit,
        new ProgramInvokeFactoryImpl());
    try {
      trace.exec(runtime);
      trace.pay();
      Assert.assertEquals(32400, trace.getReceipt().getEnergyUsage());
      Assert.assertEquals(48531930, trace.getReceipt().getEnergyFee());
      Assert.assertEquals(49503930,
          trace.getReceipt().getEnergyUsage() * 30 + trace.getReceipt().getEnergyFee());
      Assert.assertEquals(deployStorageDelta, trace.getReceipt().getStorageDelta());
      Assert.assertEquals(493800000, trace.getReceipt().getStorageFee());
      accountCapsule = dbManager.getAccountStore().get(accountCapsule.getAddress().toByteArray());
      Assert.assertEquals(deployStorageDelta, accountCapsule.getStorageLimit());
      Assert.assertEquals(deployStorageDelta, accountCapsule.getStorageUsage());
      Assert.assertEquals(0, accountCapsule.getStorageLeft());
      Assert.assertEquals(totalBalance,
          1000_000L + accountCapsule.getBalance() + trace.getReceipt().getStorageFee() + trace
              .getReceipt().getEnergyFee());
    } catch (ContractExeException e) {
      e.printStackTrace();
    } catch (ContractValidateException e) {
      e.printStackTrace();
    } catch (OutOfSlotTimeException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testPay() {
    Account account = Account.newBuilder()
        .setAddress(ownerAddress)
        .setBalance(1000000)
        .setAccountResource(
            AccountResource.newBuilder()
                .setEnergyUsage(this.energyUsage)
                .setFrozenBalanceForEnergy(
                    Frozen.newBuilder()
                        .setExpireTime(100000)
                        .setFrozenBalance(100000)
                        .build())
                .setStorageUsage(this.storageUsage)
                .setStorageLimit(3000)
                .build()).build();

    AccountCapsule accountCapsule = new AccountCapsule(account);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);
    TriggerSmartContract contract = TriggerSmartContract.newBuilder()
        .setContractAddress(contractAddress)
        .setOwnerAddress(ownerAddress)
        .build();

    SmartContract smartContract = SmartContract.newBuilder()
        .setOriginAddress(ownerAddress)
        .setContractAddress(contractAddress)
        .build();

    CreateSmartContract createSmartContract = CreateSmartContract.newBuilder()
        .setOwnerAddress(ownerAddress)
        .setNewContract(smartContract)
        .build();

    Transaction transaction = Transaction.newBuilder()
        .setRawData(
            raw.newBuilder()
                .addContract(
                    Contract.newBuilder()
                        .setParameter(Any.pack(contract))
                        .setType(ContractType.TriggerSmartContract)
                        .build())
                .build()
        )
        .build();

    dbManager.getContractStore().put(
        contractAddress.toByteArray(),
        new ContractCapsule(smartContract));

    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    TransactionTrace transactionTrace = new TransactionTrace(transactionCapsule, dbManager);
    transactionTrace.setBill(this.energyUsage, this.storageUsage);
    transactionTrace.pay();
    AccountCapsule accountCapsule1 = dbManager.getAccountStore().get(ownerAddress.toByteArray());
  }

  /**
   * destroy clear data of testing.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }
}
