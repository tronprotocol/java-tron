package org.tron.core.db2.core;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Stable identity for the first production common-checkpoint candidate. */
public final class CommonCheckpointFormat {

  public static final String ID = "java-tron-state-archive-common-checkpoint-v1";
  private static final byte[] DIGEST = Hashing.sha256()
      .hashString(ID, StandardCharsets.UTF_8).asBytes();

  private CommonCheckpointFormat() {
  }

  public static byte[] identity() {
    return Arrays.copyOf(DIGEST, DIGEST.length);
  }
}
