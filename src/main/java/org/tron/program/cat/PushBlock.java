package org.tron.program.cat;

import com.google.protobuf.ByteString;
import java.io.File;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Time;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;

public class PushBlock {
  private static final long COUNT = 1;
  private static final String TEST_CAT = "cat/config-cat.conf";

  private static Manager dbManager = new Manager();
  private static String dbPath = "output_cat_push_block";
  private static long currntNumber = 1;
  private static String currntParentHash = "718595be637d334a6aaae11afeca9b84f031ef5e45c311ac302eacd4c6cbb73b";

  public static void main(String[] args) {
    init();

    testPushBlock();

    removeDb();
  }

  private static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        TEST_CAT);
    dbManager.init();
  }

  private static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    dbManager.destory();
  }

  private static void testPushBlock() {
    for(int i = 0; i < COUNT; ++i) {
      BlockCapsule blockCapsule = createBlockCapsule();

      currntNumber += 1;
      currntParentHash = blockCapsule.getBlockId().toString();

      try {
        dbManager.pushBlock(blockCapsule);
      } catch (ValidateSignatureException e) {
        e.printStackTrace();
      } catch (ContractValidateException e) {
        e.printStackTrace();
      } catch (ContractExeException e) {
        e.printStackTrace();
      } catch (UnLinkedBlockException e) {
        e.printStackTrace();
      } catch (ValidateScheduleException e) {
        e.printStackTrace();
      }
    }
  }

  private static BlockCapsule createBlockCapsule() {
    BlockCapsule blockCapsule = new BlockCapsule(currntNumber, ByteString.copyFrom(ByteArray
        .fromHexString(currntParentHash)),
        Time.getCurrentMillis(),
        ByteString.copyFrom(
            ECKey.fromPrivate(ByteArray
                .fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()))
                .getAddress()));



    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

    return blockCapsule;
  }
}
