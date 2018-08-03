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

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.SmartContract;

@RunWith(Parameterized.class)
public class TransactionTraceTest {

  private static String dbPath = "output_TransactionTrace_test";
  private static String dbDirectory = "db_TransactionTrace_test";
  private static String indexDirectory = "index_TransactionTrace_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;

  private static ByteString ownerAddress = ByteString.copyFrom(ByteArray.fromInt(1));
  private static ByteString contractAddress = ByteString.copyFrom(ByteArray.fromInt(2));

  private long cpuUsage;
  private long storageUsage;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w"
        },
        Constant.TEST_CONF
    );
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  public TransactionTraceTest(long cpuUsage, long storageUsage) {
    this.cpuUsage = cpuUsage;
    this.storageUsage = storageUsage;
  }

  /**
   * resourceUsage prepare data for testing.
   */
  @Parameters
  public static Collection resourceUsage() {
    return Arrays.asList(new Object[][]{

        {0, 0},
        {6, 1000},
        {7, 1000},
        {10, 999},
        {13, 1000},
        {14, 1000},
        {20, 1000},
        {10, 1000},
        {10, 1001}


    });
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  @Test
  public void testCheckBill() {
//    Transaction transaction = Transaction.newBuilder()
//        .addRet(
//            Result.newBuilder()
//                .setReceipt(
//                    ResourceReceipt.newBuilder()
//                        .setCpuUsage(10)
//                        .setStorageDelta(1000)
//                        .build())
//                .build())
//        .setRawData(
//            raw.newBuilder()
//                .addContract(
//                    Contract.newBuilder()
//                        .setParameter(Any.pack(
//                            TriggerSmartContract.newBuilder()
//                                .setOwnerAddress(ByteString.EMPTY)
//                                .build()))
//                        .setType(ContractType.TriggerSmartContract)
//                        .build())
//                .build()
//        )
//        .build();
//
//    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
//
//    TransactionTrace transactionTrace = new TransactionTrace(transactionCapsule, dbManager);
//
//    transactionTrace.setBill(this.cpuUsage, this.storageUsage);
//
//    try {
//      transactionTrace.checkBill();
//    } catch (ReceiptException e) {
//      e.printStackTrace();
//    }
  }

  @Test
  public void testPay() {
    Account account = Account.newBuilder()
        .setAddress(ownerAddress)
        .setBalance(1000000)
        .setAccountResource(
            AccountResource.newBuilder()
                .setCpuUsage(this.cpuUsage)
                .setFrozenBalanceForCpu(
                    Frozen.newBuilder()
                        .setExpireTime(100000)
                        .setFrozenBalance(100000)
                        .build())
                .setStorageUsage(this.storageUsage)
                .setStorageLimit(3000)
                .build()
        )
        .build();

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

//    Transaction transaction = Transaction.newBuilder()
//        .addRet(
//            Result.newBuilder()
//                .setReceipt(
//                    ResourceReceipt.newBuilder()
//                        .setCpuUsage(10)
//                        .setStorageDelta(1000)
//                        .build())
//                .build())
//        .setRawData(
//            raw.newBuilder()
//                .addContract(
//                    Contract.newBuilder()
//                        .setParameter(Any.pack(contract))
//                        .setType(ContractType.TriggerSmartContract)
//                        .build())
//                .build()
//        )
//        .build();
//
//    dbManager.getContractStore().put(
//        contractAddress.toByteArray(),
//        new ContractCapsule(smartContract));
//
//    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
//
//    TransactionTrace transactionTrace = new TransactionTrace(transactionCapsule, dbManager);
//
//    transactionTrace.setBill(this.cpuUsage, this.storageUsage);
//
//    transactionTrace.pay();
//
//    AccountCapsule accountCapsule1 = dbManager.getAccountStore().get(ownerAddress.toByteArray());
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
