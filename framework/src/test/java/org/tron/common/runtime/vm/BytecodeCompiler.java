package org.tron.common.runtime.vm;

import org.bouncycastle.util.encoders.Hex;
import org.tron.core.vm.Op;

import java.util.ArrayList;
import java.util.List;

public class BytecodeCompiler {
  public byte[] compile(String code) {
    return compile(code.split("\\s+"));
  }

  private byte[] compile(String[] tokens) {
    List<Byte> bytecodes = new ArrayList<>();
    int ntokens = tokens.length;

    for (int i = 0; i < ntokens; i++) {
      String token = tokens[i].trim().toUpperCase();

      if (token.isEmpty())
        continue;

      if (isHexadecimal(token))
        compileHexadecimal(token, bytecodes);
      else
        bytecodes.add(Op.getOpOf(token));
    }

    int nbytes = bytecodes.size();
    byte[] bytes = new byte[nbytes];

    for (int k = 0; k < nbytes; k++)
      bytes[k] = bytecodes.get(k).byteValue();

    return bytes;
  }

  private static boolean isHexadecimal(String token) {
    return token.startsWith("0X");
  }

  private static void compileHexadecimal(String token, List<Byte> bytecodes) {
    byte[] bytes = Hex.decode(token.substring(2));

    for (int k = 0; k < bytes.length; k++)
      bytecodes.add(bytes[k]);
  }
}
