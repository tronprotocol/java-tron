package org.tron.common.runtime.vm;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.tron.core.vm.Op;

public class BytecodeCompiler {

  public byte[] compile(String code) {
    return compile(code.split("\\s+"));
  }

  private byte[] compile(String[] tokens) {
    List<Byte> bytecodes = new ArrayList<>();
    int ntokens = tokens.length;

    for (String s : tokens) {
      String token = s.trim().toUpperCase();

      if (token.isEmpty()) {
        continue;
      }

      if (isHexadecimal(token)) {
        compileHexadecimal(token, bytecodes);
      } else {
        bytecodes.add(Op.getOpOf(token));
      }
    }

    int nBytes = bytecodes.size();
    byte[] bytes = new byte[nBytes];

    for (int k = 0; k < nBytes; k++) {
      bytes[k] = bytecodes.get(k);
    }

    return bytes;
  }

  private static boolean isHexadecimal(String token) {
    return token.startsWith("0X");
  }

  private static void compileHexadecimal(String token, List<Byte> bytecodes) {
    byte[] bytes = Hex.decode(token.substring(2));

    for (byte aByte : bytes) {
      bytecodes.add(aByte);
    }
  }
}
