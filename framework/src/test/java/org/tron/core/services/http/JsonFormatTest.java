package org.tron.core.services.http;

import java.io.IOException;
import javax.annotation.Resource;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.tron.api.GrpcAPI.AccountNetMessage;

import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol;

public class JsonFormatTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final long initBalance = 43_200_000_000L;
  @Resource
  private Wallet wallet;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
  }

  public void init() {
    AccountCapsule ownerCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                    Protocol.AccountType.Normal,
                    initBalance);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  @Test
  public void testPrint() {
    ByteString addressByte = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule accountCapsule =
            new AccountCapsule(Protocol.Account.newBuilder().setAddress(addressByte).build());
    accountCapsule.setBalance(1000_000_000L);
    dbManager.getChainBaseManager().getAccountStore()
            .put(accountCapsule.createDbKey(), accountCapsule);

    AccountNetMessage accountNet = wallet.getAccountNet(addressByte);
    StringBuilder text = new StringBuilder();
    try {
      JsonFormat.print(accountNet, text, false);
    } catch (IOException e) {
      Assert.fail("print failed");
    }
  }

}
