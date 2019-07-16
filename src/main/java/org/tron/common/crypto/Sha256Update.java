package org.tron.common.crypto;

/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.ByteArray;


/**
 * A Sha256Hash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map. It also checks that the length is correct and provides a bit more type
 * safety.
 */
public class Sha256Update {

  private static int ROTR32(int x, int s) {
    return (((x) >>> (s)) | ((x) << (32 - (s))));
  }

  private static int R(int x, int s) {
    return ((x) >>> (s));
  }

  private static int S(int x, int s) {
    return ROTR32(x, s);
  }

  private static int SIG0(int x) {
    return S(x, 2) ^ S(x, 13) ^ S(x, 22);
  }

  private static int SIG1(int x) {
    return S(x, 6) ^ S(x, 11) ^ S(x, 25);
  }

  private static int sig0(int x) {
    return S(x, 7) ^ S(x, 18) ^ R(x, 3);
  }

  private static int sig1(int x) {
    return S(x, 17) ^ S(x, 19) ^ R(x, 10);
  }

  private static int CH(int x, int y, int z) {
    return ((x & (y ^ z)) ^ z);
  }

  private static int MAJ(int x, int y, int z) {
    return (((x | y) & z) | (x & y));
  }

  private static int byte2Int(byte[] a, int offset) {
    int b = 0;
    b += ((int) (a[offset++]) << 24)&0xFF000000;
    b += ((int) (a[offset++]) << 16)&0x00FF0000;
    b += ((int) (a[offset++]) << 8)&0x0000FF00;
    b += ((int) (a[offset++]))&0x000000FF;
    return b;
  }

  private static int byte2Int_(byte[] a, int offset) {
    int b = 0;
    b += ((int) (a[offset++]))&0x000000FF;
    b += ((int) (a[offset++]) << 8)&0x0000FF00;
    b += ((int) (a[offset++]) << 16)&0x00FF0000;
    b += ((int) (a[offset++]) << 24)&0xFF000000;
    return b;
  }

  private static void int2Byte(byte[] a, int offset, int b) {
    a[offset++] = (byte) ((b & 0xFF000000) >>> 24);
    a[offset++] = (byte) ((b & 0x00FF0000) >>> 16);
    a[offset++] = (byte) ((b & 0x0000FF00) >>> 8);
    a[offset++] = (byte) ((b & 0x000000FF));
  }

  public static byte[] Sha256OneBlock(byte[] message) {
    if (ArrayUtils.isEmpty(message) || message.length != 64) {
      return null;
    }
    byte[] Iv = ByteArray
        .fromHexString("6A09E667BB67AE853C6EF372A54FF53A510E527F9B05688C1F83D9AB5BE0CD19");
    int[] k = new int[]
        {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4,
            0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7,
            0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc,
            0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351,
            0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e,
            0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585,
            0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f,
            0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7,
            0xc67178f2
        };
    byte[][] gi = new byte[][]
        {
            new byte[]{0, 1, 2, 3, 4, 5, 6, 7},
            new byte[]{7, 0, 1, 2, 3, 4, 5, 6},
            new byte[]{6, 7, 0, 1, 2, 3, 4, 5},
            new byte[]{5, 6, 7, 0, 1, 2, 3, 4},
            new byte[]{4, 5, 6, 7, 0, 1, 2, 3},
            new byte[]{3, 4, 5, 6, 7, 0, 1, 2},
            new byte[]{2, 3, 4, 5, 6, 7, 0, 1},
            new byte[]{1, 2, 3, 4, 5, 6, 7, 0}
        };

    int u1Index;
    int u1Idx;
    int[] u4Data = new int[64];
    int[] u4H = new int[8];
    int A;
    int B;
    int C;
    int D;
    int E;
    int F;
    int G;
    int H;
    int temp;

    for (u1Index = 0; u1Index < 32; u1Index += 0x04) {
      u4H[u1Index / 4] = byte2Int(Iv, u1Index);
      u4Data[u1Index / 4] = byte2Int(message, u1Index);
    }
    for (; u1Index < 64; u1Index += 0x04) {
      u4Data[u1Index / 4] = byte2Int(message, u1Index);
    }
    u1Index = 48;
    int offset = 16;
    while (u1Index-- > 0) {
      temp =
          sig1(u4Data[offset - 2]) + u4Data[offset - 7] + sig0(u4Data[offset - 15]) + u4Data[offset
              - 16];
      u4Data[offset++] = temp;
    }

    A = u4H[0];
    B = u4H[1];
    C = u4H[2];
    D = u4H[3];
    E = u4H[4];
    F = u4H[5];
    G = u4H[6];
    H = u4H[7];

    for (u1Index = 0; u1Index < 64; u1Index++) {
      u1Idx = (u1Index & 0x07);
      temp =
          u4H[gi[u1Idx][7]] + SIG1(u4H[gi[u1Idx][4]]) + CH(u4H[gi[u1Idx][4]], u4H[gi[u1Idx][5]],
              u4H[gi[u1Idx][6]]) + k[u1Index] + u4Data[u1Index];
      u4H[gi[u1Idx][7]] = temp + SIG0(u4H[gi[u1Idx][0]]) + MAJ(u4H[gi[u1Idx][0]], u4H[gi[u1Idx][1]],
          u4H[gi[u1Idx][2]]);
      u4H[gi[u1Idx][3]] += temp;
    }

    u4H[0] += A;
    u4H[1] += B;
    u4H[2] += C;
    u4H[3] += D;
    u4H[4] += E;
    u4H[5] += F;
    u4H[6] += G;
    u4H[7] += H;

    for (u1Index = 0; u1Index < 32; u1Index += 0x04) {
      int2Byte(Iv, u1Index, u4H[u1Index / 4]);
    }

    return Iv;
  }

  public static void main(String[] args){
    byte[] msg = ByteArray.fromHexString("6D38B7C9D29C104292D92219BDB70139AA86585B70B728FBADB2F5DE9CB4C14DFC338BAEDE9DA5B2D2EE9DA485F3151A57A935A1EDA8239A4EF020DE8518BC5E");
    byte[] hash = Sha256OneBlock(msg);
    System.out.println(ByteArray.toHexString(hash));
  }
}
