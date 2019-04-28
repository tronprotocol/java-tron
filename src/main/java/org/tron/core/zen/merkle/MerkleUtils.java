package org.tron.core.zen.merkle;

import java.util.ArrayList;
import java.util.List;

public class MerkleUtils {

  public static List<Boolean> convertBytesVectorToVector(final byte[] bytes) {
    List<Boolean> ret = new ArrayList<>();

    byte c;
    for (int i = 0; i < bytes.length; i++) {
      c = bytes[i];
      for (int j = 0; j < 8; j++) {
        ret.add(((c >> (7 - j)) & 1) == 1);
      }
    }

    return ret;
  }
}
