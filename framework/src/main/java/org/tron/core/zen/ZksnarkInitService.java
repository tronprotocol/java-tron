package org.tron.core.zen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.exception.TronError;
import org.tron.core.exception.ZksnarkException;

@Slf4j(topic = "API")
@Component
public class ZksnarkInitService {

  @PostConstruct
  private void init() {
    librustzcashInitZksnarkParams();
  }

  public static void librustzcashInitZksnarkParams() {
    logger.info("init zk param begin");

    if (!JLibrustzcash.isOpenZen()) {
      logger.info("zen switch is off, zen will not start.");
      return;
    }

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "25fd9a0d1c1be0526c14662947ae95b758fe9f3d7fb7f55e9b4437830dcc6215a7ce3ea465"
        + "914b157715b7a4d681389ea4aa84438190e185d5e4c93574d3a19a";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "a1cb23b93256adce5bce2cb09cefbc96a1d16572675ceb691e9a3626ec15b5b546926ff1c"
        + "536cfe3a9df07d796b32fdfc3e5d99d65567257bf286cd2858d71a6";

    try {
      JLibrustzcash.librustzcashInitZksnarkParams(
          new LibrustzcashParam.InitZksnarkParams(spendPath, spendHash, outputPath, outputHash));
    } catch (ZksnarkException e) {
      throw new TronError(e, TronError.ErrCode.ZCASH_INIT);
    }
    logger.info("init zk param done");
  }

  private static String getParamsFile(String fileName) {
    InputStream in = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("params" + File.separator + fileName);
    File fileOut = new File(System.getProperty("java.io.tmpdir")
        + File.separator + fileName + "." + System.currentTimeMillis());
    try {
      FileUtils.copyToFile(in, fileOut);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    if (fileOut.exists()) {
      fileOut.deleteOnExit();
    }
    return fileOut.getAbsolutePath();
  }
}