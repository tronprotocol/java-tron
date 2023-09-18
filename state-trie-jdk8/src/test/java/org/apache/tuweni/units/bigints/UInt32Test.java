/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.units.bigints;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UInt32Test {

  private static UInt32 v(int v) {
    return UInt32.valueOf(v);
  }

  private static UInt32 hv(String s) {
    return UInt32.fromHexString(s);
  }

  private static Bytes b(String s) {
    return Bytes.fromHexString(s);
  }

  @Test
  void valueOfInt() {
    assertThrows(IllegalArgumentException.class, () -> UInt32.valueOf(-1));
    assertThrows(IllegalArgumentException.class, () -> UInt32.valueOf(Integer.MIN_VALUE));
    assertThrows(IllegalArgumentException.class, () -> UInt32.valueOf(~0));
  }

  @Test
  void valueOfBigInteger() {
    assertThrows(IllegalArgumentException.class, () -> UInt32.valueOf(BigInteger.valueOf(-1)));
    assertThrows(IllegalArgumentException.class, () -> UInt32.valueOf(BigInteger.valueOf(2).pow(32)));
  }

  @ParameterizedTest
  @MethodSource("addProvider")
  void add(UInt32 v1, UInt32 v2, UInt32 expected) {
    assertValueEquals(expected, v1.add(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addProvider() {
    return Stream
        .of(
            Arguments.of(v(1), v(0), v(1)),
            Arguments.of(v(5), v(0), v(5)),
            Arguments.of(v(0), v(1), v(1)),
            Arguments.of(v(0), v(100), v(100)),
            Arguments.of(v(2), v(2), v(4)),
            Arguments.of(v(100), v(90), v(190)),
            Arguments.of(UInt32.MAX_VALUE, v(1), v(0)),
            Arguments.of(UInt32.MAX_VALUE, v(2), v(1)),
            Arguments.of(hv("0xFFFFFFF0"), v(1), hv("0xFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFE"), v(1), UInt32.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addLongProvider")
  void addLong(UInt32 v1, int v2, UInt32 expected) {
    assertValueEquals(expected, v1.add(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 0, v(1)),
            Arguments.of(v(5), 0, v(5)),
            Arguments.of(v(0), 1, v(1)),
            Arguments.of(v(0), 100, v(100)),
            Arguments.of(v(2), 2, v(4)),
            Arguments.of(v(100), 90, v(190)),
            Arguments.of(UInt32.MAX_VALUE, 1, v(0)),
            Arguments.of(UInt32.MAX_VALUE, 2, v(1)),
            Arguments.of(hv("0xFFFFFFF0"), 1, hv("0xFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFE"), 1, UInt32.MAX_VALUE),
            Arguments.of(v(10), -5, v(5)),
            Arguments.of(v(0), -1, UInt32.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addModProvider")
  void addMod(UInt32 v1, UInt32 v2, UInt32 m, UInt32 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(1), UInt32.valueOf(2), v(1)),
            Arguments.of(v(1), v(1), UInt32.valueOf(2), v(0)),
            Arguments.of(UInt32.MAX_VALUE.subtract(2), v(1), UInt32.MAX_VALUE, UInt32.MAX_VALUE.subtract(1)),
            Arguments.of(UInt32.MAX_VALUE.subtract(1), v(1), UInt32.MAX_VALUE, v(0)),
            Arguments.of(v(2), v(1), UInt32.valueOf(2), v(1)),
            Arguments.of(v(3), v(2), UInt32.valueOf(6), v(5)),
            Arguments.of(v(3), v(4), UInt32.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModOfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(v(1), UInt32.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModUInt32UInt32Provider")
  void addModUInt32UInt32(UInt32 v1, UInt32 v2, UInt32 m, UInt32 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModUInt32UInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.ONE, UInt32.valueOf(2), v(1)),
            Arguments.of(v(1), UInt32.ONE, UInt32.valueOf(2), v(0)),
            Arguments.of(UInt32.MAX_VALUE.subtract(2), UInt32.ONE, UInt32.MAX_VALUE, UInt32.MAX_VALUE.subtract(1)),
            Arguments.of(UInt32.MAX_VALUE.subtract(1), UInt32.ONE, UInt32.MAX_VALUE, v(0)),
            Arguments.of(v(2), UInt32.ONE, UInt32.valueOf(2), v(1)),
            Arguments.of(v(3), UInt32.valueOf(2), UInt32.valueOf(6), v(5)),
            Arguments.of(v(3), UInt32.valueOf(4), UInt32.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModLongUInt32OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, UInt32.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongUInt32Provider")
  void addModLongUInt32(UInt32 v1, long v2, UInt32 m, UInt32 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 1, UInt32.valueOf(2), v(1)),
            Arguments.of(v(1), 1, UInt32.valueOf(2), v(0)),
            Arguments.of(UInt32.MAX_VALUE.subtract(2), 1, UInt32.MAX_VALUE, UInt32.MAX_VALUE.subtract(1)),
            Arguments.of(UInt32.MAX_VALUE.subtract(1), 1, UInt32.MAX_VALUE, v(0)),
            Arguments.of(v(2), 1, UInt32.valueOf(2), v(1)),
            Arguments.of(v(2), -1, UInt32.valueOf(2), v(1)),
            Arguments.of(v(1), -7, UInt32.valueOf(5), v(4)));
  }

  @Test
  void shouldThrowForAddModUInt32UInt32OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(UInt32.ONE, UInt32.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongLongProvider")
  void addModLongLong(UInt32 v1, long v2, long m, UInt32 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongLongProvider() {
    return Stream.of(Arguments.of(v(0), 1, 2, v(1)), Arguments.of(v(1), 1, 2, v(0)), Arguments.of(v(2), 1, 2, v(1)));
  }

  @Test
  void shouldThrowForAddModLongLongOfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, 0));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForAddModLongLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, -5));
    assertEquals("addMod unsigned with negative modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("subtractProvider")
  void subtract(UInt32 v1, UInt32 v2, UInt32 expected) {
    assertValueEquals(expected, v1.subtract(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractProvider() {
    return Stream
        .of(
            Arguments.of(v(1), v(0), v(1)),
            Arguments.of(v(5), v(0), v(5)),
            Arguments.of(v(2), v(1), v(1)),
            Arguments.of(v(100), v(100), v(0)),
            Arguments.of(v(0), v(1), UInt32.MAX_VALUE),
            Arguments.of(v(1), v(2), UInt32.MAX_VALUE),
            Arguments.of(UInt32.MAX_VALUE, v(1), hv("0xFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("subtractLongProvider")
  void subtractLong(UInt32 v1, int v2, UInt32 expected) {
    assertValueEquals(expected, v1.subtract(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 0, v(1)),
            Arguments.of(v(5), 0, v(5)),
            Arguments.of(v(2), 1, v(1)),
            Arguments.of(v(100), 100, v(0)),
            Arguments.of(v(0), 1, UInt32.MAX_VALUE),
            Arguments.of(v(1), 2, UInt32.MAX_VALUE),
            Arguments.of(UInt32.MAX_VALUE, 1, hv("0xFFFFFFFE")),
            Arguments.of(v(0), -1, v(1)),
            Arguments.of(v(0), -100, v(100)),
            Arguments.of(v(2), -2, v(4)));
  }

  @ParameterizedTest
  @MethodSource("multiplyProvider")
  void multiply(UInt32 v1, UInt32 v2, UInt32 expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyProvider() {
    return Stream
        .of(
            Arguments.of(v(1), v(1), v(1)),
            Arguments.of(v(0), v(2), v(0)),
            Arguments.of(v(1), v(2), v(2)),
            Arguments.of(v(2), v(2), v(4)),
            Arguments.of(v(3), v(2), v(6)),
            Arguments.of(v(4), v(2), v(8)),
            Arguments.of(v(10), v(18), v(180)),
            Arguments.of(v(2), v(8), v(16)),
            Arguments.of(v(7), v(8), v(56)),
            Arguments.of(v(8), v(8), v(64)),
            Arguments.of(v(17), v(8), v(136)),
            Arguments.of(v(22), v(0), v(0)));
  }

  @ParameterizedTest
  @MethodSource("multiplyLongProvider")
  void multiplyLong(UInt32 v1, int v2, UInt32 expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 1, v(1)),
            Arguments.of(v(0), 2, v(0)),
            Arguments.of(v(1), 2, v(2)),
            Arguments.of(v(2), 2, v(4)),
            Arguments.of(v(3), 2, v(6)),
            Arguments.of(v(4), 2, v(8)),
            Arguments.of(v(10), 18, v(180)),
            Arguments.of(v(2), 8, v(16)),
            Arguments.of(v(7), 8, v(56)),
            Arguments.of(v(8), 8, v(64)),
            Arguments.of(v(17), 8, v(136)),
            Arguments.of(v(22), 0, v(0)),
            Arguments.of(hv("0x0FFFFFFF"), 2, hv("0x1FFFFFFE")),
            Arguments.of(hv("0xFFFFFFFF"), 2, hv("0xFFFFFFFE")));
  }

  @Test
  void shouldThrowForMultiplyLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(2).multiply(-5));
    assertEquals("multiply unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModProvider")
  void multiplyMod(UInt32 v1, UInt32 v2, UInt32 m, UInt32 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(5), UInt32.valueOf(2), v(0)),
            Arguments.of(v(2), v(3), UInt32.valueOf(7), v(6)),
            Arguments.of(v(2), v(3), UInt32.valueOf(6), v(0)),
            Arguments.of(v(2), v(0), UInt32.valueOf(6), v(0)),
            Arguments.of(hv("0x0FFFFFFE"), v(2), UInt32.MAX_VALUE, hv("0x1FFFFFFC")));
  }

  @Test
  void shouldThrowForMultiplyModOfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).multiplyMod(v(1), UInt32.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongUInt32Provider")
  void multiplyModLongUInt32(UInt32 v1, int v2, UInt32 m, UInt32 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModLongUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 5, UInt32.valueOf(2), v(0)),
            Arguments.of(v(2), 3, UInt32.valueOf(7), v(6)),
            Arguments.of(v(2), 3, UInt32.valueOf(6), v(0)),
            Arguments.of(v(2), 0, UInt32.valueOf(6), v(0)),
            Arguments.of(hv("0x0FFFFFFE"), 2, UInt32.MAX_VALUE, hv("0x1FFFFFFC")));
  }

  @Test
  void shouldThrowForMultiplyModLongUInt32OfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).multiplyMod(1, UInt32.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongUInt32OfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(3).multiplyMod(-1, UInt32.valueOf(2)));
    assertEquals("multiplyMod unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongLongProvider")
  void multiplyModLongLong(UInt32 v1, int v2, int m, UInt32 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModLongLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 5, 2, v(0)),
            Arguments.of(v(2), 3, 7, v(6)),
            Arguments.of(v(2), 3, 6, v(0)),
            Arguments.of(v(2), 0, 6, v(0)),
            Arguments.of(hv("0x0FFFFFFE"), 2, Integer.MAX_VALUE, hv("0x1FFFFFFC")));
  }

  @Test
  void shouldThrowForMultiplyModLongLongOfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).multiplyMod(1, 0));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongLongOfModNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(2).multiplyMod(5, -7));
    assertEquals("multiplyMod unsigned with negative modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(3).multiplyMod(-1, 2));
    assertEquals("multiplyMod unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("divideProvider")
  void divide(UInt32 v1, UInt32 v2, UInt32 expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideProvider() {
    return Stream
        .of(
            Arguments.of(v(1), v(1), v(1)),
            Arguments.of(v(0), v(2), v(0)),
            Arguments.of(v(1), v(2), v(0)),
            Arguments.of(v(2), v(2), v(1)),
            Arguments.of(v(3), v(2), v(1)),
            Arguments.of(v(4), v(2), v(2)),
            Arguments.of(v(2), v(8), v(0)),
            Arguments.of(v(7), v(8), v(0)),
            Arguments.of(v(8), v(8), v(1)),
            Arguments.of(v(9), v(8), v(1)),
            Arguments.of(v(17), v(8), v(2)),
            Arguments.of(v(1024), v(8), v(128)),
            Arguments.of(v(1026), v(8), v(128)));
  }

  @Test
  void shouldThrowForDivideByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(v(0)));
    assertEquals("divide by zero", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("divideLongProvider")
  void divideLong(UInt32 v1, int v2, UInt32 expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 1, v(1)),
            Arguments.of(v(0), 2, v(0)),
            Arguments.of(v(1), 2, v(0)),
            Arguments.of(v(2), 2, v(1)),
            Arguments.of(v(3), 2, v(1)),
            Arguments.of(v(4), 2, v(2)),
            Arguments.of(v(2), 8, v(0)),
            Arguments.of(v(7), 8, v(0)),
            Arguments.of(v(8), 8, v(1)),
            Arguments.of(v(9), 8, v(1)),
            Arguments.of(v(17), 8, v(2)),
            Arguments.of(v(1024), 8, v(128)),
            Arguments.of(v(1026), 8, v(128)));
  }

  @Test
  void shouldThrowForDivideLongByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(0));
    assertEquals("divide by zero", exception.getMessage());
  }

  @Test
  void shouldThrowForDivideLongByNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(-5));
    assertEquals("divide unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("powUInt32Provider")
  void powUInt32(UInt32 v1, UInt32 v2, UInt32 expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.valueOf(2), v(0)),
            Arguments.of(v(2), UInt32.valueOf(2), v(4)),
            Arguments.of(v(2), UInt32.valueOf(8), v(256)),
            Arguments.of(v(3), UInt32.valueOf(3), v(27)),
            Arguments.of(hv("0xFFF0F0F0"), UInt32.valueOf(3), hv("0x19A2F000")));
  }

  @ParameterizedTest
  @MethodSource("powLongProvider")
  void powLong(UInt32 v1, long v2, UInt32 expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2, v(0)),
            Arguments.of(v(2), 2, v(4)),
            Arguments.of(v(2), 8, v(256)),
            Arguments.of(v(3), 3, v(27)),
            Arguments.of(hv("0xFFF0F0F0"), 3, hv("0x19A2F000")));
  }

  @ParameterizedTest
  @MethodSource("modLongProvider")
  void modLong(UInt32 v1, int v2, UInt32 expected) {
    assertValueEquals(expected, v1.mod(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> modLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2, v(0)),
            Arguments.of(v(1), 2, v(1)),
            Arguments.of(v(2), 2, v(0)),
            Arguments.of(v(3), 2, v(1)),
            Arguments.of(v(0), 8, v(0)),
            Arguments.of(v(1), 8, v(1)),
            Arguments.of(v(2), 8, v(2)),
            Arguments.of(v(3), 8, v(3)),
            Arguments.of(v(7), 8, v(7)),
            Arguments.of(v(8), 8, v(0)),
            Arguments.of(v(9), 8, v(1)),
            Arguments.of(v(1024), 8, v(0)),
            Arguments.of(v(1026), 8, v(2)));
  }

  @Test
  void shouldThrowForModLongByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).mod(0));
    assertEquals("mod by zero", exception.getMessage());
  }

  @Test
  void shouldThrowForModLongByNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).mod(-5));
    assertEquals("mod by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("andProvider")
  void and(UInt32 v1, Object v2, UInt32 expected) {
    if (v2 instanceof UInt32) {
      assertValueEquals(expected, v1.and((UInt32) v2));
    } else if (v2 instanceof Bytes) {
      assertValueEquals(expected, v1.and((Bytes) v2));
    } else {
      throw new IllegalArgumentException();
    }
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> andProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x0000FFFF"), b("0xFFFF0000"), hv("0x00000000")),
            Arguments.of(hv("0x0000FFFF"), b("0xFFFFFF00"), hv("0x0000FF00")),
            Arguments.of(hv("0x0000FFFF"), hv("0xFFFF0000"), hv("0x00000000")),
            Arguments.of(hv("0x0000FFFF"), hv("0xFFFFFF00"), hv("0x0000FF00")));
  }

  @ParameterizedTest
  @MethodSource("orProvider")
  void or(UInt32 v1, Object v2, UInt32 expected) {
    if (v2 instanceof UInt32) {
      assertValueEquals(expected, v1.or((UInt32) v2));
    } else if (v2 instanceof Bytes) {
      assertValueEquals(expected, v1.or((Bytes) v2));
    } else {
      throw new IllegalArgumentException();
    }
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> orProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x0000FFFF"), b("0xFFFF0000"), hv("0xFFFFFFFF")),
            Arguments.of(hv("0x0000FFFF"), b("0xFFFF0000"), hv("0xFFFFFFFF")),
            Arguments.of(hv("0x000000FF"), b("0xFFFF0000"), hv("0xFFFF00FF")),
            Arguments.of(hv("0x0000FFFF"), hv("0xFFFF0000"), hv("0xFFFFFFFF")),
            Arguments.of(hv("0x0000FFFF"), hv("0xFFFF0000"), hv("0xFFFFFFFF")),
            Arguments.of(hv("0x000000FF"), hv("0xFFFF0000"), hv("0xFFFF00FF")));
  }

  @ParameterizedTest
  @MethodSource("xorProvider")
  void xor(UInt32 v1, Object v2, UInt32 expected) {
    if (v2 instanceof UInt32) {
      assertValueEquals(expected, v1.xor((UInt32) v2));
    } else if (v2 instanceof Bytes) {
      assertValueEquals(expected, v1.xor((Bytes) v2));
    } else {
      throw new IllegalArgumentException();
    }
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> xorProvider() {
    return Stream
        .of(
            Arguments.of(hv("0xFFFFFFFF"), b("0xFFFFFFFF"), hv("0x00000000")),
            Arguments.of(hv("0x0000FFFF"), b("0xFFFF0000"), hv("0xFFFFFFFF")),
            Arguments.of(hv("0x0000FFFF"), b("0xFFFFFF00"), hv("0xFFFF00FF")),
            Arguments.of(hv("0xFFFFFFFF"), hv("0xFFFFFFFF"), hv("0x00000000")),
            Arguments.of(hv("0x0000FFFF"), hv("0xFFFF0000"), hv("0xFFFFFFFF")),
            Arguments.of(hv("0x0000FFFF"), hv("0xFFFFFF00"), hv("0xFFFF00FF")));
  }

  @ParameterizedTest
  @MethodSource("notProvider")
  void not(UInt32 value, UInt32 expected) {
    assertValueEquals(expected, value.not());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> notProvider() {
    return Stream
        .of(
            Arguments.of(hv("0xFFFFFFFF"), hv("0x00000000")),
            Arguments.of(hv("0x00000000"), hv("0xFFFFFFFF")),
            Arguments.of(hv("0x0000FFFF"), hv("0xFFFF0000")));
  }

  @ParameterizedTest
  @MethodSource("shiftLeftProvider")
  void shiftLeft(UInt32 value, int distance, UInt32 expected) {
    assertValueEquals(expected, value.shiftLeft(distance));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> shiftLeftProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x01"), 0, hv("0x01")),
            Arguments.of(hv("0x01"), 1, hv("0x02")),
            Arguments.of(hv("0x01"), 2, hv("0x04")),
            Arguments.of(hv("0x01"), 8, hv("0x0100")),
            Arguments.of(hv("0x01"), 9, hv("0x0200")),
            Arguments.of(hv("0x01"), 16, hv("0x10000")),
            Arguments.of(hv("0x00FF00"), 4, hv("0x0FF000")),
            Arguments.of(hv("0x00FF00"), 8, hv("0xFF0000")),
            Arguments.of(hv("0x00FF00"), 1, hv("0x01FE00")),
            Arguments.of(hv("0x00000001"), 16, hv("0x00010000")),
            Arguments.of(hv("0x00000001"), 15, hv("0x00008000")),
            Arguments.of(hv("0xFFFFFFFF"), 23, hv("0xFF800000")),
            Arguments.of(hv("0x0000FFFF"), 18, hv("0xFFFC0000")));
  }

  @ParameterizedTest
  @MethodSource("shiftRightProvider")
  void shiftRight(UInt32 value, int distance, UInt32 expected) {
    assertValueEquals(expected, value.shiftRight(distance));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> shiftRightProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x01"), 0, hv("0x01")),
            Arguments.of(hv("0x01"), 1, hv("0x00")),
            Arguments.of(hv("0x10"), 1, hv("0x08")),
            Arguments.of(hv("0x10"), 2, hv("0x04")),
            Arguments.of(hv("0x10"), 8, hv("0x00")),
            Arguments.of(hv("0x1000"), 4, hv("0x0100")),
            Arguments.of(hv("0x1000"), 5, hv("0x0080")),
            Arguments.of(hv("0x1000"), 8, hv("0x0010")),
            Arguments.of(hv("0x1000"), 9, hv("0x0008")),
            Arguments.of(hv("0x1000"), 16, hv("0x0000")),
            Arguments.of(hv("0x00FF00"), 4, hv("0x000FF0")),
            Arguments.of(hv("0x00FF00"), 8, hv("0x0000FF")),
            Arguments.of(hv("0x00FF00"), 1, hv("0x007F80")),
            Arguments.of(hv("0x100000"), 16, hv("0x000010")),
            Arguments.of(hv("0x100000"), 15, hv("0x000020")),
            Arguments.of(hv("0xFFFFFFFF"), 23, hv("0x000001FF")),
            Arguments.of(hv("0xFFFFFFFF"), 202, hv("0x00000000")));
  }

  @ParameterizedTest
  @MethodSource("intValueProvider")
  void intValue(UInt32 value, int expected) {
    assertEquals(expected, value.intValue());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> intValueProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x"), 0),
            Arguments.of(hv("0x00"), 0),
            Arguments.of(hv("0x00000000"), 0),
            Arguments.of(hv("0x01"), 1),
            Arguments.of(hv("0x0001"), 1),
            Arguments.of(hv("0x000001"), 1),
            Arguments.of(hv("0x00000001"), 1),
            Arguments.of(hv("0x0100"), 256),
            Arguments.of(hv("0x000100"), 256),
            Arguments.of(hv("0x00000100"), 256));
  }

  @ParameterizedTest
  @MethodSource("longValueProvider")
  void longValue(UInt32 value, long expected) {
    assertEquals(expected, value.toLong());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> longValueProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x"), 0L),
            Arguments.of(hv("0x00"), 0L),
            Arguments.of(hv("0x00000000"), 0L),
            Arguments.of(hv("0x01"), 1L),
            Arguments.of(hv("0x0001"), 1L),
            Arguments.of(hv("0x000001"), 1L),
            Arguments.of(hv("0x0100"), 256L),
            Arguments.of(hv("0x000100"), 256L),
            Arguments.of(hv("0x00000100"), 256L),
            Arguments.of(hv("0xFFFFFFFF"), (1L << 32) - 1));
  }

  @ParameterizedTest
  @MethodSource("compareToProvider")
  void compareTo(UInt32 v1, UInt32 v2, int expected) {
    assertEquals(expected, v1.compareTo(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> compareToProvider() {
    return Stream
        .of(
            Arguments.of(v(5), v(5), 0),
            Arguments.of(v(5), v(3), 1),
            Arguments.of(v(5), v(6), -1),
            Arguments.of(hv("0x00000000"), hv("0x00000000"), 0),
            Arguments.of(hv("0xFFFFFFFF"), hv("0xFFFFFFFF"), 0),
            Arguments.of(hv("0x0000FFFF"), hv("0x0000FFFF"), 0),
            Arguments.of(hv("0xFFFFFFFF"), hv("0x00000000"), 1),
            Arguments.of(hv("0x00000000"), hv("0xFFFFFFFF"), -1),
            Arguments.of(hv("0x0001FFFF"), hv("0x0000FFFF"), 1),
            Arguments.of(hv("0x0000FFFE"), hv("0x0000FFFF"), -1));
  }

  @ParameterizedTest
  @MethodSource("toBytesProvider")
  void toBytesTest(UInt32 value, Bytes expected) {
    assertEquals(expected, value.toBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toBytesProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), Bytes.fromHexString("0x00000000")),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")),
            Arguments.of(hv("0xf10000ab"), Bytes.fromHexString("0xF10000AB")));
  }

  @ParameterizedTest
  @MethodSource("toMinimalBytesProvider")
  void toMinimalBytesTest(UInt32 value, Bytes expected) {
    assertEquals(expected, value.toMinimalBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toMinimalBytesProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), Bytes.EMPTY),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")),
            Arguments.of(hv("0xf10000ab"), Bytes.fromHexString("0xf10000ab")),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")));
  }

  @ParameterizedTest
  @MethodSource("numberOfLeadingZerosProvider")
  void numberOfLeadingZeros(UInt32 value, int expected) {
    assertEquals(expected, value.numberOfLeadingZeros());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> numberOfLeadingZerosProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), 32),
            Arguments.of(hv("0x01"), 31),
            Arguments.of(hv("0x02"), 30),
            Arguments.of(hv("0x03"), 30),
            Arguments.of(hv("0x0F"), 28),
            Arguments.of(hv("0x8F"), 24),
            Arguments.of(hv("0x1000000"), 7));
  }

  @ParameterizedTest
  @MethodSource("bitLengthProvider")
  void bitLength(UInt32 value, int expected) {
    assertEquals(expected, value.bitLength());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> bitLengthProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), 0),
            Arguments.of(hv("0x01"), 1),
            Arguments.of(hv("0x02"), 2),
            Arguments.of(hv("0x03"), 2),
            Arguments.of(hv("0x0F"), 4),
            Arguments.of(hv("0x8F"), 8),
            Arguments.of(hv("0x10000000"), 29));
  }

  @ParameterizedTest
  @MethodSource("addExactProvider")
  void addExact(UInt32 value, UInt32 operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactProvider() {
    return Stream.of(Arguments.of(UInt32.MAX_VALUE, v(1)), Arguments.of(UInt32.MAX_VALUE, UInt32.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addExactLongProvider")
  void addExactLong(UInt32 value, int operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactLongProvider() {
    return Stream
        .of(
            Arguments.of(UInt32.MAX_VALUE, 3),
            Arguments.of(UInt32.MAX_VALUE, Integer.MAX_VALUE),
            Arguments.of(v(0), -1));
  }

  @ParameterizedTest
  @MethodSource("subtractExactProvider")
  void subtractExact(UInt32 value, UInt32 operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactProvider() {
    return Stream.of(Arguments.of(v(0), v(1)), Arguments.of(v(0), UInt32.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("subtractExactLongProvider")
  void subtractExactLong(UInt32 value, int operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactLongProvider() {
    return Stream.of(Arguments.of(v(0), 1), Arguments.of(v(0), Integer.MAX_VALUE), Arguments.of(UInt32.MAX_VALUE, -1));
  }

  private void assertValueEquals(UInt32 expected, UInt32 actual) {
    String msg = String.format("Expected %s but got %s", expected.toHexString(), actual.toHexString());
    assertEquals(expected, actual, msg);
  }

  @Test
  void testToUInt32() {
    UInt32 value = UInt32.valueOf(42);
    assertSame(value, value.toUInt32());
  }

  @Test
  void toIntTooLarge() {
    assertThrows(ArithmeticException.class, UInt32.MAX_VALUE::intValue);
  }

  @Test
  void toLongTooLarge() {
    assertEquals(4294967295L, UInt32.MAX_VALUE.toLong());
  }

  @Test
  void testToDecimalString() {
    assertEquals("3456", UInt32.valueOf(3456).toDecimalString());
  }
}