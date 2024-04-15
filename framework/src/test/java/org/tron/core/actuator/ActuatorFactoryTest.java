package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.contract.BalanceContract.TransferContract;

public class ActuatorFactoryTest extends BaseTest {

  private static final String OWNER_ADDRESS = Wallet.getAddressPreFixString()
          + "548794500882809695a8a687866e76d4271a1abc";
  private static final String TO_ADDRESS = Wallet.getAddressPreFixString()
          + "abd4b9367799eaa3197fecb144eb71de1e049abc";

  static {
    Args.setParam(
            new String[] {
                "--output-directory", dbPath()
            },
            Constant.TEST_CONF
    );
  }

  private TransferContract getContract(long count, String owneraddress, String toaddress) {
    return TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owneraddress)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toaddress)))
            .setAmount(count)
            .build();
  }

  @Before
  public void createCapsule() {
    AccountCapsule ownerCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                    AccountType.Normal,
                    10000L);
    AccountCapsule toAccountCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("toAccount"),
                    ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
                    AccountType.Normal,
                    10L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }


  @Test
  public void testCreateActuator() {
    TransferContract contract = getContract(10L, OWNER_ADDRESS, TO_ADDRESS);
    TransactionCapsule trx = new TransactionCapsule(contract,
            chainBaseManager.getAccountStore());
    List<Actuator> actList = ActuatorFactory.createActuator(trx, chainBaseManager);

    Assert.assertEquals(1, actList.size());
  }

}
