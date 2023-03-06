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
package org.apache.tuweni.bytes;



/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 * <p>
 * This class may be used to create more types that represent 48 bytes, but need a different name for business logic.
 */
public class DelegatingBytes48 extends DelegatingBytes implements Bytes48 {

  protected DelegatingBytes48(Bytes delegate) {
    super(delegate);
  }

  @Override
  public int size() {
    return Bytes48.SIZE;
  }

  @Override
  public Bytes48 copy() {
    return Bytes48.wrap(toArray());
  }

  @Override
  public MutableBytes48 mutableCopy() {
    return MutableBytes48.wrap(toArray());
  }

}
