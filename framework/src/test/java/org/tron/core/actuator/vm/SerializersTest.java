package org.tron.core.actuator.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.tron.core.vm.trace.Serializers;

public class SerializersTest {

  @Test
  public void testSerializeFieldsOnly() {
    assertEquals("\"testString\"", Serializers.serializeFieldsOnly("testString"));
  }

  @Test
  public void testSerializeFieldsOnlyPojo() {
    TestBean bean = new TestBean("hello", 42);
    String json = Serializers.serializeFieldsOnly(bean);
    assertTrue("Should contain name field", json.contains("\"name\""));
    assertTrue("Should contain name value", json.contains("\"hello\""));
    assertTrue("Should contain value field", json.contains("\"value\""));
    assertTrue("Should contain value 42", json.contains("42"));
  }

  @Test
  public void testSerializeFieldsOnlyIgnoresGetters() {
    TestBean bean = new TestBean("hello", 42);
    String json = Serializers.serializeFieldsOnly(bean);
    // getComputedField() returns "computed" but should not appear
    // because getter visibility is NONE
    assertFalse("Should not serialize getter-only property",
        json.contains("computed"));
  }

  @Test
  public void testSerializeFieldsOnlyNull() {
    TestBean bean = new TestBean(null, 0);
    String json = Serializers.serializeFieldsOnly(bean);
    assertTrue("Should contain null name", json.replaceAll("\\s+", "")
        .contains("\"name\":null"));
    assertTrue("Should contain value 0", json.replaceAll("\\s+", "")
        .contains("\"value\":0"));
  }

  @Test
  public void testSerializeFieldsOnlyReturnsEmptyOnError() {
    // A non-serializable object should return "{}"
    assertEquals("{}", Serializers.serializeFieldsOnly(new Object() {
      // anonymous class with circular reference
      Object self = this;
    }));
  }

  @SuppressWarnings("unused")
  static class TestBean {
    private String name;
    private int value;

    TestBean(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getComputedField() {
      return "computed";
    }
  }
}
