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
package org.apache.gossip.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {

  /**
   * Create an instance of a thing. This method essentially makes code more readable by handing the various exception
   * trapping.
   * @param className
   * @param constructorTypes
   * @param constructorArgs
   * @param <T>
   * @return constructed instance of a thing.
   */
  @SuppressWarnings("unchecked")
  public static <T> T constructWithReflection(String className, Class<?>[] constructorTypes, Object[] constructorArgs) {
    try {
      Constructor<?> c = Class.forName(className).getConstructor(constructorTypes);
      c.setAccessible(true);
      return (T) c.newInstance(constructorArgs);
    } catch (InvocationTargetException e) {
      // catch ITE and throw the target if it is a RTE.
      if (e.getTargetException() != null && RuntimeException.class.isAssignableFrom(e.getTargetException().getClass())) {
        throw (RuntimeException) e.getTargetException();
      } else {
        throw new RuntimeException(e);
      }
    } catch (ReflectiveOperationException others) {
      // Note: No class in the above list should be a descendent of RuntimeException. Otherwise, we're just wrapping
      //       and making stack traces confusing.
      throw new RuntimeException(others);
    }
  }
}
