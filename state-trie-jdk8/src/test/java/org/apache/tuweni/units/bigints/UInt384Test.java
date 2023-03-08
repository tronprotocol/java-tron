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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UInt384Test {

  private static UInt384 v(long v) {
    return UInt384.valueOf(v);
  }

  private static UInt384 biv(String s) {
    return UInt384.valueOf(new BigInteger(s));
  }

  private static UInt384 hv(String s) {
    return UInt384.fromHexString(s);
  }

  @Test
  void valueOfLong() {
    assertThrows(IllegalArgumentException.class, () -> UInt384.valueOf(-1));
    assertThrows(IllegalArgumentException.class, () -> UInt384.valueOf(Long.MIN_VALUE));
    assertThrows(IllegalArgumentException.class, () -> UInt384.valueOf(~0L));
  }

  @Test
  void valueOfBigInteger() {
    assertThrows(IllegalArgumentException.class, () -> UInt384.valueOf(BigInteger.valueOf(-1)));
    assertThrows(IllegalArgumentException.class, () -> UInt384.valueOf(BigInteger.valueOf(2).pow(384)));
  }

  @ParameterizedTest
  @MethodSource("addProvider")
  void add(UInt384 v1, UInt384 v2, UInt384 expected) {
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
            Arguments.of(biv("9223372036854775807"), v(1), biv("9223372036854775808")),
            Arguments.of(biv("13492324908428420834234908342"), v(10), biv("13492324908428420834234908352")),
            Arguments
                .of(biv("13492324908428420834234908342"), v(23422141424214L), biv("13492324908428444256376332556")),
            Arguments.of(UInt384.MAX_VALUE, v(1), v(0)),
            Arguments.of(UInt384.MAX_VALUE, v(2), v(1)),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0"),
                    v(1),
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF1")),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"),
                    v(1),
                    UInt384.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addLongProvider")
  void addLong(UInt384 v1, long v2, UInt384 expected) {
    assertValueEquals(expected, v1.add(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 0L, v(1)),
            Arguments.of(v(5), 0L, v(5)),
            Arguments.of(v(0), 1L, v(1)),
            Arguments.of(v(0), 100L, v(100)),
            Arguments.of(v(2), 2L, v(4)),
            Arguments.of(v(100), 90L, v(190)),
            Arguments.of(biv("13492324908428420834234908342"), 10L, biv("13492324908428420834234908352")),
            Arguments.of(biv("13492324908428420834234908342"), 23422141424214L, biv("13492324908428444256376332556")),
            Arguments.of(UInt384.MAX_VALUE, 1L, v(0)),
            Arguments.of(UInt384.MAX_VALUE, 2L, v(1)),
            Arguments
                .of(
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0"),
                    1L,
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF1")),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"),
                    1L,
                    UInt384.MAX_VALUE),
            Arguments.of(v(10), -5L, v(5)),
            Arguments.of(v(0), -1L, UInt384.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addModProvider")
  void addMod(UInt384 v1, UInt384 v2, UInt384 m, UInt384 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(1), UInt384.valueOf(2), v(1)),
            Arguments.of(v(1), v(1), UInt384.valueOf(2), v(0)),
            Arguments.of(UInt384.MAX_VALUE.subtract(2), v(1), UInt384.MAX_VALUE, UInt384.MAX_VALUE.subtract(1)),
            Arguments.of(UInt384.MAX_VALUE.subtract(1), v(1), UInt384.MAX_VALUE, v(0)),
            Arguments.of(v(2), v(1), UInt384.valueOf(2), v(1)),
            Arguments.of(v(3), v(2), UInt384.valueOf(6), v(5)),
            Arguments.of(v(3), v(4), UInt384.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModOfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(v(1), UInt384.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModUInt384UInt384Provider")
  void addModUInt384UInt384(UInt384 v1, UInt384 v2, UInt384 m, UInt384 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModUInt384UInt384Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt384.ONE, UInt384.valueOf(2), v(1)),
            Arguments.of(v(1), UInt384.ONE, UInt384.valueOf(2), v(0)),
            Arguments.of(UInt384.MAX_VALUE.subtract(2), UInt384.ONE, UInt384.MAX_VALUE, UInt384.MAX_VALUE.subtract(1)),
            Arguments.of(UInt384.MAX_VALUE.subtract(1), UInt384.ONE, UInt384.MAX_VALUE, v(0)),
            Arguments.of(v(2), UInt384.ONE, UInt384.valueOf(2), v(1)),
            Arguments.of(v(3), UInt384.valueOf(2), UInt384.valueOf(6), v(5)),
            Arguments.of(v(3), UInt384.valueOf(4), UInt384.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModLongUInt384OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, UInt384.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongUInt384Provider")
  void addModLongUInt384(UInt384 v1, long v2, UInt384 m, UInt384 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongUInt384Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 1L, UInt384.valueOf(2), v(1)),
            Arguments.of(v(1), 1L, UInt384.valueOf(2), v(0)),
            Arguments.of(UInt384.MAX_VALUE.subtract(2), 1L, UInt384.MAX_VALUE, UInt384.MAX_VALUE.subtract(1)),
            Arguments.of(UInt384.MAX_VALUE.subtract(1), 1L, UInt384.MAX_VALUE, v(0)),
            Arguments.of(v(2), 1L, UInt384.valueOf(2), v(1)),
            Arguments.of(v(2), -1L, UInt384.valueOf(2), v(1)),
            Arguments.of(v(1), -7L, UInt384.valueOf(5), v(4)));
  }

  @Test
  void shouldThrowForAddModUInt384UInt384OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(UInt384.ONE, UInt384.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongLongProvider")
  void addModLongLong(UInt384 v1, long v2, long m, UInt384 expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongLongProvider() {
    return Stream
        .of(Arguments.of(v(0), 1L, 2L, v(1)), Arguments.of(v(1), 1L, 2L, v(0)), Arguments.of(v(2), 1L, 2L, v(1)));
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
  void subtract(UInt384 v1, UInt384 v2, UInt384 expected) {
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
            Arguments.of(biv("13492324908428420834234908342"), v(10), biv("13492324908428420834234908332")),
            Arguments
                .of(biv("13492324908428420834234908342"), v(23422141424214L), biv("13492324908428397412093484128")),
            Arguments.of(v(0), v(1), UInt384.MAX_VALUE),
            Arguments.of(v(1), v(2), UInt384.MAX_VALUE),
            Arguments
                .of(
                    UInt384.MAX_VALUE,
                    v(1),
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("subtractLongProvider")
  void subtractLong(UInt384 v1, long v2, UInt384 expected) {
    assertValueEquals(expected, v1.subtract(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractLongProvider() {
    return Stream
        .of(
            Arguments.of(v(1), 0L, v(1)),
            Arguments.of(v(5), 0L, v(5)),
            Arguments.of(v(2), 1L, v(1)),
            Arguments.of(v(100), 100L, v(0)),
            Arguments.of(biv("13492324908428420834234908342"), 10L, biv("13492324908428420834234908332")),
            Arguments.of(biv("13492324908428420834234908342"), 23422141424214L, biv("13492324908428397412093484128")),
            Arguments.of(v(0), 1L, UInt384.MAX_VALUE),
            Arguments.of(v(1), 2L, UInt384.MAX_VALUE),
            Arguments
                .of(
                    UInt384.MAX_VALUE,
                    1L,
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE")),
            Arguments.of(v(0), -1L, v(1)),
            Arguments.of(v(0), -100L, v(100)),
            Arguments.of(v(2), -2L, v(4)));
  }

  @ParameterizedTest
  @MethodSource("multiplyProvider")
  void multiply(UInt384 v1, UInt384 v2, UInt384 expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(2), v(0)),
            Arguments.of(v(1), v(2), v(2)),
            Arguments.of(v(2), v(2), v(4)),
            Arguments.of(v(3), v(2), v(6)),
            Arguments.of(v(4), v(2), v(8)),
            Arguments.of(v(10), v(18), v(180)),
            Arguments.of(biv("13492324908428420834234908341"), v(2), biv("26984649816856841668469816682")),
            Arguments.of(biv("13492324908428420834234908342"), v(2), biv("26984649816856841668469816684")),
            Arguments.of(v(2), v(8), v(16)),
            Arguments.of(v(7), v(8), v(56)),
            Arguments.of(v(8), v(8), v(64)),
            Arguments.of(v(17), v(8), v(136)),
            Arguments.of(biv("13492324908428420834234908342"), v(8), biv("107938599267427366673879266736")),
            Arguments.of(biv("13492324908428420834234908342"), v(2048), biv("27632281412461405868513092284416")),
            Arguments.of(biv("13492324908428420834234908342"), v(131072), biv("1768466010397529975584837906202624")),
            Arguments.of(v(22), v(0), v(0)));
  }

  @ParameterizedTest
  @MethodSource("multiplyLongProvider")
  void multiplyLong(UInt384 v1, long v2, UInt384 expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(1), 2L, v(2)),
            Arguments.of(v(2), 2L, v(4)),
            Arguments.of(v(3), 2L, v(6)),
            Arguments.of(v(4), 2L, v(8)),
            Arguments.of(v(10), 18L, v(180)),
            Arguments.of(biv("13492324908428420834234908341"), 2L, biv("26984649816856841668469816682")),
            Arguments.of(biv("13492324908428420834234908342"), 2L, biv("26984649816856841668469816684")),
            Arguments.of(v(2), 8L, v(16)),
            Arguments.of(v(7), 8L, v(56)),
            Arguments.of(v(8), 8L, v(64)),
            Arguments.of(v(17), 8L, v(136)),
            Arguments.of(biv("13492324908428420834234908342"), 8L, biv("107938599267427366673879266736")),
            Arguments.of(biv("13492324908428420834234908342"), 2048L, biv("27632281412461405868513092284416")),
            Arguments.of(biv("13492324908428420834234908342"), 131072L, biv("1768466010397529975584837906202624")),
            Arguments.of(v(22), 0L, v(0)),
            Arguments
                .of(
                    hv(
                        "0x0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    2L,
                    hv(
                        "0x1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE")),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    2L,
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE")));
  }

  @Test
  void shouldThrowForMultiplyLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(2).multiply(-5));
    assertEquals("multiply unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModProvider")
  void multiplyMod(UInt384 v1, UInt384 v2, UInt384 m, UInt384 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(5), UInt384.valueOf(2), v(0)),
            Arguments.of(v(2), v(3), UInt384.valueOf(7), v(6)),
            Arguments.of(v(2), v(3), UInt384.valueOf(6), v(0)),
            Arguments.of(v(2), v(0), UInt384.valueOf(6), v(0)),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"),
                    v(2),
                    UInt384.MAX_VALUE,
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModOfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).multiplyMod(v(1), UInt384.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongUInt384Provider")
  void multiplyModLongUInt384(UInt384 v1, long v2, UInt384 m, UInt384 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModLongUInt384Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 5L, UInt384.valueOf(2), v(0)),
            Arguments.of(v(2), 3L, UInt384.valueOf(7), v(6)),
            Arguments.of(v(2), 3L, UInt384.valueOf(6), v(0)),
            Arguments.of(v(2), 0L, UInt384.valueOf(6), v(0)),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"),
                    2L,
                    UInt384.MAX_VALUE,
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModLongUInt384OfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).multiplyMod(1L, UInt384.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongUInt384OfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(3).multiplyMod(-1, UInt384.valueOf(2)));
    assertEquals("multiplyMod unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongLongProvider")
  void multiplyModLongLong(UInt384 v1, long v2, long m, UInt384 expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModLongLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 5L, 2L, v(0)),
            Arguments.of(v(2), 3L, 7L, v(6)),
            Arguments.of(v(2), 3L, 6L, v(0)),
            Arguments.of(v(2), 0L, 6L, v(0)),
            Arguments
                .of(
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"),
                    2L,
                    Long.MAX_VALUE,
                    hv("0x000000000000000000000000000000000000000000000000000000000000001C")));
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
  void divide(UInt384 v1, UInt384 v2, UInt384 expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(2), v(0)),
            Arguments.of(v(1), v(2), v(0)),
            Arguments.of(v(2), v(2), v(1)),
            Arguments.of(v(3), v(2), v(1)),
            Arguments.of(v(4), v(2), v(2)),
            Arguments.of(biv("13492324908428420834234908341"), v(2), biv("6746162454214210417117454170")),
            Arguments.of(biv("13492324908428420834234908342"), v(2), biv("6746162454214210417117454171")),
            Arguments.of(biv("13492324908428420834234908343"), v(2), biv("6746162454214210417117454171")),
            Arguments.of(v(2), v(8), v(0)),
            Arguments.of(v(7), v(8), v(0)),
            Arguments.of(v(8), v(8), v(1)),
            Arguments.of(v(9), v(8), v(1)),
            Arguments.of(v(17), v(8), v(2)),
            Arguments.of(v(1024), v(8), v(128)),
            Arguments.of(v(1026), v(8), v(128)),
            Arguments.of(biv("13492324908428420834234908342"), v(8), biv("1686540613553552604279363542")),
            Arguments.of(biv("13492324908428420834234908342"), v(2048), biv("6588049271693564860466263")),
            Arguments.of(biv("13492324908428420834234908342"), v(131072), biv("102938269870211950944785")));
  }

  @Test
  void shouldThrowForDivideByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(v(0)));
    assertEquals("divide by zero", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("divideLongProvider")
  void divideLong(UInt384 v1, long v2, UInt384 expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(1), 2L, v(0)),
            Arguments.of(v(2), 2L, v(1)),
            Arguments.of(v(3), 2L, v(1)),
            Arguments.of(v(4), 2L, v(2)),
            Arguments.of(biv("13492324908428420834234908341"), 2L, biv("6746162454214210417117454170")),
            Arguments.of(biv("13492324908428420834234908342"), 2L, biv("6746162454214210417117454171")),
            Arguments.of(biv("13492324908428420834234908343"), 2L, biv("6746162454214210417117454171")),
            Arguments.of(v(2), 8L, v(0)),
            Arguments.of(v(7), 8L, v(0)),
            Arguments.of(v(8), 8L, v(1)),
            Arguments.of(v(9), 8L, v(1)),
            Arguments.of(v(17), 8L, v(2)),
            Arguments.of(v(1024), 8L, v(128)),
            Arguments.of(v(1026), 8L, v(128)),
            Arguments.of(biv("13492324908428420834234908342"), 8L, biv("1686540613553552604279363542")),
            Arguments.of(biv("13492324908428420834234908342"), 2048L, biv("6588049271693564860466263")),
            Arguments.of(biv("13492324908428420834234908342"), 131072L, biv("102938269870211950944785")));
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
  @MethodSource("powUInt384Provider")
  void powUInt384(UInt384 v1, UInt384 v2, UInt384 expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powUInt384Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt384.valueOf(2), v(0)),
            Arguments.of(v(2), UInt384.valueOf(2), v(4)),
            Arguments.of(v(2), UInt384.valueOf(8), v(256)),
            Arguments.of(v(3), UInt384.valueOf(3), v(27)),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0F0F0"),
                    UInt384.valueOf(3),
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF2A920E119A2F000")));
  }

  @ParameterizedTest
  @MethodSource("powLongProvider")
  void powLong(UInt384 v1, long v2, UInt384 expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(2), 2L, v(4)),
            Arguments.of(v(2), 8L, v(256)),
            Arguments.of(v(3), 3L, v(27)),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0F0F0"),
                    3L,
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF2A920E119A2F000")),
            Arguments
                .of(
                    v(3),
                    -3L,
                    hv(
                        "0x4BDA12F684BDA12F684BDA12F684BDA12F684BDA12F684BDA12F684BDA12F684BDA12F684BDA12F684BDA12F684BDA13")));
  }

  @ParameterizedTest
  @MethodSource("modLongProvider")
  void modLong(UInt384 v1, long v2, UInt384 expected) {
    assertValueEquals(expected, v1.mod(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> modLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(1), 2L, v(1)),
            Arguments.of(v(2), 2L, v(0)),
            Arguments.of(v(3), 2L, v(1)),
            Arguments.of(biv("13492324908428420834234908342"), 2L, v(0)),
            Arguments.of(biv("13492324908428420834234908343"), 2L, v(1)),
            Arguments.of(v(0), 8L, v(0)),
            Arguments.of(v(1), 8L, v(1)),
            Arguments.of(v(2), 8L, v(2)),
            Arguments.of(v(3), 8L, v(3)),
            Arguments.of(v(7), 8L, v(7)),
            Arguments.of(v(8), 8L, v(0)),
            Arguments.of(v(9), 8L, v(1)),
            Arguments.of(v(1024), 8L, v(0)),
            Arguments.of(v(1026), 8L, v(2)),
            Arguments.of(biv("13492324908428420834234908342"), 8L, v(6)),
            Arguments.of(biv("13492324908428420834234908343"), 8L, v(7)),
            Arguments.of(biv("13492324908428420834234908344"), 8L, v(0)));
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
  void and(UInt384 v1, UInt384 v2, UInt384 expected) {
    assertValueEquals(expected, v1.and(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> andProvider() {
    return Stream
        .of(
            Arguments
                .of(
                    hv("0x00000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000"),
                    hv("0x0000000000000000000000000000000000000000000000000000000000000000")),
            Arguments
                .of(
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000"),
                    hv("0x000000000000000000000000000000FF00000000000000000000000000000000")));
  }

  @ParameterizedTest
  @MethodSource("orProvider")
  void or(UInt384 v1, UInt384 v2, UInt384 expected) {
    assertValueEquals(expected, v1.or(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> orProvider() {
    return Stream
        .of(
            Arguments
                .of(
                    hv("0x00000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")),
            Arguments
                .of(
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")),
            Arguments
                .of(
                    hv("0x0000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")));
  }

  @ParameterizedTest
  @MethodSource("xorProvider")
  void xor(UInt384 v1, UInt384 v2, UInt384 expected) {
    assertValueEquals(expected, v1.xor(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> xorProvider() {
    return Stream
        .of(
            Arguments
                .of(
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0x0000000000000000000000000000000000000000000000000000000000000000")),
            Arguments
                .of(
                    hv("0x00000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")),
            Arguments
                .of(
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")));
  }

  @ParameterizedTest
  @MethodSource("notProvider")
  void not(UInt384 value, UInt384 expected) {
    assertValueEquals(expected, value.not());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> notProvider() {
    return Stream
        .of(
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv(
                        "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")),
            Arguments
                .of(
                    hv(
                        "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")),
            Arguments
                .of(
                    hv(
                        "0x00000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000")));
  }

  @ParameterizedTest
  @MethodSource("shiftLeftProvider")
  void shiftLeft(UInt384 value, int distance, UInt384 expected) {
    assertValueEquals(expected, value.shiftLeft(distance));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> shiftLeftProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x01"), 1, hv("0x02")),
            Arguments.of(hv("0x01"), 2, hv("0x04")),
            Arguments.of(hv("0x01"), 8, hv("0x0100")),
            Arguments.of(hv("0x01"), 9, hv("0x0200")),
            Arguments.of(hv("0x01"), 16, hv("0x10000")),
            Arguments.of(hv("0x00FF00"), 4, hv("0x0FF000")),
            Arguments.of(hv("0x00FF00"), 8, hv("0xFF0000")),
            Arguments.of(hv("0x00FF00"), 1, hv("0x01FE00")),
            Arguments
                .of(
                    hv(
                        "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001"),
                    16,
                    hv(
                        "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000")),
            Arguments
                .of(
                    hv(
                        "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001"),
                    15,
                    hv(
                        "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008000")),
            Arguments
                .of(
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    55,
                    hv(
                        "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF80000000000000")),
            Arguments
                .of(
                    hv(
                        "0x00000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    330,
                    hv(
                        "0xFFFFFFFFFFFFFC0000000000000000000000000000000000000000000000000000000000000000000000000000000000")));
  }

  @ParameterizedTest
  @MethodSource("shiftRightProvider")
  void shiftRight(UInt384 value, int distance, UInt384 expected) {
    assertValueEquals(expected, value.shiftRight(distance));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> shiftRightProvider() {
    return Stream
        .of(
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
            Arguments
                .of(
                    hv("0x1000000000000000000000000000000000000000000000000000000000000000"),
                    16,
                    hv("0x0000100000000000000000000000000000000000000000000000000000000000")),
            Arguments
                .of(
                    hv("0x1000000000000000000000000000000000000000000000000000000000000000"),
                    15,
                    hv("0x0000200000000000000000000000000000000000000000000000000000000000")),
            Arguments
                .of(
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    55,
                    hv("0x00000000000001FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")),
            Arguments
                .of(
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000000000000"),
                    202,
                    hv("0x000000000000000000000000000000000000000000000000003FFFFFFFFFFFFF")));
  }

  @ParameterizedTest
  @MethodSource("intValueProvider")
  void intValue(UInt384 value, int expected) {
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

  @Test
  void shouldThrowForIntValueOfOversizeValue() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> hv("0x0100000000").intValue());
    assertEquals("Value does not fit a 4 byte int", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("longValueProvider")
  void longValue(UInt384 value, long expected) {
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
            Arguments.of(hv("0x00000001"), 1L),
            Arguments.of(hv("0x0000000001"), 1L),
            Arguments.of(hv("0x000000000001"), 1L),
            Arguments.of(hv("0x0100"), 256L),
            Arguments.of(hv("0x000100"), 256L),
            Arguments.of(hv("0x00000100"), 256L),
            Arguments.of(hv("0x00000100"), 256L),
            Arguments.of(hv("0x000000000100"), 256L),
            Arguments.of(hv("0x00000000000100"), 256L),
            Arguments.of(hv("0x0000000000000100"), 256L),
            Arguments.of(hv("0xFFFFFFFF"), (1L << 32) - 1));
  }

  @Test
  void shouldThrowForLongValueOfOversizeValue() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> hv("0x010000000000000000").toLong());
    assertEquals("Value does not fit a 8 byte long", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("compareToProvider")
  void compareTo(UInt384 v1, UInt384 v2, int expected) {
    assertEquals(expected, v1.compareTo(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> compareToProvider() {
    return Stream
        .of(
            Arguments.of(v(5), v(5), 0),
            Arguments.of(v(5), v(3), 1),
            Arguments.of(v(5), v(6), -1),
            Arguments
                .of(
                    hv("0x0000000000000000000000000000000000000000000000000000000000000000"),
                    hv("0x0000000000000000000000000000000000000000000000000000000000000000"),
                    0),
            Arguments
                .of(
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    0),
            Arguments
                .of(
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    0),
            Arguments
                .of(
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0x0000000000000000000000000000000000000000000000000000000000000000"),
                    1),
            Arguments
                .of(
                    hv("0x0000000000000000000000000000000000000000000000000000000000000000"),
                    hv("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    -1),
            Arguments
                .of(
                    hv("0x000000000000000000000000000001FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    1),
            Arguments
                .of(
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"),
                    hv("0x000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"),
                    -1));
  }

  @ParameterizedTest
  @MethodSource("toBytesProvider")
  void toBytesTest(UInt384 value, Bytes expected) {
    assertEquals(expected, value.toBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toBytesProvider() {
    return Stream
        .of(
            Arguments
                .of(
                    hv("0x00"),
                    Bytes
                        .fromHexString(
                            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")),
            Arguments
                .of(
                    hv("0x01000000"),
                    Bytes
                        .fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000")),
            Arguments
                .of(
                    hv("0x0100000000"),
                    Bytes
                        .fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000")),
            Arguments
                .of(
                    hv("0xf100000000ab"),
                    Bytes
                        .fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000000000000000000000000000f100000000ab")),
            Arguments
                .of(
                    hv("0x0400000000000000000000000000000000000000000000000000f100000000ab"),
                    Bytes
                        .fromHexString(
                            "0x000000000000000000000000000000000400000000000000000000000000000000000000000000000000f100000000ab")));
  }

  @ParameterizedTest
  @MethodSource("toMinimalBytesProvider")
  void toMinimalBytesTest(UInt384 value, Bytes expected) {
    assertEquals(expected, value.toMinimalBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toMinimalBytesProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), Bytes.EMPTY),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")),
            Arguments.of(hv("0x0100000000"), Bytes.fromHexString("0x0100000000")),
            Arguments.of(hv("0xf100000000ab"), Bytes.fromHexString("0xf100000000ab")),
            Arguments
                .of(
                    hv("0x0400000000000000000000000000000000000000000000000000f100000000ab"),
                    Bytes.fromHexString("0x0400000000000000000000000000000000000000000000000000f100000000ab")));
  }

  @ParameterizedTest
  @MethodSource("numberOfLeadingZerosProvider")
  void numberOfLeadingZeros(UInt384 value, int expected) {
    assertEquals(expected, value.numberOfLeadingZeros());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> numberOfLeadingZerosProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), 384),
            Arguments.of(hv("0x01"), 383),
            Arguments.of(hv("0x02"), 382),
            Arguments.of(hv("0x03"), 382),
            Arguments.of(hv("0x0F"), 380),
            Arguments.of(hv("0x8F"), 376),
            Arguments.of(hv("0x100000000"), 351));
  }

  @ParameterizedTest
  @MethodSource("bitLengthProvider")
  void bitLength(UInt384 value, int expected) {
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
            Arguments.of(hv("0x100000000"), 33));
  }

  @ParameterizedTest
  @MethodSource("addExactProvider")
  void addExact(UInt384 value, UInt384 operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactProvider() {
    return Stream.of(Arguments.of(UInt384.MAX_VALUE, v(1)), Arguments.of(UInt384.MAX_VALUE, UInt384.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addExactLongProvider")
  void addExactLong(UInt384 value, long operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactLongProvider() {
    return Stream
        .of(
            Arguments.of(UInt384.MAX_VALUE, 3),
            Arguments.of(UInt384.MAX_VALUE, Long.MAX_VALUE),
            Arguments.of(v(0), -1));
  }

  @ParameterizedTest
  @MethodSource("subtractExactProvider")
  void subtractExact(UInt384 value, UInt384 operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactProvider() {
    return Stream.of(Arguments.of(v(0), v(1)), Arguments.of(v(0), UInt384.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("subtractExactLongProvider")
  void subtractExactLong(UInt384 value, long operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactLongProvider() {
    return Stream.of(Arguments.of(v(0), 1), Arguments.of(v(0), Long.MAX_VALUE), Arguments.of(UInt384.MAX_VALUE, -1));
  }

  private void assertValueEquals(UInt384 expected, UInt384 actual) {
    String msg = String.format("Expected %s but got %s", expected.toHexString(), actual.toHexString());
    assertEquals(expected, actual, msg);
  }

  @Test
  void testToDecimalString() {
    assertEquals("3456", UInt384.valueOf(3456).toDecimalString());
  }
}