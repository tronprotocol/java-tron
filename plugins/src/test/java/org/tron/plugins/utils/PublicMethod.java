package org.tron.plugins.utils;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

/**
 * @author liukai
 * @since 2023/5/24.
 */
public class PublicMethod {

  public static byte[] getFinalAddress(String priKey) {
    Wallet.setAddressPreFixByte((byte) 0x41);
    ECKey key = ECKey.fromPrivate(new BigInteger(priKey, 16));
    return key.getAddress();
  }

  public static Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey,
                                 WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte((byte) 0x41);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    int times = 0;
    while (times++ <= 2) {

      BalanceContract.TransferContract.Builder builder =
              BalanceContract.TransferContract.newBuilder();
      com.google.protobuf.ByteString bsTo = com.google.protobuf.ByteString.copyFrom(to);
      com.google.protobuf.ByteString bsOwner = ByteString.copyFrom(owner);
      builder.setToAddress(bsTo);
      builder.setOwnerAddress(bsOwner);
      builder.setAmount(amount);

      BalanceContract.TransferContract contract = builder.build();
      Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        continue;
      }
      transaction = signTransaction(ecKey, transaction);
      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      return response.getResult();
    }
    return false;
  }

  /**
   * Sign TX.
   *
   * @param ecKey       ecKey of the private key
   * @param transaction transaction object
   */
  public static Protocol.Transaction signTransaction(ECKey ecKey,
                                                     Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      return null;
    }
    transaction = setTimestamp(transaction);
    return sign(transaction, ecKey);
  }

  /**
   * Broadcast TX.
   *
   * @param transaction      transaction object
   * @param blockingStubFull Grpc interface
   */
  public static GrpcAPI.Return broadcastTransaction(
          Protocol.Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    while (!response.getResult() && response.getCode() == GrpcAPI.Return.response_code.SERVER_BUSY
            && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
    }
    return response;

  }

  /**
   * constructor.
   */

  public static Protocol.Transaction setTimestamp(Protocol.Transaction transaction) {
    long currentTime = System.currentTimeMillis();//*1000000 + System.nanoTime()%1000000;
    Protocol.Transaction.Builder builder = transaction.toBuilder();
    org.tron.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
            .toBuilder();
    rowBuilder.setTimestamp(currentTime);
    builder.setRawData(rowBuilder.build());
    return builder.build();
  }

  public static Protocol.Transaction sign(Protocol.Transaction transaction, ECKey myKey) {
    ByteString lockSript = ByteString.copyFrom(myKey.getAddress());
    Protocol.Transaction.Builder transactionBuilderSigned = transaction.toBuilder();

    byte[] hash = Sha256Hash.hash(CommonParameter
            .getInstance().isECKeyCryptoEngine(), transaction.getRawData().toByteArray());
    List<Protocol.Transaction.Contract> listContract = transaction.getRawData().getContractList();
    for (int i = 0; i < listContract.size(); i++) {
      ECKey.ECDSASignature signature = myKey.sign(hash);
      ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
      transactionBuilderSigned.addSignature(
              bsSign);//Each contract may be signed with a different private key in the future.
    }

    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  public static int chooseRandomPort() {
    return chooseRandomPort(10240, 65000);
  }

  public static int chooseRandomPort(int min, int max) {
    int port = new Random().nextInt(max - min + 1) + min;
    try {
      while (!checkPortAvailable(port)) {
        port = new Random().nextInt(max - min + 1) + min;
      }
    } catch (IOException e) {
      return new Random().nextInt(max - min + 1) + min;
    }
    return port;
  }

  private static boolean checkPortAvailable(int port) throws IOException {
    InetAddress theAddress = InetAddress.getByName("127.0.0.1");
    try (Socket socket = new Socket(theAddress, port)) {
      // only check
      socket.getPort();
    } catch (IOException e) {
      return true;
    }
    return false;
  }
}