package org.tron.core.zksnark;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutCiphertext;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;

@Slf4j
public class NoteEncDecryTest {

  private static final String dbPath = "note_encdec_test";
  private static final String FROM_ADDRESS;
  private static final String ADDRESS_ONE_PRIVATE_KEY;
  private static final long OWNER_BALANCE = 100_000_000;
  private static final long FROM_AMOUNT = 110_000_000;
  private static final long tokenId = 1;
  private static final String ASSET_NAME = "trx";
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Wallet wallet;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, "config-localtest.conf");
    context = new TronApplicationContext(DefaultConfig.class);
    FROM_ADDRESS = Wallet.getAddressPreFixString() + "a7d8a35b260395c14aa456297662092ba3b76fc0";
    ADDRESS_ONE_PRIVATE_KEY = "7f7f701e94d4f1dd60ee5205e7ea8ee31121427210417b608a6b2e96433549a7";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    wallet = context.getBean(Wallet.class);
    dbManager = context.getBean(Manager.class);
    //give a big value for pool, avoid for
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(10_000_000_000L);
    // Args.getInstance().setAllowShieldedTransaction(1);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();

    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createToken() {
    Args.getInstance().setZenTokenId(String.valueOf(tokenId));
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);

    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(FROM_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(tokenId))
            .setTotalSupply(OWNER_BALANCE)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
  }

  @Test
  public void testDecryptWithOvk() throws ZksnarkException {
    //input
    OutCiphertext outCiphertext = new OutCiphertext();
    outCiphertext.setData(new byte[]{41, -103, -114, -122, -5, 19, 19, -98, 68, 30, -102, 44, -24,
        -23, 19, 26,
        -82, 83, -19, 49, -30, 107, -82, -41, -66, 115, -94, -89, -29, -80, 7, -64,
        25, -27, 108, -75, 91, 120, 82, -95, -121, 34, 14, -87, 33, 97, 113, 45,
        30, 47, -120, -6, 51, 55, -35, 54, -100, -60, 72, 1, 22, -97, -51, -70,
        -72, -83, -128, 3, -15, 77, -4, 101, 104, 80, -75, 4, 24, -32, 104, 57});

    byte[] ovk = {-91, -41, -115, 8, -94, 69, 15, -49, -44, 69, -65, 38, 15, -115, 53, -47,
        48, 54, 106, -123, 126, -12, 3, -104, 18, 20, 57, -39, -114, -72, 74, -118};
    byte[] cv = {-122, -27, -112, -34, -69, 8, 53, 93, -79, -3, 97, 27, 87, 67, 58, -66,
        8, 83, -17, -113, 119, 127, 97, 36, -83, 75, -21, -28, 0, -114, 21, -50};
    byte[] cm = {-121, 97, -112, -111, -78, -25, -53, 114, -41, -111, 45, 35, 99, -119, 9, -124,
        48, 124, -49, -107, 6, -55, -13, 25, -47, 78, 114, 59, 27, 78, -41, 18};
    byte[] epk = {37, -37, 66, -69, 121, -18, -51, 5, 68, -114, 27, -24, -21, -79, -50, 111,
        51, 58, 31, -13, -28, 94, 24, -104, 92, -107, 78, -74, 17, -89, 30, 71};

    //output
    Optional<OutgoingPlaintext> ret = OutgoingPlaintext
        .decrypt(outCiphertext, ovk, cv, cm, epk);
    OutgoingPlaintext result = ret.get();

    byte[] pkD = {-115, 5, -71, -104, -116, -55, -12, 33, 108, -64, -122, -21, 126, 79, -20, 3,
        58, -81, -63, 8, -11, -122, 107, 119, 11, -1, -70, 90, 103, 91, -69, -103};
    byte[] esk = {-108, -3, 17, -93, 49, 22, -101, -67, 45, -22, 114, 108, -19, 89, -108, 124,
        113, 99, 88, -6, 77, 75, 53, -20, -12, 45, 29, 90, -31, -113, 100, 2};

    Assert.assertArrayEquals(result.getPkD(), pkD);
    Assert.assertArrayEquals(result.getEsk(), esk);
  }

  @Test
  public void testDecryptEncWithEpk() throws ZksnarkException {
    //input
    Encryption.EncCiphertext enc = new Encryption.EncCiphertext();
    enc.setData(new byte[]{
        -113, 13, -92, 109, 3, -75, -15, -79, -102, -125, -17, 25, 68, -57, 13, -70,
        60, -3, -93, -37, 35, 31, 38, -52, -24, -125, -46, -40, 45, 37, 120, -45,
        -50, 99, -114, -22, -89, 94, 11, 119, -62, -19, -60, -90, 17, -99, 3, -5,
        77, 48, -81, -30, -1, -89, 9, -38, 94, 40, -82, -43, -55, 59, 62, -111,
        -104, 84, 56, 15, -24, -123, -6, 38, -123, -38, -11, -50, -12, 86, -2, 30,
        117, 3, 41, 95, 106, -97, -59, 25, 34, -96, 32, -100, -3, 107, -20, -83,
        -75, 35, 12, -5, -18, -41, -29, -71, -98, 118, -52, 55, 98, 15, -87, 76,
        79, 21, -104, 10, 69, 59, -69, 73, 111, -77, -66, -40, 93, 112, -44, 19,
        18, -125, -108, 6, 51, 40, 69, 109, -56, 5, -86, 84, 62, -32, -40, -60,
        93, 79, -33, -81, -46, -46, -39, -48, 95, -120, 40, -116, -20, -29, 79, -31,
        47, -48, -26, 47, -48, 21, -116, 76, 83, 54, 5, -59, -63, 119, -81, 101,
        89, -53, 1, -105, -50, 45, -43, -114, -1, -10, -109, 85, -85, -74, 90, -84,
        113, 85, -98, 85, 14, 67, 16, 77, 110, 5, 47, -64, -82, 24, 36, 28,
        -46, 14, -117, -42, -52, -46, 70, -22, -83, -85, -46, -13, -16, 6, 33, 13,
        75, 12, -50, 123, -67, 40, -46, -123, 89, 75, 4, -8, -27, 124, -83, 112,
        122, 13, 5, 16, -13, -55, -14, -105, 60, 58, 115, 54, 94, 65, -43, -35,
        -77, 23, 81, -25, 2, 111, 3, -71, -54, -45, -121, 107, -53, 39, -71, 82,
        -63, -79, -127, -120, -90, 74, 47, 103, -26, 106, 41, -68, 26, -30, 22, -18,
        46, -110, -35, -83, 92, 63, -27, -72, 56, -54, 54, -80, -36, 95, -114, -99,
        -13, -51, 89, 93, -48, -116, 112, 56, 24, 42, -46, -119, -85, -70, -49, 51,
        -21, -97, -86, 23, 103, 116, -126, 4, 25, -16, 5, -78, 95, 30, -69, 13,
        -69, -44, -120, -97, 60, 108, 68, -9, 98, 84, 95, 69, 116, 122, -69, 75,
        93, -100, 88, 113, 7, -43, -74, 12, 76, 35, -44, -10, 98, -115, -125, -91,
        58, -84, 19, -76, -26, -86, 3, -24, 121, 20, 33, -105, 93, 123, 107, -64,
        106, -92, 24, -108, 11, 123, 125, -52, 28, -8, 62, -117, -107, 60, -101, 29,
        -99, -74, -34, 101, 1, -13, -38, 40, -24, -59, -83, -94, 28, -13, -2, 74,
        80, 97, 72, 62, -112, -86, 48, -128, -14, -104, -37, 54, 11, 32, -57, -90,
        60, -8, -127, 53, 94, 71, 54, 77, -118, 27, 29, -33, -107, 31, 79, -4,
        27, 84, 83, -105, 54, 24, 30, -9, -7, -17, 51, -10, -72, 122, 123, 42,
        10, 47, 118, -71, -97, 102, 82, -105, 3, -87, -41, 103, 1, 24, 48, 22,
        21, -110, -85, 0, 96, -90, -89, -119, -36, 112, -68, -75, -127, -29, -53, 103,
        -36, -122, -82, -17, -6, -48, 18, 124, 5, -115, -93, 56, 15, -44, 80, 120,
        -92, 101, 110, 98, 40, -1, -56, -77, 0, -38, 72, 39, 115, 49, -59, 101,
        61, 73, -61, 39, -65, -125, -86, 64, 26, 114, -43, 74, 96, -46, -1, 30,
        105, -17, 120, -16, 18, -120, 35, 85, -5, 67, 47, -62, -49, -127, -11, -58,
        -97, -86, -23, -102, 120, -59, 26, -8, -3, 80, 23, 40, 111, -62, 85, -118,
        -92, 84, -72, 37});

    byte[] cmu_opt = new byte[]{-105, 84, 5, 15, -9, -62, -76, -32, -67, -117, -66, 9, 2, -85, -104,
        91,
        11, -74, -114, 22, -68, 48, -6, 116, 104, 51, 34, 3, 56, -64, -128, 71};

    //same as last case
    byte[] epk = new byte[]{37, -37, 66, -69, 121, -18, -51, 5, 68, -114, 27, -24, -21, -79, -50,
        111,
        51, 58, 31, -13, -28, 94, 24, -104, 92, -107, 78, -74, 17, -89, 30, 71};

    //output of last case
    byte[] pkD = new byte[]{-115, 5, -71, -104, -116, -55, -12, 33, 108, -64, -122, -21, 126, 79,
        -20, 3,
        58, -81, -63, 8, -11, -122, 107, 119, 11, -1, -70, 90, 103, 91, -69, -103};
    byte[] esk = new byte[]{-108, -3, 17, -93, 49, 22, -101, -67, 45, -22, 114, 108, -19, 89, -108,
        124,
        113, 99, 88, -6, 77, 75, 53, -20, -12, 45, 29, 90, -31, -113, 100, 2};

    OutgoingPlaintext outgoingPlaintext = new OutgoingPlaintext(pkD, esk);

    //output
    Optional<Note> ret2 = Note.decrypt(
        enc, epk, outgoingPlaintext.getEsk(), outgoingPlaintext.getPkD(), cmu_opt);
    Note result2 = ret2.get();

    byte[] rcm = new byte[]{-125, -45, 111, -44, -56, -18, -66, -59, 22, -61, -88, -50, 47, -28,
        -125, 46,
        1, -21, 87, -67, 127, -97, -100, -98, 11, -42, -116, -58, -102, 91, 15, 6};
    byte[] d = new byte[]{90, -81, -67, -95, 91, 121, 13, 56, 99, 112, 23};
    Assert.assertArrayEquals(d, result2.getD().getData());
    Assert.assertArrayEquals(rcm, result2.getRcm());
    Assert.assertEquals(4000, result2.getValue());
  }
}
