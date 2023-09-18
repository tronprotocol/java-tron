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

import static org.apache.tuweni.bytes.Checks.checkArgument;


final class DelegatingMutableBytes32 extends DelegatingMutableBytes implements MutableBytes32 {

  private DelegatingMutableBytes32(MutableBytes delegate) {
    super(delegate);
  }

  static MutableBytes32 delegateTo(MutableBytes value) {
    checkArgument(value.size() == SIZE, "Expected %s bytes but got %s", SIZE, value.size());
    return new DelegatingMutableBytes32(value);
  }

  @Override
  public Bytes32 copy() {
    return Bytes32.wrap(delegate.toArray());
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return MutableBytes32.wrap(delegate.toArray());
  }

}
