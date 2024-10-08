package org.tron.core.services.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.tron.protos.Protocol;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JsonFormat.class,
})
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
    String out1 = Whitebox.invokeMethod(JsonFormat.class,
        "unsignedToString", 100L);
    assertEquals("100", out1);
    String out2 = Whitebox.invokeMethod(JsonFormat.class,
        "unsignedToString", -100L);
    assertNotNull(out2);

    String out3 = Whitebox.invokeMethod(JsonFormat.class,
        "unsignedToString", 100);
    assertEquals("100", out3);
    String out4 = Whitebox.invokeMethod(JsonFormat.class,
        "unsignedToString", -100);
    assertNotNull(out4);
  }

  @Test
  public void testToStringBuilderWithNonReadableInput() throws Exception {
    String inputString = "Hello, World!";
    Readable input = new CharArrayReader(inputString.toCharArray());

    StringBuilder out =  Whitebox.invokeMethod(JsonFormat.class,
        "toStringBuilder", input);
    assertEquals(inputString, out.toString());
  }


  @Test
  public void testUnicodeEscaped() throws Exception {
    char input1 = 0x09;
    String out =  Whitebox.invokeMethod(JsonFormat.class,
        "unicodeEscaped", input1);
    assertNotNull(out);

    char input2 = 0x99;
    String out2 =  Whitebox.invokeMethod(JsonFormat.class,
        "unicodeEscaped", input2);
    assertNotNull(out2);

    char input3 = 0x999;
    String out3 =  Whitebox.invokeMethod(JsonFormat.class,
        "unicodeEscaped", input3);
    assertNotNull(out3);

    char input4 = 0x1001;
    String out4 =  Whitebox.invokeMethod(JsonFormat.class,
        "unicodeEscaped", input4);
    assertNotNull(out4);
  }

  @Test
  public void testEscapeText() throws Exception {
    String input1 = "\b\f\n\r\t\\\"\\b\\f\\n\\r\\t\\\\\"test123";
    String out = Whitebox.invokeMethod(JsonFormat.class,
        "escapeText", input1);
    assertNotNull(out);
  }

  @Test
  public void testAppendEscapedUnicode() throws Exception {
    char input1 = 0x09;
    StringBuilder out1 = new StringBuilder();
    Whitebox.invokeMethod(JsonFormat.class,
        "appendEscapedUnicode", out1, input1);
    assertNotNull(out1);

    char input2 = 0x99;
    StringBuilder out2 = new StringBuilder();
    Whitebox.invokeMethod(JsonFormat.class,
        "appendEscapedUnicode", out2, input2);
    assertNotNull(out2);

    char input3 = 0x999;
    StringBuilder out3 = new StringBuilder();
    Whitebox.invokeMethod(JsonFormat.class,
        "appendEscapedUnicode", out3, input3);
    assertNotNull(out3);

    char input4 = 0x1001;
    StringBuilder out4 = new StringBuilder();
    Whitebox.invokeMethod(JsonFormat.class,
        "appendEscapedUnicode", out4, input4);
    assertNotNull(out4);
  }

  @Test
  public void testUnescapeText() throws Exception {
    String input = "\\u1234\\b\\f\\n\\r\\t\\\\\"test123";;
    String out = Whitebox.invokeMethod(JsonFormat.class,
        "unescapeText", input);
    assertNotNull(out);
  }

  @Test
  public void testDigitValue() throws Exception {
    char input = '1';
    int out = Whitebox.invokeMethod(JsonFormat.class,
        "digitValue", input);
    assertEquals(1, out);

    char input1 = 'b';
    int out1 = Whitebox.invokeMethod(JsonFormat.class,
        "digitValue", input1);
    assertEquals(11, out1);

    char input2 = 'B';
    int out2 = Whitebox.invokeMethod(JsonFormat.class,
        "digitValue", input2);
    assertEquals(11, out2);
  }

  @Test
  public void testParseUInt64() throws Exception {
    String input = "12312312312";
    long out = Whitebox.invokeMethod(JsonFormat.class,
        "parseUInt64", input);
    assertEquals(12312312312L, out);

    String input1 = "0x10";
    long out1 = Whitebox.invokeMethod(JsonFormat.class,
        "parseUInt64", input1);
    assertEquals(16L, out1);

    String input2 = "010";
    long out2 = Whitebox.invokeMethod(JsonFormat.class,
        "parseUInt64", input2);
    assertEquals(8L, out2);

    String input3 = "-12312312312";
    assertThrows(
        NumberFormatException.class,
        () -> {
          Whitebox.invokeMethod(JsonFormat.class,
              "parseUInt64", input3);
        }
    );
  }

  @Test
  public void testParseInteger() {
    String input1 = "92233720368547758070";
    assertThrows(
        NumberFormatException.class,
        () -> {
          Whitebox.invokeMethod(JsonFormat.class, "parseInteger",
              input1, true, true);
        }
    );
    String input5 = "92233720368547758070";
    assertThrows(
        NumberFormatException.class,
        () -> {
          Whitebox.invokeMethod(JsonFormat.class, "parseInteger",
              input5, false, true);
        }
    );

    String input2 = "-92233720368547758";
    assertThrows(
        NumberFormatException.class,
        () -> {
          Whitebox.invokeMethod(JsonFormat.class, "parseInteger",
              input2, false, true);
        }
    );

    String input3 = "92233720368547758070";
    assertThrows(
        NumberFormatException.class,
        () -> {
          Whitebox.invokeMethod(JsonFormat.class, "parseInteger",
              input3, false, false);
        }
    );
    String input4 = "-92233720368547758070";
    assertThrows(
        NumberFormatException.class,
        () -> {
          Whitebox.invokeMethod(JsonFormat.class, "parseInteger",
              input4, true, false);
        }
    );
  }

}
