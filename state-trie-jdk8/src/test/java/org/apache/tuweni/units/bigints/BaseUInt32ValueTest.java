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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This test is in a `test` sub-package to ensure that it does not have access to package-private
// methods within the bigints package, as it should be testing the usage of the public API.
class BaseUInt32ValueTest {

  private static class Value extends BaseUInt32Value<Value> {
    static final Value MAX_VALUE = new Value(UInt32.MAX_VALUE);

    Value(UInt32 v) {
      super(v, Value::new);
    }

    Value(int v) {
      super(v, Value::new);
    }
  }

  private static Value v(int v) {
    return new Value(v);
  }

  private static Value hv(String s) {
    return new Value(UInt32.fromHexString(s));
  }

  @ParameterizedTest
  @MethodSource("addProvider")
  void add(Value v1, Value v2, Value expected) {
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
            Arguments.of(Value.MAX_VALUE, v(1), v(0)),
            Arguments.of(Value.MAX_VALUE, v(2), v(1)),
            Arguments.of(hv("0xFFFFFFF0"), v(1), hv("0xFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFE"), v(1), Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addUInt32Provider")
  void addUInt32(Value v1, UInt32 v2, Value expected) {
    assertValueEquals(expected, v1.add(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(1), UInt32.ZERO, v(1)),
            Arguments.of(v(5), UInt32.ZERO, v(5)),
            Arguments.of(v(0), UInt32.ONE, v(1)),
            Arguments.of(v(0), UInt32.valueOf(100), v(100)),
            Arguments.of(v(2), UInt32.valueOf(2), v(4)),
            Arguments.of(v(100), UInt32.valueOf(90), v(190)),
            Arguments.of(Value.MAX_VALUE, UInt32.valueOf(1), v(0)),
            Arguments.of(Value.MAX_VALUE, UInt32.valueOf(2), v(1)),
            Arguments.of(hv("0xFFFFFFF0"), UInt32.valueOf(1), hv("0xFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFE"), UInt32.valueOf(1), Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addLongProvider")
  void addLong(Value v1, int v2, Value expected) {
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
            Arguments.of(Value.MAX_VALUE, 1, v(0)),
            Arguments.of(Value.MAX_VALUE, 2, v(1)),
            Arguments.of(hv("0xFFFFFFF0"), 1, hv("0xFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFE"), 1, Value.MAX_VALUE),
            Arguments.of(v(10), -5, v(5)),
            Arguments.of(v(0), -1, Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addModProvider")
  void addMod(Value v1, Value v2, UInt32 m, Value expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(1), UInt32.valueOf(2), v(1)),
            Arguments.of(v(1), v(1), UInt32.valueOf(2), v(0)),
            Arguments.of(Value.MAX_VALUE.subtract(2), v(1), UInt32.MAX_VALUE, Value.MAX_VALUE.subtract(1)),
            Arguments.of(Value.MAX_VALUE.subtract(1), v(1), UInt32.MAX_VALUE, v(0)),
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
  void addModUInt32UInt32(Value v1, UInt32 v2, UInt32 m, Value expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModUInt32UInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.ONE, UInt32.valueOf(2), v(1)),
            Arguments.of(v(1), UInt32.ONE, UInt32.valueOf(2), v(0)),
            Arguments.of(Value.MAX_VALUE.subtract(2), UInt32.ONE, UInt32.MAX_VALUE, Value.MAX_VALUE.subtract(1)),
            Arguments.of(Value.MAX_VALUE.subtract(1), UInt32.ONE, UInt32.MAX_VALUE, v(0)),
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
  void addModLongUInt32(Value v1, int v2, UInt32 m, Value expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 1, UInt32.valueOf(2), v(1)),
            Arguments.of(v(1), 1, UInt32.valueOf(2), v(0)),
            Arguments.of(Value.MAX_VALUE.subtract(2), 1, UInt32.MAX_VALUE, Value.MAX_VALUE.subtract(1)),
            Arguments.of(Value.MAX_VALUE.subtract(1), 1, UInt32.MAX_VALUE, v(0)),
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
  void addModLongLong(Value v1, int v2, int m, Value expected) {
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
  void subtract(Value v1, Value v2, Value expected) {
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
            Arguments.of(v(0), v(1), Value.MAX_VALUE),
            Arguments.of(v(1), v(2), Value.MAX_VALUE),
            Arguments.of(Value.MAX_VALUE, v(1), hv("0xFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("subtractUInt32Provider")
  void subtractUInt32(Value v1, UInt32 v2, Value expected) {
    assertValueEquals(expected, v1.subtract(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(1), UInt32.ZERO, v(1)),
            Arguments.of(v(5), UInt32.ZERO, v(5)),
            Arguments.of(v(2), UInt32.ONE, v(1)),
            Arguments.of(v(100), UInt32.valueOf(100), v(0)),
            Arguments.of(v(0), UInt32.ONE, Value.MAX_VALUE),
            Arguments.of(v(1), UInt32.valueOf(2), Value.MAX_VALUE),
            Arguments.of(Value.MAX_VALUE, UInt32.ONE, hv("0xFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("subtractLongProvider")
  void subtractLong(Value v1, int v2, Value expected) {
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
            Arguments.of(v(0), 1, Value.MAX_VALUE),
            Arguments.of(v(1), 2, Value.MAX_VALUE),
            Arguments.of(Value.MAX_VALUE, 1, hv("0xFFFFFFFE")),
            Arguments.of(v(0), -1, v(1)),
            Arguments.of(v(0), -100, v(100)),
            Arguments.of(v(2), -2, v(4)));
  }

  @ParameterizedTest
  @MethodSource("multiplyProvider")
  void multiply(Value v1, Value v2, Value expected) {
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
            Arguments.of(v(2), v(8), v(16)),
            Arguments.of(v(7), v(8), v(56)),
            Arguments.of(v(8), v(8), v(64)),
            Arguments.of(v(17), v(8), v(136)),
            Arguments.of(v(22), v(0), v(0)));
  }

  @ParameterizedTest
  @MethodSource("multiplyUInt32Provider")
  void multiplyUInt32(Value v1, UInt32 v2, Value expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.valueOf(2), v(0)),
            Arguments.of(v(1), UInt32.valueOf(2), v(2)),
            Arguments.of(v(2), UInt32.valueOf(2), v(4)),
            Arguments.of(v(3), UInt32.valueOf(2), v(6)),
            Arguments.of(v(4), UInt32.valueOf(2), v(8)),
            Arguments.of(v(10), UInt32.valueOf(18), v(180)),
            Arguments.of(v(2), UInt32.valueOf(8), v(16)),
            Arguments.of(v(7), UInt32.valueOf(8), v(56)),
            Arguments.of(v(8), UInt32.valueOf(8), v(64)),
            Arguments.of(v(17), UInt32.valueOf(8), v(136)),
            Arguments.of(v(22), UInt32.ZERO, v(0)),
            Arguments.of(hv("0xFFFFFFFF"), UInt32.valueOf(2), hv("0xFFFFFFFE")),
            Arguments.of(hv("0xFFFFFFFF"), UInt32.valueOf(2), hv("0xFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("multiplyLongProvider")
  void multiplyLong(Value v1, int v2, Value expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyLongProvider() {
    return Stream
        .of(
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
            Arguments.of(hv("0xFFFFFFFF"), 2, hv("0xFFFFFFFE")),
            Arguments.of(hv("0xFFFFFFFF"), 2, hv("0xFFFFFFFE")));
  }

  @Test
  void shouldThrowForMultiplyLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(2).multiply(-5));
    assertEquals("multiply unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModProvider")
  void multiplyMod(Value v1, Value v2, UInt32 m, Value expected) {
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
            Arguments.of(hv("0xFFFFFFFE"), v(2), UInt32.MAX_VALUE, hv("0xFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModOfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).multiplyMod(v(1), UInt32.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModUInt32UInt32Provider")
  void multiplyModUInt32UInt32(Value v1, UInt32 v2, UInt32 m, Value expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModUInt32UInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.valueOf(5), UInt32.valueOf(2), v(0)),
            Arguments.of(v(2), UInt32.valueOf(3), UInt32.valueOf(7), v(6)),
            Arguments.of(v(2), UInt32.valueOf(3), UInt32.valueOf(6), v(0)),
            Arguments.of(v(2), UInt32.ZERO, UInt32.valueOf(6), v(0)),
            Arguments.of(hv("0xFFFFFFFE"), UInt32.valueOf(2), UInt32.MAX_VALUE, hv("0xFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModUInt32UInt32OfModZero() {
    Throwable exception =
        assertThrows(ArithmeticException.class, () -> v(0).multiplyMod(UInt32.valueOf(5), UInt32.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongUInt32Provider")
  void multiplyModLongUInt32(Value v1, int v2, UInt32 m, Value expected) {
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
            Arguments.of(hv("0xFFFFFFFE"), 2, UInt32.MAX_VALUE, hv("0xFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModLongUInt32OfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(3).multiplyMod(1, UInt32.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongUInt32OfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).multiplyMod(-1, UInt32.valueOf(2)));
    assertEquals("multiplyMod unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongLongProvider")
  void multiplyModLongLong(Value v1, int v2, int m, Value expected) {
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
  void divide(Value v1, Value v2, Value expected) {
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
  @MethodSource("divideUInt32Provider")
  void divideUInt32(Value v1, UInt32 v2, Value expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.valueOf(2), v(0)),
            Arguments.of(v(1), UInt32.valueOf(2), v(0)),
            Arguments.of(v(2), UInt32.valueOf(2), v(1)),
            Arguments.of(v(3), UInt32.valueOf(2), v(1)),
            Arguments.of(v(4), UInt32.valueOf(2), v(2)),
            Arguments.of(v(2), UInt32.valueOf(8), v(0)),
            Arguments.of(v(7), UInt32.valueOf(8), v(0)),
            Arguments.of(v(8), UInt32.valueOf(8), v(1)),
            Arguments.of(v(9), UInt32.valueOf(8), v(1)),
            Arguments.of(v(17), UInt32.valueOf(8), v(2)),
            Arguments.of(v(1024), UInt32.valueOf(8), v(128)),
            Arguments.of(v(1026), UInt32.valueOf(8), v(128)));
  }

  @Test
  void shouldThrowForDivideUInt32ByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(UInt32.ZERO));
    assertEquals("divide by zero", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("divideLongProvider")
  void divideLong(Value v1, int v2, Value expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideLongProvider() {
    return Stream
        .of(
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
  void powUInt32(Value v1, UInt32 v2, Value expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.valueOf(2), v(0)),
            Arguments.of(v(2), UInt32.valueOf(2), v(4)),
            Arguments.of(v(2), UInt32.valueOf(8), v(256)),
            Arguments.of(v(3), UInt32.valueOf(3), v(27)));
  }

  @ParameterizedTest
  @MethodSource("powLongProvider")
  void powLong(Value v1, int v2, Value expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2, v(0)),
            Arguments.of(v(2), 2, v(4)),
            Arguments.of(v(2), 8, v(256)),
            Arguments.of(v(3), 3, v(27)));
  }

  @ParameterizedTest
  @MethodSource("modUInt32Provider")
  void modUInt32(Value v1, UInt32 v2, Value expected) {
    assertValueEquals(expected, v1.mod(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> modUInt32Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt32.valueOf(2), v(0)),
            Arguments.of(v(1), UInt32.valueOf(2), v(1)),
            Arguments.of(v(2), UInt32.valueOf(2), v(0)),
            Arguments.of(v(3), UInt32.valueOf(2), v(1)),
            Arguments.of(v(0), UInt32.valueOf(8), v(0)),
            Arguments.of(v(1), UInt32.valueOf(8), v(1)),
            Arguments.of(v(2), UInt32.valueOf(8), v(2)),
            Arguments.of(v(3), UInt32.valueOf(8), v(3)),
            Arguments.of(v(7), UInt32.valueOf(8), v(7)),
            Arguments.of(v(8), UInt32.valueOf(8), v(0)),
            Arguments.of(v(9), UInt32.valueOf(8), v(1)),
            Arguments.of(v(1024), UInt32.valueOf(8), v(0)),
            Arguments.of(v(1026), UInt32.valueOf(8), v(2)));
  }

  @Test
  void shouldThrowForModUInt32ByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).mod(UInt32.ZERO));
    assertEquals("mod by zero", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("modLongProvider")
  void modLong(Value v1, int v2, Value expected) {
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
  @MethodSource("compareToProvider")
  void compareTo(Value v1, Value v2, int expected) {
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
  void toBytesTest(Value value, Bytes expected) {
    assertEquals(expected, value.toBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toBytesProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), Bytes.fromHexString("0x00000000")),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")));
  }

  @ParameterizedTest
  @MethodSource("toMinimalBytesProvider")
  void toMinimalBytesTest(Value value, Bytes expected) {
    assertEquals(expected, value.toMinimalBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toMinimalBytesProvider() {
    return Stream
        .of(Arguments.of(hv("0x00"), Bytes.EMPTY), Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")));
  }

  @ParameterizedTest
  @MethodSource("numberOfLeadingZerosProvider")
  void numberOfLeadingZeros(Value value, int expected) {
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
            Arguments.of(hv("0x8F"), 24));
  }

  @ParameterizedTest
  @MethodSource("bitLengthProvider")
  void bitLength(Value value, int expected) {
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
            Arguments.of(hv("0x8F"), 8));
  }

  @ParameterizedTest
  @MethodSource("addExactProvider")
  void addExact(Value value, Value operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactProvider() {
    return Stream.of(Arguments.of(Value.MAX_VALUE, v(1)), Arguments.of(Value.MAX_VALUE, Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addExactLongProvider")
  void addExactLong(Value value, int operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactLongProvider() {
    return Stream
        .of(Arguments.of(Value.MAX_VALUE, 3), Arguments.of(Value.MAX_VALUE, Integer.MAX_VALUE), Arguments.of(v(0), -1));
  }

  @ParameterizedTest
  @MethodSource("subtractExactProvider")
  void subtractExact(Value value, Value operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactProvider() {
    return Stream.of(Arguments.of(v(0), v(1)), Arguments.of(v(0), Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("subtractExactLongProvider")
  void subtractExactLong(Value value, int operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactLongProvider() {
    return Stream.of(Arguments.of(v(0), 1), Arguments.of(v(0), Integer.MAX_VALUE), Arguments.of(Value.MAX_VALUE, -1));
  }

  private void assertValueEquals(Value expected, Value actual) {
    String msg = String.format("Expected %s but got %s", expected.toHexString(), actual.toHexString());
    assertEquals(expected, actual, msg);
  }
}