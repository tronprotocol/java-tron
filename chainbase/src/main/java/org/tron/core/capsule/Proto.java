package org.tron.core.capsule;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.tron.core.exception.BadItemException;

public class Proto {

 private static final Map<Class<? extends ProtoCapsule>, TypeToken> tokenCache =  Maps.newConcurrentMap();


  public  static <T extends ProtoCapsule> T of(byte[] value, Class<T> clazz)
      throws BadItemException {
    if (value == null) {
      return null;
    }
    tokenCache.putIfAbsent(clazz, TypeToken.of(clazz));
    try {
      Constructor constructor = tokenCache.getOrDefault(
          clazz,TypeToken.of(clazz) ).getRawType().getConstructor(byte[].class);
      return (T) constructor.newInstance(value);
    } catch (NoSuchMethodException | IllegalAccessException
        | InstantiationException | InvocationTargetException e) {
      throw new BadItemException(e.getMessage());
    }
  }

}
