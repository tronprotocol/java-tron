package org.tron.core.zen;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.exception.TronError;
import org.tron.core.exception.ZksnarkException;

@Slf4j(topic = "API")
@Component
public class ZksnarkInitService {

  private static final AtomicBoolean initialized = new AtomicBoolean(false);
  private static final Set<PosixFilePermission> OWNER_ONLY =
      Collections.unmodifiableSet(EnumSet.of(
          PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

  @PostConstruct
  private void init() {
    librustzcashInitZksnarkParams();
  }

  public static void librustzcashInitZksnarkParams() {
    if (initialized.get()) {
      logger.info("zk param already initialized");
      return;
    }

    synchronized (ZksnarkInitService.class) {
      if (initialized.get()) {
        logger.info("zk param already initialized");
        return;
      }
      logger.info("init zk param begin");

      String spendPath = getParamsFile("sapling-spend.params");
      String spendHash = "25fd9a0d1c1be0526c14662947ae95b758fe9f3d7fb7f55e9b4437830dcc6215a7ce3ea"
          + "465914b157715b7a4d681389ea4aa84438190e185d5e4c93574d3a19a";

      String outputPath = getParamsFile("sapling-output.params");
      String outputHash = "a1cb23b93256adce5bce2cb09cefbc96a1d16572675ceb691e9a3626ec15b5b546926f"
          + "f1c536cfe3a9df07d796b32fdfc3e5d99d65567257bf286cd2858d71a6";

      try {
        JLibrustzcash.librustzcashInitZksnarkParams(
            new LibrustzcashParam.InitZksnarkParams(spendPath, spendHash, outputPath, outputHash));
      } catch (ZksnarkException e) {
        throw new TronError(e, TronError.ErrCode.ZCASH_INIT);
      }
      initialized.set(true);
      logger.info("init zk param done");
    }
  }

  @VisibleForTesting
  static String getParamsFile(String fileName) {
    InputStream resource = ZksnarkInitService.class.getResourceAsStream("/params/" + fileName);
    if (resource == null) {
      throw new TronError("Missing zk param resource: " + fileName,
          TronError.ErrCode.ZCASH_INIT);
    }

    Path fileOut = null;
    try (InputStream in = resource) {
      fileOut = copyToSecureTempFile(in, fileName);
      fileOut.toFile().deleteOnExit();
      return fileOut.toAbsolutePath().toString();
    } catch (IOException | RuntimeException e) {
      deleteTempFile(fileOut, e);
      throw new TronError("Failed to release zk param resource: " + fileName, e,
          TronError.ErrCode.ZCASH_INIT);
    }
  }

  @VisibleForTesting
  static Path copyToSecureTempFile(InputStream in, String fileName) throws IOException {
    int dotIndex = fileName.lastIndexOf('.');
    String prefix = (dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName) + "-";
    String suffix = dotIndex > 0 ? fileName.substring(dotIndex) : ".params";
    Path fileOut = createTempFile(prefix, suffix);

    try (OutputStream out = Files.newOutputStream(fileOut,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING,
        LinkOption.NOFOLLOW_LINKS)) {
      IOUtils.copyLarge(in, out);
      return fileOut;
    } catch (IOException | RuntimeException e) {
      deleteTempFile(fileOut, e);
      throw e;
    }
  }

  private static Path createTempFile(String prefix, String suffix) throws IOException {
    try {
      return Files.createTempFile(prefix, suffix,
          PosixFilePermissions.asFileAttribute(OWNER_ONLY));
    } catch (UnsupportedOperationException e) {
      // Non-POSIX providers still create the unpredictable name atomically.
      return Files.createTempFile(prefix, suffix);
    }
  }

  private static void deleteTempFile(Path file, Throwable failure) {
    if (file == null) {
      return;
    }
    try {
      Files.deleteIfExists(file);
    } catch (IOException | RuntimeException cleanupError) {
      failure.addSuppressed(cleanupError);
    }
  }
}
