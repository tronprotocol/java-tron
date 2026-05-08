package org.tron.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a Typesafe {@link Config} from a bean instance's current field values.
 *
 * <p>Used by each {@code XxxConfig.fromConfig()} to replace the role that
 * {@code reference.conf} played: ensures every key ConfigBeanFactory needs is
 * present, so a partial user config works without throwing
 * {@code ConfigException.Missing}.
 *
 * <p>Only public getter+setter pairs (standard JavaBean properties) are included —
 * the same set that {@code ConfigBeanFactory.create()} auto-binds. Keys are
 * decapitalized exactly as ConfigBeanFactory does:
 * {@code Character.toLowerCase(name.charAt(0)) + name.substring(1)}.
 *
 * <p>Nested bean fields are recursed into nested HOCON objects.
 * {@code List} fields are serialized as HOCON lists (empty by default).
 * Fields with no public setter (e.g. {@code @Getter(AccessLevel.NONE)} overrides)
 * are automatically skipped — these are handled manually in each
 * {@code fromConfig()} via {@code hasPath} guards.
 */
public final class BeanDefaults {

  private BeanDefaults() {}

  /**
   * Convert {@code bean}'s public JavaBean properties to a Typesafe Config.
   * The resulting Config can be used as a {@code withFallback()} for a user's
   * config section to guarantee all keys are present for ConfigBeanFactory.
   */
  public static Config toConfig(Object bean) {
    return ConfigFactory.parseMap(toMap(bean));
  }

  /**
   * Returns a copy of {@code config} with all null-valued leaf paths removed.
   * Call this on a user-supplied config section before {@link Config#withFallback}
   * so that HOCON {@code null} entries in legacy configs do not shadow bean defaults.
   *
   * <p>Uses {@link ConfigObject#entrySet()} (not {@link Config#entrySet()}) because
   * the latter silently excludes null values, making them impossible to detect.
   */
  public static Config stripNullLeaves(Config config) {
    return stripNullObject(config.root()).toConfig();
  }

  /**
   * Returns a copy of {@code config} where the value at {@code fromKey} is moved to
   * {@code toKey}, leaving the original key absent. If {@code fromKey} is absent, the
   * config is returned unchanged. Use this in {@code fromConfig()} to bridge config keys
   * that violate JavaBean naming (e.g. {@code pBFTExpireNum} → {@code PBFTExpireNum}) so
   * that {@code ConfigBeanFactory} finds the value under the key it derives from the setter.
   */
  public static Config remapKey(Config config, String fromKey, String toKey) {
    if (!config.hasPath(fromKey)) {
      return config;
    }
    return config.withValue(toKey, config.getValue(fromKey)).withoutPath(fromKey);
  }

  private static ConfigObject stripNullObject(ConfigObject obj) {
    ConfigObject result = obj;
    for (Map.Entry<String, ConfigValue> entry : obj.entrySet()) {
      ConfigValue v = entry.getValue();
      if (v.valueType() == ConfigValueType.NULL) {
        result = result.withoutKey(entry.getKey());
      } else if (v.valueType() == ConfigValueType.OBJECT) {
        result = result.withValue(entry.getKey(), stripNullObject((ConfigObject) v));
      }
    }
    return result;
  }

  private static Map<String, Object> toMap(Object bean) {
    Map<String, Object> map = new LinkedHashMap<>();
    BeanInfo info;
    try {
      info = Introspector.getBeanInfo(bean.getClass());
    } catch (java.beans.IntrospectionException e) {
      // Programming error: bean class does not conform to JavaBean spec.
      // Propagate immediately so the misconfigured class is identified at startup,
      // rather than returning a silent empty map that produces a confusing
      // ConfigException.Missing pointing at the user config.
      throw new IllegalStateException("Cannot introspect bean: " + bean.getClass().getName(), e);
    }
    for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
      Method getter = pd.getReadMethod();
      Method setter = pd.getWriteMethod();
      // Skip read-only properties (no setter) — matches ConfigBeanFactory's contract
      if (getter == null || setter == null) {
        continue;
      }
      // Use the property name exactly as Introspector produced it.
      // ConfigBeanFactory does configProps.get(beanProp.getName()) — the lookup key
      // is the property name verbatim, not decapitalized.  For ordinary camelCase
      // setters (setMaxConnections → "MaxConnections" → decapitalize → "maxConnections")
      // Introspector already returns the lowercase form.  For setters that start with
      // two consecutive uppercase letters (setPBFTEnable → "PBFTEnable") the JavaBean
      // spec forbids decapitalization, so pd.getName() == "PBFTEnable" — matching the
      // capital-P key that config.conf uses for those fields.
      try {
        String key = pd.getName();
        Object value = getter.invoke(bean);
        map.put(key, toValue(value));
      } catch (Exception ignored) {
        // Best-effort: skip individual unresolvable property so that the rest of
        // the defaults are still emitted. getter.invoke() is the only realistic
        // throw site (InvocationTargetException / IllegalAccessException).
      }
    }
    return map;
  }

  private static Object toValue(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Boolean || value instanceof Number || value instanceof String) {
      return value;
    }
    if (value instanceof List) {
      List<Object> list = new ArrayList<>();
      for (Object item : (List<?>) value) {
        list.add(toValue(item));
      }
      return list;
    }
    // Assume nested bean — recurse so it becomes a nested HOCON object.
    return toMap(value);
  }
}
