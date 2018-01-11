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

package org.tron.peer;

import static org.tron.crypto.Hash.sha3;

import java.math.BigInteger;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;

import org.tron.core.BlockUtils;
import org.tron.protos.core.TronBlock.Block;
import org.tron.utils.FastByteComparisons;
import org.tron.utils.TypeConversion;

public class Validation implements ValidationRule {
  private boolean isStop;

  @Override
  public byte[] start(Block block) {
    isStop = false;
    BigInteger max = BigInteger.valueOf(2).pow(255);
    byte[] target = BigIntegers.asUnsignedByteArray(32,
        max.divide(new BigInteger(1, block.getBlockHeader()
            .getDifficulty()
            .toByteArray())));

    byte[] testNonce = new byte[32];
    byte[] concat;

    while (TypeConversion.increment(testNonce) && !isStop) {

      if (testNonce[31] == 0 && testNonce[30] == 0) {
        System.out.println("mining: " + new BigInteger(1, testNonce));
      }

      if (testNonce[31] == 0) {
        sleep();
      }
      concat = Arrays.concatenate(BlockUtils.prepareData(block),
          testNonce);
      byte[] result = sha3(concat);
      if (FastByteComparisons.compareTo(result, 0, 32, target, 0, 32) <
          0) {
        System.out.println("mined success");
        return testNonce;
      }
    }
    return new byte[] {};
  }

  @Override
  public void stop() {
    this.isStop = true;
  }

  @Override
  public boolean validate(Block block) {
    byte[] proof = BlockUtils.getMineValue(block);
    byte[] boundary = BlockUtils.getPowBoundary(block);

    return FastByteComparisons.compareTo(proof, 0, 32, boundary, 0, 32) <= 0;
  }

  private void sleep() {
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
