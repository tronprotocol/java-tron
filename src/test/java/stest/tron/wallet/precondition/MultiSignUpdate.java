package stest.tron.wallet.precondition;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MultiSignUpdate {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = "47.94.239.172:50051";
  String[] permissionKeyString = new String[5];
  String[] ownerKeyString = new String[1];
  String accountPermissionJson = "";
  //TYtZeP1Xnho7LKcgeNsTY2Xg3LTpjfF6G5
  String ownerKey = "795D7F7A3120132695DFB8977CC3B7ACC9770C125EB69037F19DCA55B075C4AE";

  //TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE
  String manager1Key = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  //TWKKwLswTTcK5cp31F2bAteQrzU8cYhtU5
  String manager2Key = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
  //TT4MHXVApKfbcq7cDLKnes9h9wLSD4eMJi
  String manager3Key = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb8";
  //TCw4yb4hS923FisfMsxAzQ85srXkK6RWGk
  String manager4Key = "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d";
  //TLYUrci5Qw5fUPho2GvFv38kAK4QSmdhhN
  String manager5Key = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";


  private final byte[] ownerAddress = PublicMethed.getFinalAddress(ownerKey);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void testMutiSign1CreateAssetissue() {

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    permissionKeyString[2] = manager3Key;
    permissionKeyString[3] = manager4Key;
    permissionKeyString[4] = manager5Key;

    ownerKeyString[0] = ownerKey;
//    accountPermissionJson =
//        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
//            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
//            + "\",\"weight\":1}]},"
//            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
//            + "\"operations\":\"0200000000000000000000000000000000000000000000000000000000000000\","
//            + "\"keys\":["
//            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
//            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
//            + "]}]}";

    accountPermissionJson =
            "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":5,\"keys\":["
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager3Key) + "\",\"weight\":1}"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager4Key) + "\",\"weight\":1}"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager5Key) + "\",\"weight\":1}]},"
                    + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
                    + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
                    + "\"keys\":["
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
                    + "]}]}";

    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,ownerAddress,ownerKey,
            blockingStubFull,ownerKeyString);

  }
  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


