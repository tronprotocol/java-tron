package org.tron.core.zksnark;

import java.util.List;
import org.testng.collections.Lists;
import org.tron.common.zksnark.sapling.RpcWallet;
import org.tron.common.zksnark.sapling.ShieldCoinConstructor;
import org.tron.common.zksnark.sapling.TransactionBuilder.TransactionBuilderResult;
import org.tron.common.zksnark.sapling.transaction.Recipient;

public class SendCoinShieldTest {

  static RpcWallet wallet = new RpcWallet();

  public static void main(String[] args) {
    String fromAddr = wallet.getNewAddress();

    List<Recipient> outputs = Lists.newArrayList();
    Recipient recipient = new Recipient();
    recipient.address = wallet.getNewAddress();
    recipient.value = 1000_000L;
    recipient.memo = "demo";

    ShieldCoinConstructor constructor =
        new ShieldCoinConstructor(fromAddr, outputs);
    TransactionBuilderResult result = constructor.build();
  }
}
