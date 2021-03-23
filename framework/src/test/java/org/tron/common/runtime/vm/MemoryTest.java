/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.runtime.vm;

import static java.lang.Math.ceil;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.core.vm.program.Memory;

@Slf4j
public class MemoryTest {

  private static final int WORD_SIZE = 32;
  private static final int CHUNK_SIZE = 1024;

  private static void checkMemoryExtend(int dataSize) {
    Memory memory = new Memory();
    memory.extend(0, dataSize);
    Assert.assertEquals(calcSize(dataSize, CHUNK_SIZE), memory.internalSize());
    Assert.assertEquals(calcSize(dataSize, WORD_SIZE), memory.size());
  }

  private static int calcSize(int dataSize, int chunkSize) {
    return (int) ceil((double) dataSize / chunkSize) * chunkSize;
  }

  @Test
  public void testExtend() {
    checkMemoryExtend(0);
    checkMemoryExtend(1);
    checkMemoryExtend(WORD_SIZE);
    checkMemoryExtend(WORD_SIZE * 2);
    checkMemoryExtend(CHUNK_SIZE - 1);
    checkMemoryExtend(CHUNK_SIZE);
    checkMemoryExtend(CHUNK_SIZE + 1);
    checkMemoryExtend(2000);
  }

  @Test
  public void memorySave_1() {

    Memory memoryBuffer = new Memory();
    byte[] data = {1, 1, 1, 1};

    memoryBuffer.write(0, data, data.length, false);

    assertTrue(1 == memoryBuffer.getChunks().size());

    byte[] chunk = memoryBuffer.getChunks().get(0);
    assertTrue(chunk[0] == 1);
    assertTrue(chunk[1] == 1);
    assertTrue(chunk[2] == 1);
    assertTrue(chunk[3] == 1);
    assertTrue(chunk[4] == 0);

    assertTrue(memoryBuffer.size() == 32);
  }

  @Test
  public void memorySave_2() {

    Memory memoryBuffer = new Memory();
    byte[] data = Hex.decode("0101010101010101010101010101010101010101010101010101010101010101");

    memoryBuffer.write(0, data, data.length, false);

    assertTrue(1 == memoryBuffer.getChunks().size());

    byte[] chunk = memoryBuffer.getChunks().get(0);
    assertTrue(chunk[0] == 1);
    assertTrue(chunk[1] == 1);

    assertTrue(chunk[30] == 1);
    assertTrue(chunk[31] == 1);
    assertTrue(chunk[32] == 0);

    assertTrue(memoryBuffer.size() == 32);
  }

  @Test
  public void memorySave_3() {

    Memory memoryBuffer = new Memory();
    byte[] data = Hex.decode("010101010101010101010101010101010101010101010101010101010101010101");

    memoryBuffer.write(0, data, data.length, false);

    assertTrue(1 == memoryBuffer.getChunks().size());

    byte[] chunk = memoryBuffer.getChunks().get(0);
    assertTrue(chunk[0] == 1);
    assertTrue(chunk[1] == 1);

    assertTrue(chunk[30] == 1);
    assertTrue(chunk[31] == 1);
    assertTrue(chunk[32] == 1);
    assertTrue(chunk[33] == 0);

    assertTrue(memoryBuffer.size() == 64);
  }

  @Test
  public void memorySave_4() {

    Memory memoryBuffer = new Memory();
    byte[] data = new byte[1024];
    Arrays.fill(data, (byte) 1);

    memoryBuffer.write(0, data, data.length, false);

    assertTrue(1 == memoryBuffer.getChunks().size());

    byte[] chunk = memoryBuffer.getChunks().get(0);
    assertTrue(chunk[0] == 1);
    assertTrue(chunk[1] == 1);

    assertTrue(chunk[1022] == 1);
    assertTrue(chunk[1023] == 1);

    assertTrue(memoryBuffer.size() == 1024);
  }

  @Test
  public void memorySave_5() {

    Memory memoryBuffer = new Memory();

    byte[] data = new byte[1025];
    Arrays.fill(data, (byte) 1);

    memoryBuffer.write(0, data, data.length, false);

    assertTrue(2 == memoryBuffer.getChunks().size());

    byte[] chunk1 = memoryBuffer.getChunks().get(0);
    assertTrue(chunk1[0] == 1);
    assertTrue(chunk1[1] == 1);

    assertTrue(chunk1[1022] == 1);
    assertTrue(chunk1[1023] == 1);

    byte[] chunk2 = memoryBuffer.getChunks().get(1);
    assertTrue(chunk2[0] == 1);
    assertTrue(chunk2[1] == 0);

    assertTrue(memoryBuffer.size() == 1056);
  }

  @Test
  public void memorySave_6() {

    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[1024];
    Arrays.fill(data1, (byte) 1);

    byte[] data2 = new byte[1024];
    Arrays.fill(data2, (byte) 2);

    memoryBuffer.write(0, data1, data1.length, false);
    memoryBuffer.write(1024, data2, data2.length, false);

    assertTrue(2 == memoryBuffer.getChunks().size());

    byte[] chunk1 = memoryBuffer.getChunks().get(0);
    assertTrue(chunk1[0] == 1);
    assertTrue(chunk1[1] == 1);

    assertTrue(chunk1[1022] == 1);
    assertTrue(chunk1[1023] == 1);

    byte[] chunk2 = memoryBuffer.getChunks().get(1);
    assertTrue(chunk2[0] == 2);
    assertTrue(chunk2[1] == 2);

    assertTrue(chunk2[1022] == 2);
    assertTrue(chunk2[1023] == 2);

    assertTrue(memoryBuffer.size() == 2048);
  }

  @Test
  public void memorySave_7() {

    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[1024];
    Arrays.fill(data1, (byte) 1);

    byte[] data2 = new byte[1024];
    Arrays.fill(data2, (byte) 2);

    byte[] data3 = new byte[1];
    Arrays.fill(data3, (byte) 3);

    memoryBuffer.write(0, data1, data1.length, false);
    memoryBuffer.write(1024, data2, data2.length, false);
    memoryBuffer.write(2048, data3, data3.length, false);

    assertTrue(3 == memoryBuffer.getChunks().size());

    byte[] chunk1 = memoryBuffer.getChunks().get(0);
    assertTrue(chunk1[0] == 1);
    assertTrue(chunk1[1] == 1);

    assertTrue(chunk1[1022] == 1);
    assertTrue(chunk1[1023] == 1);

    byte[] chunk2 = memoryBuffer.getChunks().get(1);
    assertTrue(chunk2[0] == 2);
    assertTrue(chunk2[1] == 2);

    assertTrue(chunk2[1022] == 2);
    assertTrue(chunk2[1023] == 2);

    byte[] chunk3 = memoryBuffer.getChunks().get(2);
    assertTrue(chunk3[0] == 3);

    assertTrue(memoryBuffer.size() == 2080);
  }

  @Test
  public void memorySave_8() {

    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[128];
    Arrays.fill(data1, (byte) 1);

    memoryBuffer.extendAndWrite(0, 256, data1);

    int ones = 0;
    int zeroes = 0;
    for (int i = 0; i < memoryBuffer.size(); ++i) {
      if (memoryBuffer.readByte(i) == 1) {
        ++ones;
      }
      if (memoryBuffer.readByte(i) == 0) {
        ++zeroes;
      }
    }

    assertTrue(ones == zeroes);
    assertTrue(256 == memoryBuffer.size());
  }


  @Test
  public void memoryLoad_1() {

    Memory memoryBuffer = new Memory();
    DataWord value = memoryBuffer.readWord(100);
    assertTrue(value.intValue() == 0);
    assertTrue(memoryBuffer.getChunks().size() == 1);
    assertTrue(memoryBuffer.size() == 32 * 5);
  }

  @Test
  public void memoryLoad_2() {

    Memory memoryBuffer = new Memory();
    DataWord value = memoryBuffer.readWord(2015);
    assertTrue(value.intValue() == 0);
    assertTrue(memoryBuffer.getChunks().size() == 2);
    assertTrue(memoryBuffer.size() == 2048);
  }

  @Test
  public void memoryLoad_3() {

    Memory memoryBuffer = new Memory();
    DataWord value = memoryBuffer.readWord(2016);
    assertTrue(value.intValue() == 0);
    assertTrue(memoryBuffer.getChunks().size() == 2);
    assertTrue(memoryBuffer.size() == 2048);
  }

  @Test
  public void memoryLoad_4() {

    Memory memoryBuffer = new Memory();
    DataWord value = memoryBuffer.readWord(2017);
    assertTrue(value.intValue() == 0);
    assertTrue(memoryBuffer.getChunks().size() == 3);
    assertTrue(memoryBuffer.size() == 2080);
  }

  @Test
  public void memoryLoad_5() {

    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[1024];
    Arrays.fill(data1, (byte) 1);

    byte[] data2 = new byte[1024];
    Arrays.fill(data2, (byte) 2);

    memoryBuffer.write(0, data1, data1.length, false);
    memoryBuffer.write(1024, data2, data2.length, false);

    assertTrue(memoryBuffer.getChunks().size() == 2);
    assertTrue(memoryBuffer.size() == 2048);

    DataWord val1 = memoryBuffer.readWord(0x3df);
    DataWord val2 = memoryBuffer.readWord(0x3e0);
    DataWord val3 = memoryBuffer.readWord(0x3e1);

    assertArrayEquals(
        Hex.decode("0101010101010101010101010101010101010101010101010101010101010101"),
        val1.getData());

    assertArrayEquals(
        Hex.decode("0101010101010101010101010101010101010101010101010101010101010101"),
        val2.getData());

    assertArrayEquals(
        Hex.decode("0101010101010101010101010101010101010101010101010101010101010102"),
        val3.getData());
    assertTrue(memoryBuffer.size() == 2048);
  }


  @Test
  public void memoryChunk_1() {
    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[32];
    Arrays.fill(data1, (byte) 1);

    byte[] data2 = new byte[32];
    Arrays.fill(data2, (byte) 2);

    memoryBuffer.write(0, data1, data1.length, false);
    memoryBuffer.write(32, data2, data2.length, false);

    byte[] data = memoryBuffer.read(0, 64);

    assertArrayEquals(
        Hex.decode("0101010101010101010101010101010101010101010101010101010101010101"
            + "0202020202020202020202020202020202020202020202020202020202020202"),
        data
    );

    Assert.assertEquals(64, memoryBuffer.size());
  }


  @Test
  public void memoryChunk_2() {
    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[32];
    Arrays.fill(data1, (byte) 1);

    memoryBuffer.write(0, data1, data1.length, false);
    assertTrue(32 == memoryBuffer.size());

    byte[] data = memoryBuffer.read(0, 64);

    assertArrayEquals(
        Hex.decode("0101010101010101010101010101010101010101010101010101010101010101"
            + "0000000000000000000000000000000000000000000000000000000000000000"),
        data
    );

    Assert.assertEquals(64, memoryBuffer.size());
  }

  @Test
  public void memoryChunk_3() {

    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[1024];
    Arrays.fill(data1, (byte) 1);

    byte[] data2 = new byte[1024];
    Arrays.fill(data2, (byte) 2);

    memoryBuffer.write(0, data1, data1.length, false);
    memoryBuffer.write(1024, data2, data2.length, false);

    byte[] data = memoryBuffer.read(0, 2048);

    int ones = 0;
    int twos = 0;
    for (int i = 0; i < data.length; ++i) {
      if (data[i] == 1) {
        ++ones;
      }
      if (data[i] == 2) {
        ++twos;
      }
    }

    assertTrue(ones == twos);
    assertTrue(2048 == memoryBuffer.size());
  }

  @Test
  public void memoryChunk_4() {

    Memory memoryBuffer = new Memory();

    byte[] data1 = new byte[1024];
    Arrays.fill(data1, (byte) 1);

    byte[] data2 = new byte[1024];
    Arrays.fill(data2, (byte) 2);

    memoryBuffer.write(0, data1, data1.length, false);
    memoryBuffer.write(1024, data2, data2.length, false);

    byte[] data = memoryBuffer.read(0, 2049);

    int ones = 0;
    int twos = 0;
    int zero = 0;
    for (int i = 0; i < data.length; ++i) {
      if (data[i] == 1) {
        ++ones;
      }
      if (data[i] == 2) {
        ++twos;
      }
      if (data[i] == 0) {
        ++zero;
      }
    }

    assertTrue(zero == 1);
    assertTrue(ones == twos);
    assertTrue(2080 == memoryBuffer.size());
  }


  @Test
  public void memoryWriteLimited_1() {

    Memory memoryBuffer = new Memory();
    memoryBuffer.extend(0, 3072);

    byte[] data1 = new byte[6272];
    Arrays.fill(data1, (byte) 1);

    memoryBuffer.write(2720, data1, data1.length, true);

    byte lastZero = memoryBuffer.readByte(2719);
    byte firstOne = memoryBuffer.readByte(2721);

    assertTrue(memoryBuffer.size() == 3072);
    assertTrue(lastZero == 0);
    assertTrue(firstOne == 1);

    byte[] data = memoryBuffer.read(2720, 352);

    int ones = 0;
    int zero = 0;
    for (int i = 0; i < data.length; ++i) {
      if (data[i] == 1) {
        ++ones;
      }
      if (data[i] == 0) {
        ++zero;
      }
    }

    assertTrue(ones == data.length);
    assertTrue(zero == 0);
  }

  @Test
  public void memoryWriteLimited_2() {

    Memory memoryBuffer = new Memory();
    memoryBuffer.extend(0, 3072);

    byte[] data1 = new byte[6272];
    Arrays.fill(data1, (byte) 1);

    memoryBuffer.write(2720, data1, 300, true);

    byte lastZero = memoryBuffer.readByte(2719);
    byte firstOne = memoryBuffer.readByte(2721);

    assertTrue(memoryBuffer.size() == 3072);
    assertTrue(lastZero == 0);
    assertTrue(firstOne == 1);

    byte[] data = memoryBuffer.read(2720, 352);

    int ones = 0;
    int zero = 0;
    for (int i = 0; i < data.length; ++i) {
      if (data[i] == 1) {
        ++ones;
      }
      if (data[i] == 0) {
        ++zero;
      }
    }

    assertTrue(ones == 300);
    assertTrue(zero == 52);
  }

  @Test
  public void memoryWriteLimited_3() {

    Memory memoryBuffer = new Memory();
    memoryBuffer.extend(0, 128);

    byte[] data1 = new byte[20];
    Arrays.fill(data1, (byte) 1);

    memoryBuffer.write(10, data1, 40, true);

    byte lastZero = memoryBuffer.readByte(9);
    byte firstOne = memoryBuffer.readByte(10);

    assertTrue(memoryBuffer.size() == 128);
    assertTrue(lastZero == 0);
    assertTrue(firstOne == 1);

    byte[] data = memoryBuffer.read(10, 30);

    int ones = 0;
    int zero = 0;
    for (int i = 0; i < data.length; ++i) {
      if (data[i] == 1) {
        ++ones;
      }
      if (data[i] == 0) {
        ++zero;
      }
    }

    assertTrue(ones == 20);
    assertTrue(zero == 10);
  }


}