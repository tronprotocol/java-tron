package org.tron.stresstest.dispatch.creator.asset;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.api.GrpcAPI.ReceiveNote;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Configuration;
import org.tron.core.Wallet;
import org.tron.core.exception.ZksnarkException;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.AbiUtil;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.stresstest.exception.EncodingException;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.stresstest.dispatch.AbstractTransactionCreator;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
//import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.stresstest.exception.EncodingException;
import org.tron.common.utils.ShieldAddressInfo;


@Setter
public class ShieldTransactionCreator extends AbstractTransactionCreator implements GoodCaseTransactonCreator {
  //private String assetName = zentokenid;
  private String zenTokenOwnerAddress = Configuration.getByPath("stress.conf").getString("address.zenTokenOwnerAddress");
  private String toAddress = commonToAddress;
  private long zenTokenFee = Configuration.getByPath("stress.conf").getLong("param.zenTokenFee");
  private String privateKey = Configuration.getByPath("stress.conf").getString("privateKey.zenTokenOwnerKey");
  List<Note> shieldOutList = new ArrayList<>();
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("stress.conf").getStringList("fullnode.ip.list")
      .get(0);


  @Override
  protected Protocol.Transaction create() {
    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());
    byte[] zenTokenOwnerAddressBytes = Wallet.decodeFromBase58Check(zenTokenOwnerAddress);
    Optional<ShieldAddressInfo> shieldAddressInfo1 = generateShieldAddress();
    String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    String memo1 = "Stress test for public to shield transaction on " + System.currentTimeMillis();
    shieldOutList.clear();
    shieldOutList = addShieldOutputList(shieldOutList, shieldAddress1,
        "" + zenTokenFee, memo1);


/*    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);*/




    Protocol.Transaction transaction = publicToShieldedTransferContract(zenTokenOwnerAddressBytes, 2 * zenTokenFee,shieldOutList,blockingStubFull);
    Any any = transaction.getRawData().getContract(0).getParameter();
    try {
      Contract.ShieldedTransferContract shieldedTransferContract =
          any.unpack(Contract.ShieldedTransferContract.class);
    } catch (Exception e) {
      System.out.println(e);
    }
    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    try{
      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      }
    } catch (Exception e) {
      System.out.println(e);
    }


    return transaction;





  }




}
