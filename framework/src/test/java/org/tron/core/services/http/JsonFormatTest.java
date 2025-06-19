package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.protos.Protocol;

public class JsonFormatTest {
  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testPrintErrorMsg() {
    Exception ex = new Exception("test");
    String out = JsonFormat.printErrorMsg(ex);
    assertEquals("{\"Error\":\"test\"}", out);
  }

  @Test
  public void testPrintWithHelloMessage() throws IOException {
    Protocol.HelloMessage message = Protocol.HelloMessage.newBuilder()
        .setAddress(ByteString.copyFrom("address".getBytes()))
        .build();
    StringWriter output = new StringWriter();

    JsonFormat.print(message, output, true);
    assertNotNull(output.toString());
  }

  private UnknownFieldSet createValidUnknownFieldSet() {
    UnknownFieldSet unknownFieldSet2 = UnknownFieldSet.newBuilder().build();
    UnknownFieldSet.Field unknownField1 = UnknownFieldSet.Field.newBuilder()
        .addFixed32(123)
        .addFixed64(12345L)
        .addGroup(unknownFieldSet2)
        .addLengthDelimited(ByteString.copyFrom("length".getBytes()))
        .addVarint(12345678L)
        .build();

    return UnknownFieldSet.newBuilder()
        .addField(1, unknownField1)
        .build();
  }

  @Test
  public void testPrintWithFields() throws IOException {
    UnknownFieldSet unknownFieldSet = createValidUnknownFieldSet();
    StringWriter output = new StringWriter();
    JsonFormat.print(unknownFieldSet, output, true);
    assertNotNull(output.toString());
  }

  @Test
  public void testPrintToString() {
    UnknownFieldSet unknownFieldSet = createValidUnknownFieldSet();
    String output = JsonFormat.printToString(unknownFieldSet, true);
    assertNotNull(output);
  }

  @Test
  public void testUnsignedToString() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("unsignedToString", int.class);
    privateMethod.setAccessible(true);
    String out3 =  (String)privateMethod.invoke(null, 100);
    assertEquals("100", out3);
    String out4 = (String)privateMethod.invoke(null, -100);
    assertNotNull(out4);
  }

  @Test
  public void testToStringBuilderWithNonReadableInput() throws Exception {
    String inputString = "Hello, World!";
    Readable input = new CharArrayReader(inputString.toCharArray());

    Method privateMethod = JsonFormat.class.getDeclaredMethod("toStringBuilder", Readable.class);
    privateMethod.setAccessible(true);

    StringBuilder out =  (StringBuilder)privateMethod.invoke(null, input);
    assertEquals(inputString, out.toString());
  }


  @Test
  public void testUnicodeEscaped() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("unicodeEscaped", char.class);
    privateMethod.setAccessible(true);


    char input1 = 0x09;
    String out =  (String)privateMethod.invoke(null, input1);
    assertNotNull(out);

    char input2 = 0x99;
    String out2 =  (String)privateMethod.invoke(null, input2);
    assertNotNull(out2);

    char input3 = 0x999;
    String out3 =  (String)privateMethod.invoke(null, input3);
    assertNotNull(out3);

    char input4 = 0x1001;
    String out4 =  (String)privateMethod.invoke(null, input4);
    assertNotNull(out4);
  }

  @Test
  public void testEscapeText() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("escapeText", String.class);
    privateMethod.setAccessible(true);

    String input1 = "\b\f\n\r\t\\\"\\b\\f\\n\\r\\t\\\\\"test123";
    String out = (String)privateMethod.invoke(null, input1);
    assertNotNull(out);
  }

  @Test
  public void testAppendEscapedUnicode() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("appendEscapedUnicode",
        StringBuilder.class, char.class);
    privateMethod.setAccessible(true);

    char input1 = 0x09;
    StringBuilder out1 = new StringBuilder();

    privateMethod.invoke(null, out1, input1);
    assertNotNull(out1);

    char input2 = 0x99;
    StringBuilder out2 = new StringBuilder();
    privateMethod.invoke(null, out2, input2);
    assertNotNull(out2);

    char input3 = 0x999;
    StringBuilder out3 = new StringBuilder();
    privateMethod.invoke(null, out3, input3);
    assertNotNull(out3);

    char input4 = 0x1001;
    StringBuilder out4 = new StringBuilder();
    privateMethod.invoke(null, out4, input4);
    assertNotNull(out4);
  }

  @Test
  public void testUnescapeText() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("unescapeText", String.class);
    privateMethod.setAccessible(true);

    String input = "\\u1234\\b\\f\\n\\r\\t\\\\\"test123";;
    String out = (String)privateMethod.invoke(null, input);
    assertNotNull(out);
  }

  @Test
  public void testDigitValue() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("digitValue", char.class);
    privateMethod.setAccessible(true);

    char input = '1';
    int out = (int)privateMethod.invoke(null, input);
    assertEquals(1, out);

    char input1 = 'b';
    int out1 = (int)privateMethod.invoke(null, input1);
    assertEquals(11, out1);

    char input2 = 'B';
    int out2 = (int)privateMethod.invoke(null, input2);
    assertEquals(11, out2);
  }

  @Test
  public void testParseUInt64() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("parseUInt64", String.class);
    privateMethod.setAccessible(true);

    String input = "12312312312";
    long out = (long)privateMethod.invoke(null, input);
    assertEquals(12312312312L, out);

    String input1 = "0x10";
    long out1 = (long)privateMethod.invoke(null, input1);
    assertEquals(16L, out1);

    String input2 = "010";
    long out2 = (long)privateMethod.invoke(null, input2);
    assertEquals(8L, out2);

    String input3 = "-12312312312";
    Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
      privateMethod.invoke(null, input3);
    });
    Throwable cause = thrown.getCause();
    assertTrue(cause instanceof NumberFormatException);
  }

  @Test
  public void testParseInteger() throws Exception {
    Method privateMethod = JsonFormat.class.getDeclaredMethod("parseInteger",
        String.class, boolean.class, boolean.class);
    privateMethod.setAccessible(true);

    String input1 = "92233720368547758070";
    Throwable thrown = assertThrows(InvocationTargetException.class, () -> {
      privateMethod.invoke(null,input1, true, true);
    });
    Throwable cause = thrown.getCause();
    assertTrue(cause instanceof NumberFormatException);

    String input5 = "92233720368547758070";
    thrown = assertThrows(InvocationTargetException.class, () -> {
      privateMethod.invoke(null,input5, false, true);
    });
    cause = thrown.getCause();
    assertTrue(cause instanceof NumberFormatException);

    String input2 = "-92233720368547758";
    thrown = assertThrows(InvocationTargetException.class, () -> {
      privateMethod.invoke(null,input2, false, true);
    });
    cause = thrown.getCause();
    assertTrue(cause instanceof NumberFormatException);

    String input3 = "92233720368547758070";
    thrown = assertThrows(InvocationTargetException.class, () -> {
      privateMethod.invoke(null,input3, false, false);
    });
    cause = thrown.getCause();
    assertTrue(cause instanceof NumberFormatException);

    String input4 = "-92233720368547758070";
    thrown = assertThrows(InvocationTargetException.class, () -> {
      privateMethod.invoke(null,input4, true, false);
    });
    cause = thrown.getCause();
    assertTrue(cause instanceof NumberFormatException);
  }

}