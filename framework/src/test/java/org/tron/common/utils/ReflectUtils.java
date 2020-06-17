package org.tron.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.springframework.util.ReflectionUtils;

public class ReflectUtils {

  public static Object getFieldObject(Object target, String fieldName) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    ReflectionUtils.makeAccessible(field);
    return ReflectionUtils.getField(field, target);
  }

  public static <T> T getFieldValue(Object target, String fieldName) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    ReflectionUtils.makeAccessible(field);
    return (T) ReflectionUtils.getField(field, target);
  }

  public static void setFieldValue(Object target, String fieldName, Object value) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    ReflectionUtils.makeAccessible(field);
    ReflectionUtils.setField(field, target, value);
  }

  public static <T> T invokeMethod(Object target, String methodName) {
    Method method = ReflectionUtils.findMethod(target.getClass(), methodName);
    ReflectionUtils.makeAccessible(method);
    return (T) ReflectionUtils.invokeMethod(method, target);
  }

  public static void invokeMethod(Object target, String methodName, Class[] param, Object... args) {
    Method method = ReflectionUtils.findMethod(target.getClass(), methodName, param);
    ReflectionUtils.makeAccessible(method);
    ReflectionUtils.invokeMethod(method, target, args);
  }

}
