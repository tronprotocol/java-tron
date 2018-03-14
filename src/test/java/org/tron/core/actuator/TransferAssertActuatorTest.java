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

package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;

@Ignore
public class TransferAssertActuatorTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  private static Manager dbManager;
  private static Any contract;
  private static final String ASSET_NAME = "trx";
  private static final String OWNER_ADDRESS = "abd4b9367799eaa3197fecb144eb71de1e049150";
  private static final String TO_ADDRESS = "548794500882809695a8a687866e76d4271a146a";
  private static final long AMOUNT = 100L;

  private static final long TOTAL_SUPPLY = 10L;
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int DECAY_RATIO = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";

  /**
   * Init data.
   */
  @Ignore
  public void create() {
    Args.setParam(new String[]{"--storage-directory", "contract-test"},
        Configuration.getByPath("config-junit.conf"));
    dbManager = new Manager();
    dbManager.init();

    contract = Any.pack(
        Contract.TransferAssertContract
            .newBuilder()
            .setAssertName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(AMOUNT)
            .build());

    AssetIssueContract assetIssueContract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setTotalSupply(TOTAL_SUPPLY)
        .setTrxNum(TRX_NUM)
        .setNum(NUM)
        .setStartTime(START_TIME)
        .setEndTime(END_TIME)
        .setDecayRatio(DECAY_RATIO)
        .setVoteScore(VOTE_SCORE)
        .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
        .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
        .build();

    AssetIssueCapsule capsule = new AssetIssueCapsule(assetIssueContract);


  }

  /**
   * Unit test.
   */
  @Ignore
  public void right() {
    TransferAssertActuator actuator = new TransferAssertActuator(contract, dbManager);
    try {
      actuator.validate();
//      actuator.execute();
    } catch (ContractValidateException e) {
      e.printStackTrace();
    }
  }

  /**
   * Release resources.
   */
  @Ignore
  public void destroy() {
    if (dbManager != null) {
      dbManager.close();
    }

    String filePath = Args.getInstance().getOutputDirectory() + "contract-test";

    if (FileUtil.deleteDir(new File(filePath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}
