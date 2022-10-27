package org.tron.common.utils;

import lombok.Data;
import org.junit.Assert;
import org.junit.Test;

public class JsonUtilTest {

  @Data
  public static class A {
    private String key;
    private int value;

    public A() {}

    public A(String key, int value) {
      this.key = key;
      this.value = value;
    }
  }

  @Test
  public void test() {
    A a1 = new A();
    a1.setKey("abc");
    a1.setValue(100);

    String jsonString = JsonUtil.obj2Json(a1);

    A a2 = JsonUtil.json2Obj(jsonString, A.class);

    Assert.assertEquals(a2.getKey(), "abc");
    Assert.assertEquals(a2.getValue(), 100);
  }
}
