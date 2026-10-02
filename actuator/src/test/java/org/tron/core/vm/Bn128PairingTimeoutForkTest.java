package org.tron.core.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.tron.core.vm.program.Program.OutOfTimeException;

public class Bn128PairingTimeoutForkTest {

  @Test
  public void expiredDeadlineTimesOut() {
    PrecompiledContracts.BN128Pairing pairing = pairing(0);
    assertThrows(OutOfTimeException.class, () -> pairing.execute(onePair()));
  }

  @Test
  public void futureDeadlineReturnsPairingResult() {
    PrecompiledContracts.BN128Pairing pairing = pairing(Long.MAX_VALUE / 1000);
    Pair<Boolean, byte[]> out = pairing.execute(onePair());
    assertTrue(out.getLeft());
    assertEquals(32, out.getRight().length);
  }

  private static PrecompiledContracts.BN128Pairing pairing(long vmShouldEndInUs) {
    PrecompiledContracts.BN128Pairing pairing = new PrecompiledContracts.BN128Pairing();
    pairing.setVmShouldEndInUs(vmShouldEndInUs);
    return pairing;
  }

  private static byte[] onePair() {
    byte[] input = new byte[192];
    write(input, 0, BigInteger.ONE);
    write(input, 32, BigInteger.valueOf(2));
    write(input, 64, new BigInteger(
        "11559732032986387107991004021392285783925812861821192530917403151452391805634"));
    write(input, 96, new BigInteger(
        "10857046999023057135944570762232829481370756359578518086990519993285655852781"));
    write(input, 128, new BigInteger(
        "4082367875863433681332203403145435568316851327593401208105741076214120093531"));
    write(input, 160, new BigInteger(
        "8495653923123431417604973247489272438418190587263600148770280649306958101930"));
    return input;
  }

  private static void write(byte[] dest, int offset, BigInteger value) {
    byte[] raw = value.toByteArray();
    int src = raw.length > 32 ? raw.length - 32 : 0;
    System.arraycopy(raw, src, dest, offset + 32 - (raw.length - src), raw.length - src);
  }
}
