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
class BaseUInt64ValueTest {

  private static class Value extends BaseUInt64Value<Value> {
    static final Value MAX_VALUE = new Value(UInt64.MAX_VALUE);

    Value(UInt64 v) {
      super(v, Value::new);
    }

    Value(long v) {
      super(v, Value::new);
    }
  }

  private static Value v(long v) {
    return new Value(v);
  }

  private static Value hv(String s) {
    return new Value(UInt64.fromHexString(s));
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
            Arguments.of(hv("0xFFFFFFFFFFFFFFF0"), v(1), hv("0xFFFFFFFFFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), v(1), Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addUInt64Provider")
  void addUInt64(Value v1, UInt64 v2, Value expected) {
    assertValueEquals(expected, v1.add(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(1), UInt64.ZERO, v(1)),
            Arguments.of(v(5), UInt64.ZERO, v(5)),
            Arguments.of(v(0), UInt64.ONE, v(1)),
            Arguments.of(v(0), UInt64.valueOf(100), v(100)),
            Arguments.of(v(2), UInt64.valueOf(2), v(4)),
            Arguments.of(v(100), UInt64.valueOf(90), v(190)),
            Arguments.of(Value.MAX_VALUE, UInt64.valueOf(1), v(0)),
            Arguments.of(Value.MAX_VALUE, UInt64.valueOf(2), v(1)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFF0"), UInt64.valueOf(1), hv("0xFFFFFFFFFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), UInt64.valueOf(1), Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addLongProvider")
  void addLong(Value v1, long v2, Value expected) {
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
            Arguments.of(Value.MAX_VALUE, 1L, v(0)),
            Arguments.of(Value.MAX_VALUE, 2L, v(1)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFF0"), 1L, hv("0xFFFFFFFFFFFFFFF1")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), 1L, Value.MAX_VALUE),
            Arguments.of(v(10), -5L, v(5)),
            Arguments.of(v(0), -1L, Value.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("addModProvider")
  void addMod(Value v1, Value v2, UInt64 m, Value expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(1), UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), v(1), UInt64.valueOf(2), v(0)),
            Arguments.of(Value.MAX_VALUE.subtract(2), v(1), UInt64.MAX_VALUE, Value.MAX_VALUE.subtract(1)),
            Arguments.of(Value.MAX_VALUE.subtract(1), v(1), UInt64.MAX_VALUE, v(0)),
            Arguments.of(v(2), v(1), UInt64.valueOf(2), v(1)),
            Arguments.of(v(3), v(2), UInt64.valueOf(6), v(5)),
            Arguments.of(v(3), v(4), UInt64.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModOfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(v(1), UInt64.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModUInt64UInt64Provider")
  void addModUInt64UInt64(Value v1, UInt64 v2, UInt64 m, Value expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModUInt64UInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.ONE, UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), UInt64.ONE, UInt64.valueOf(2), v(0)),
            Arguments.of(Value.MAX_VALUE.subtract(2), UInt64.ONE, UInt64.MAX_VALUE, Value.MAX_VALUE.subtract(1)),
            Arguments.of(Value.MAX_VALUE.subtract(1), UInt64.ONE, UInt64.MAX_VALUE, v(0)),
            Arguments.of(v(2), UInt64.ONE, UInt64.valueOf(2), v(1)),
            Arguments.of(v(3), UInt64.valueOf(2), UInt64.valueOf(6), v(5)),
            Arguments.of(v(3), UInt64.valueOf(4), UInt64.valueOf(2), v(1)));
  }

  @Test
  void shouldThrowForAddModLongUInt64OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(1, UInt64.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongUInt64Provider")
  void addModLongUInt64(Value v1, long v2, UInt64 m, Value expected) {
    assertValueEquals(expected, v1.addMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addModLongUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 1L, UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), 1L, UInt64.valueOf(2), v(0)),
            Arguments.of(Value.MAX_VALUE.subtract(2), 1L, UInt64.MAX_VALUE, Value.MAX_VALUE.subtract(1)),
            Arguments.of(Value.MAX_VALUE.subtract(1), 1L, UInt64.MAX_VALUE, v(0)),
            Arguments.of(v(2), 1L, UInt64.valueOf(2), v(1)),
            Arguments.of(v(2), -1L, UInt64.valueOf(2), v(1)),
            Arguments.of(v(1), -7L, UInt64.valueOf(5), v(4)));
  }

  @Test
  void shouldThrowForAddModUInt64UInt64OfZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).addMod(UInt64.ONE, UInt64.ZERO));
    assertEquals("addMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("addModLongLongProvider")
  void addModLongLong(Value v1, long v2, long m, Value expected) {
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
            Arguments.of(Value.MAX_VALUE, v(1), hv("0xFFFFFFFFFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("subtractUInt64Provider")
  void subtractUInt64(Value v1, UInt64 v2, Value expected) {
    assertValueEquals(expected, v1.subtract(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(1), UInt64.ZERO, v(1)),
            Arguments.of(v(5), UInt64.ZERO, v(5)),
            Arguments.of(v(2), UInt64.ONE, v(1)),
            Arguments.of(v(100), UInt64.valueOf(100), v(0)),
            Arguments.of(v(0), UInt64.ONE, Value.MAX_VALUE),
            Arguments.of(v(1), UInt64.valueOf(2), Value.MAX_VALUE),
            Arguments.of(Value.MAX_VALUE, UInt64.ONE, hv("0xFFFFFFFFFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("subtractLongProvider")
  void subtractLong(Value v1, long v2, Value expected) {
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
            Arguments.of(v(0), 1L, Value.MAX_VALUE),
            Arguments.of(v(1), 2L, Value.MAX_VALUE),
            Arguments.of(Value.MAX_VALUE, 1L, hv("0xFFFFFFFFFFFFFFFE")),
            Arguments.of(v(0), -1L, v(1)),
            Arguments.of(v(0), -100L, v(100)),
            Arguments.of(v(2), -2L, v(4)));
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
  @MethodSource("multiplyUInt64Provider")
  void multiplyUInt64(Value v1, UInt64 v2, Value expected) {
    assertValueEquals(expected, v1.multiply(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.valueOf(2), v(0)),
            Arguments.of(v(1), UInt64.valueOf(2), v(2)),
            Arguments.of(v(2), UInt64.valueOf(2), v(4)),
            Arguments.of(v(3), UInt64.valueOf(2), v(6)),
            Arguments.of(v(4), UInt64.valueOf(2), v(8)),
            Arguments.of(v(10), UInt64.valueOf(18), v(180)),
            Arguments.of(v(2), UInt64.valueOf(8), v(16)),
            Arguments.of(v(7), UInt64.valueOf(8), v(56)),
            Arguments.of(v(8), UInt64.valueOf(8), v(64)),
            Arguments.of(v(17), UInt64.valueOf(8), v(136)),
            Arguments.of(v(22), UInt64.ZERO, v(0)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), UInt64.valueOf(2), hv("0xFFFFFFFFFFFFFFFE")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), UInt64.valueOf(2), hv("0xFFFFFFFFFFFFFFFE")));
  }

  @ParameterizedTest
  @MethodSource("multiplyLongProvider")
  void multiplyLong(Value v1, long v2, Value expected) {
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
            Arguments.of(v(2), 8L, v(16)),
            Arguments.of(v(7), 8L, v(56)),
            Arguments.of(v(8), 8L, v(64)),
            Arguments.of(v(17), 8L, v(136)),
            Arguments.of(v(22), 0L, v(0)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), 2L, hv("0xFFFFFFFFFFFFFFFE")),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), 2L, hv("0xFFFFFFFFFFFFFFFE")));
  }

  @Test
  void shouldThrowForMultiplyLongOfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(2).multiply(-5));
    assertEquals("multiply unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModProvider")
  void multiplyMod(Value v1, Value v2, UInt64 m, Value expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModProvider() {
    return Stream
        .of(
            Arguments.of(v(0), v(5), UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), v(3), UInt64.valueOf(7), v(6)),
            Arguments.of(v(2), v(3), UInt64.valueOf(6), v(0)),
            Arguments.of(v(2), v(0), UInt64.valueOf(6), v(0)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), v(2), UInt64.MAX_VALUE, hv("0xFFFFFFFFFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModOfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(0).multiplyMod(v(1), UInt64.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModUInt64UInt64Provider")
  void multiplyModUInt64UInt64(Value v1, UInt64 v2, UInt64 m, Value expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModUInt64UInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.valueOf(5), UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), UInt64.valueOf(3), UInt64.valueOf(7), v(6)),
            Arguments.of(v(2), UInt64.valueOf(3), UInt64.valueOf(6), v(0)),
            Arguments.of(v(2), UInt64.ZERO, UInt64.valueOf(6), v(0)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), UInt64.valueOf(2), UInt64.MAX_VALUE, hv("0xFFFFFFFFFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModUInt64UInt64OfModZero() {
    Throwable exception =
        assertThrows(ArithmeticException.class, () -> v(0).multiplyMod(UInt64.valueOf(5), UInt64.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongUInt64Provider")
  void multiplyModLongUInt64(Value v1, long v2, UInt64 m, Value expected) {
    assertValueEquals(expected, v1.multiplyMod(v2, m));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> multiplyModLongUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), 5L, UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), 3L, UInt64.valueOf(7), v(6)),
            Arguments.of(v(2), 3L, UInt64.valueOf(6), v(0)),
            Arguments.of(v(2), 0L, UInt64.valueOf(6), v(0)),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFE"), 2L, UInt64.MAX_VALUE, hv("0xFFFFFFFFFFFFFFFD")));
  }

  @Test
  void shouldThrowForMultiplyModLongUInt64OfModZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(3).multiplyMod(1L, UInt64.ZERO));
    assertEquals("multiplyMod with zero modulus", exception.getMessage());
  }

  @Test
  void shouldThrowForMultiplyModLongUInt64OfNegative() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).multiplyMod(-1, UInt64.valueOf(2)));
    assertEquals("multiplyMod unsigned by negative", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("multiplyModLongLongProvider")
  void multiplyModLongLong(Value v1, long v2, long m, Value expected) {
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
            Arguments.of(hv("0x0FFFFFFFFFFFFFFE"), 2L, Long.MAX_VALUE, hv("0x1FFFFFFFFFFFFFFC")));
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
  @MethodSource("divideUInt64Provider")
  void divideUInt64(Value v1, UInt64 v2, Value expected) {
    assertValueEquals(expected, v1.divide(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> divideUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.valueOf(2), v(0)),
            Arguments.of(v(1), UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), UInt64.valueOf(2), v(1)),
            Arguments.of(v(3), UInt64.valueOf(2), v(1)),
            Arguments.of(v(4), UInt64.valueOf(2), v(2)),
            Arguments.of(v(2), UInt64.valueOf(8), v(0)),
            Arguments.of(v(7), UInt64.valueOf(8), v(0)),
            Arguments.of(v(8), UInt64.valueOf(8), v(1)),
            Arguments.of(v(9), UInt64.valueOf(8), v(1)),
            Arguments.of(v(17), UInt64.valueOf(8), v(2)),
            Arguments.of(v(1024), UInt64.valueOf(8), v(128)),
            Arguments.of(v(1026), UInt64.valueOf(8), v(128)));
  }

  @Test
  void shouldThrowForDivideUInt64ByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).divide(UInt64.ZERO));
    assertEquals("divide by zero", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("divideLongProvider")
  void divideLong(Value v1, long v2, Value expected) {
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
            Arguments.of(v(2), 8L, v(0)),
            Arguments.of(v(7), 8L, v(0)),
            Arguments.of(v(8), 8L, v(1)),
            Arguments.of(v(9), 8L, v(1)),
            Arguments.of(v(17), 8L, v(2)),
            Arguments.of(v(1024), 8L, v(128)),
            Arguments.of(v(1026), 8L, v(128)));
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
  @MethodSource("powUInt64Provider")
  void powUInt64(Value v1, UInt64 v2, Value expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.valueOf(2), v(0)),
            Arguments.of(v(2), UInt64.valueOf(2), v(4)),
            Arguments.of(v(2), UInt64.valueOf(8), v(256)),
            Arguments.of(v(3), UInt64.valueOf(3), v(27)),
            Arguments.of(hv("0xFFFFFFFFFFF0F0F0"), UInt64.valueOf(3), hv("0xF2A920E119A2F000")));
  }

  @ParameterizedTest
  @MethodSource("powLongProvider")
  void powLong(Value v1, long v2, Value expected) {
    assertValueEquals(expected, v1.pow(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> powLongProvider() {
    return Stream
        .of(
            Arguments.of(v(0), 2L, v(0)),
            Arguments.of(v(2), 2L, v(4)),
            Arguments.of(v(2), 8L, v(256)),
            Arguments.of(v(3), 3L, v(27)));
  }

  @ParameterizedTest
  @MethodSource("modUInt64Provider")
  void modUInt64(Value v1, UInt64 v2, Value expected) {
    assertValueEquals(expected, v1.mod(v2));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> modUInt64Provider() {
    return Stream
        .of(
            Arguments.of(v(0), UInt64.valueOf(2), v(0)),
            Arguments.of(v(1), UInt64.valueOf(2), v(1)),
            Arguments.of(v(2), UInt64.valueOf(2), v(0)),
            Arguments.of(v(3), UInt64.valueOf(2), v(1)),
            Arguments.of(v(0), UInt64.valueOf(8), v(0)),
            Arguments.of(v(1), UInt64.valueOf(8), v(1)),
            Arguments.of(v(2), UInt64.valueOf(8), v(2)),
            Arguments.of(v(3), UInt64.valueOf(8), v(3)),
            Arguments.of(v(7), UInt64.valueOf(8), v(7)),
            Arguments.of(v(8), UInt64.valueOf(8), v(0)),
            Arguments.of(v(9), UInt64.valueOf(8), v(1)),
            Arguments.of(v(1024), UInt64.valueOf(8), v(0)),
            Arguments.of(v(1026), UInt64.valueOf(8), v(2)));
  }

  @Test
  void shouldThrowForModUInt64ByZero() {
    Throwable exception = assertThrows(ArithmeticException.class, () -> v(5).mod(UInt64.ZERO));
    assertEquals("mod by zero", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("modLongProvider")
  void modLong(Value v1, long v2, Value expected) {
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
            Arguments.of(v(0), 8L, v(0)),
            Arguments.of(v(1), 8L, v(1)),
            Arguments.of(v(2), 8L, v(2)),
            Arguments.of(v(3), 8L, v(3)),
            Arguments.of(v(7), 8L, v(7)),
            Arguments.of(v(8), 8L, v(0)),
            Arguments.of(v(9), 8L, v(1)),
            Arguments.of(v(1024), 8L, v(0)),
            Arguments.of(v(1026), 8L, v(2)));
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
            Arguments.of(hv("0x0000000000000000"), hv("0x0000000000000000"), 0),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), hv("0xFFFFFFFFFFFFFFFF"), 0),
            Arguments.of(hv("0x00000000FFFFFFFF"), hv("0x00000000FFFFFFFF"), 0),
            Arguments.of(hv("0xFFFFFFFFFFFFFFFF"), hv("0x0000000000000000"), 1),
            Arguments.of(hv("0x0000000000000000"), hv("0xFFFFFFFFFFFFFFFF"), -1),
            Arguments.of(hv("0x00000001FFFFFFFF"), hv("0x00000000FFFFFFFF"), 1),
            Arguments.of(hv("0x00000000FFFFFFFE"), hv("0x00000000FFFFFFFF"), -1));
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
            Arguments.of(hv("0x00"), Bytes.fromHexString("0x0000000000000000")),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x0000000001000000")),
            Arguments.of(hv("0x0100000000"), Bytes.fromHexString("0x0000000100000000")),
            Arguments.of(hv("0xf100000000ab"), Bytes.fromHexString("0x0000f100000000ab")));
  }

  @ParameterizedTest
  @MethodSource("toMinimalBytesProvider")
  void toMinimalBytesTest(Value value, Bytes expected) {
    assertEquals(expected, value.toMinimalBytes());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> toMinimalBytesProvider() {
    return Stream
        .of(
            Arguments.of(hv("0x00"), Bytes.EMPTY),
            Arguments.of(hv("0x01000000"), Bytes.fromHexString("0x01000000")),
            Arguments.of(hv("0x0100000000"), Bytes.fromHexString("0x0100000000")),
            Arguments.of(hv("0xf100000000ab"), Bytes.fromHexString("0xf100000000ab")));
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
            Arguments.of(hv("0x00"), 64),
            Arguments.of(hv("0x01"), 63),
            Arguments.of(hv("0x02"), 62),
            Arguments.of(hv("0x03"), 62),
            Arguments.of(hv("0x0F"), 60),
            Arguments.of(hv("0x8F"), 56),
            Arguments.of(hv("0x100000000"), 31));
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
            Arguments.of(hv("0x8F"), 8),
            Arguments.of(hv("0x100000000"), 33));
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
  void addExactLong(Value value, long operand) {
    assertThrows(ArithmeticException.class, () -> value.addExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> addExactLongProvider() {
    return Stream
        .of(Arguments.of(Value.MAX_VALUE, 3), Arguments.of(Value.MAX_VALUE, Long.MAX_VALUE), Arguments.of(v(0), -1));
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
  void subtractExactLong(Value value, long operand) {
    assertThrows(ArithmeticException.class, () -> value.subtractExact(operand));
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> subtractExactLongProvider() {
    return Stream.of(Arguments.of(v(0), 1), Arguments.of(v(0), Long.MAX_VALUE), Arguments.of(Value.MAX_VALUE, -1));
  }

  private void assertValueEquals(Value expected, Value actual) {
    String msg = String.format("Expected %s but got %s", expected.toHexString(), actual.toHexString());
    assertEquals(expected, actual, msg);
  }
}