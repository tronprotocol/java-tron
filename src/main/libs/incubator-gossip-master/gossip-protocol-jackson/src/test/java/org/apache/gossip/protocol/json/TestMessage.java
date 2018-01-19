/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.protocol.json;

import org.apache.gossip.model.Base;
import org.apache.gossip.udp.Trackable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
 * Here is a test class for serialization. I've tried to include a lot of things in it including nested classes.
 * Note that there are no Jackson annotations.
 * getters and setters are the keys to making this work without the Jackson annotations.
 */
class TestMessage extends Base implements Trackable {
  private String unique;
  private String from;
  private String uuid;
  private String derivedField;
  private Subclass otherThing;
  private float floatValue;
  private double doubleValue;
  private Object[] arrayOfThings;
  private Map<String, String> mapOfThings = new HashMap<>();

  @SuppressWarnings("unused")//Used by ObjectMapper
  private TestMessage() {
  }

  TestMessage(String unique) {
    this.unique = unique;
    from = Integer.toHexString(unique.hashCode());
    uuid = Integer.toHexString(from.hashCode());
    derivedField = Integer.toHexString(uuid.hashCode());
    otherThing = new Subclass(Integer.toHexString(derivedField.hashCode()));
    floatValue = (float) unique.hashCode() / (float) from.hashCode();
    doubleValue = (double) uuid.hashCode() / (double) derivedField.hashCode();
    arrayOfThings = new Object[]{
        this.unique, from, uuid, derivedField, otherThing, floatValue, doubleValue
    };

    String curThing = unique;
    for (int i = 0; i < 100; i++) {
      String key = Integer.toHexString(curThing.hashCode());
      String value = Integer.toHexString(key.hashCode());
      curThing = value;
      mapOfThings.put(key, value);
    }
  }

  @Override
  public String getUriFrom() {
    return from;
  }

  @Override
  public void setUriFrom(String uriFrom) {
    this.from = uriFrom;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TestMessage)) return false;
    TestMessage that = (TestMessage) o;
    return Objects.equals(unique, that.unique) &&
        Objects.equals(from, that.from) &&
        Objects.equals(getUuid(), that.getUuid()) &&
        Objects.equals(derivedField, that.derivedField) &&
        Objects.equals(floatValue, that.floatValue) &&
        Objects.equals(doubleValue, that.doubleValue) &&
        Arrays.equals(arrayOfThings, that.arrayOfThings) &&
        Objects.equals(mapOfThings, that.mapOfThings);
  }

  public String getUnique() {
    return unique;
  }

  public void setUnique(String unique) {
    this.unique = unique;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getDerivedField() {
    return derivedField;
  }

  public void setDerivedField(String derivedField) {
    this.derivedField = derivedField;
  }

  public Subclass getOtherThing() {
    return otherThing;
  }

  public void setOtherThing(Subclass otherThing) {
    this.otherThing = otherThing;
  }

  public float getFloatValue() {
    return floatValue;
  }

  public void setFloatValue(float floatValue) {
    this.floatValue = floatValue;
  }

  public double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(double doubleValue) {
    this.doubleValue = doubleValue;
  }

  public Object[] getArrayOfThings() {
    return arrayOfThings;
  }

  public void setArrayOfThings(Object[] arrayOfThings) {
    this.arrayOfThings = arrayOfThings;
  }

  public Map<String, String> getMapOfThings() {
    return mapOfThings;
  }

  public void setMapOfThings(Map<String, String> mapOfThings) {
    this.mapOfThings = mapOfThings;
  }

  @Override
  public int hashCode() {
    return Objects.hash(unique, getUriFrom(), getUuid(), derivedField, floatValue, doubleValue, arrayOfThings, mapOfThings);
  }

  static class Subclass {
    private String thing;

    public Subclass() {
    }

    public Subclass(String thing) {
      this.thing = thing;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Subclass)) return false;
      Subclass subclass = (Subclass) o;
      return Objects.equals(thing, subclass.thing);
    }

    @Override
    public int hashCode() {
      return Objects.hash(thing);
    }

    public String getThing() {
      return thing;
    }
  }
}