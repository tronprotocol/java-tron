package stest.tron.wallet.dailybuild.tvmnewcommand.clearabi;

import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.WalletClient;

public class test {
  public static void main(String args[]){
    long x = 27;
    System.out.println(18 >= x * 7/10);

    String GateWayAddress = "TMRzf7sA32t9YCkk5TsospfhNbqLEueWeT";
    byte[] a = WalletClient.decodeFromBase58Check(GateWayAddress);
    String b = ByteArray.toHexString(a);
    System.out.println("Address : " + b);
  }
}
