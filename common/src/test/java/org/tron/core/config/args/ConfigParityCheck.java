package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared helpers for reference.conf &lt;-&gt; {@code *Config} bean parity tests.
 * Asserts that every HOCON key under a section binds to a writable bean
 * property and matches the bean's default. Drift fails the build at PR time
 * instead of waiting for {@code ConfigBeanFactory} to throw at startup.
 * <p>
 * {@code [parity-*]} audit log lines land in the JUnit XML {@code <system-out>}
 * when the gate runs in isolation; if mixed with tests that boot a tron main,
 * production logback may redirect them to {@code logs/} on disk.
 */
@Slf4j(topic = "test")
final class ConfigParityCheck {

  private ConfigParityCheck() {

  }

  private static Map<String, PropertyDescriptor> writablePropertyDescriptors(Class<?> beanClass) {
    try {
      Map<String, PropertyDescriptor> m = new TreeMap<>();
      for (PropertyDescriptor pd :
          Introspector.getBeanInfo(beanClass, Object.class).getPropertyDescriptors()) {
        if (pd.getWriteMethod() != null) {
          m.put(pd.getName(), pd);
        }
      }
      return m;
    } catch (java.beans.IntrospectionException e) {
      throw new AssertionError("Introspector failed on " + beanClass.getName(), e);
    }
  }

  /**
   * {@code shapeMismatches}: HOCON key matches a bean property of nested
   * {@code *Config} type but the HOCON value is not OBJECT — walker cannot
   * recurse, and downstream binding would throw {@code WrongType}.
   */
  private static final class OrphanCounters {
    int total;
    int bound;
    final Set<String> orphans = new TreeSet<>();
    final Set<String> allowlisted = new TreeSet<>();
    final Set<String> shapeMismatches = new TreeSet<>();
  }

  /**
   * Fails when reference.conf has keys under {@code sectionPath} (recursively
   * through nested {@code *Config} sub-sections) that the bean cannot bind.
   * Recurses into a sub-config when the property type satisfies
   * {@link #isRecursiveConfigBean} AND the HOCON value is an OBJECT.
   */
  static void assertNoHoconOrphans(
      String sectionPath, Class<?> beanClass, Set<String> allowedHoconOrphans) {
    Config section = ConfigFactory.defaultReference().getConfig(sectionPath);
    OrphanCounters c = walkAndLogHoconOrphans(
        sectionPath, section, beanClass, allowedHoconOrphans);
    AGGREGATES.hoconKey += c.total;
    AGGREGATES.hoconBound += c.bound;
    AGGREGATES.hoconAllowlisted += c.allowlisted.size();
    AGGREGATES.beans.add(beanClass);
    failOnHoconOrphans(sectionPath, beanClass, c);
  }

  /** Overload for meta-tests: walks the supplied Config directly, skips AGGREGATES. */
  static void assertNoHoconOrphans(
      String label, Config section, Class<?> beanClass,
      Set<String> allowedHoconOrphans) {
    OrphanCounters c = walkAndLogHoconOrphans(
        label, section, beanClass, allowedHoconOrphans);
    failOnHoconOrphans(label, beanClass, c);
  }

  private static OrphanCounters walkAndLogHoconOrphans(
      String label, Config section, Class<?> beanClass, Set<String> allowed) {
    OrphanCounters c = new OrphanCounters();
    walkHoconOrphans(beanClass, section, "", allowed, c);
    logger.info("[parity-hocon] {} -> {}: hoconKey={}, bound={}, allowlisted={}{}",
        label, beanClass.getSimpleName(), c.total, c.bound,
        c.allowlisted.size(), c.allowlisted.isEmpty() ? "" : " " + c.allowlisted);
    return c;
  }

  private static void failOnHoconOrphans(
      String label, Class<?> beanClass, OrphanCounters c) {
    if (!c.shapeMismatches.isEmpty()) {
      throw new AssertionError(
          "reference.conf has " + label + ".* keys whose HOCON value "
              + "shape does not match the bean property type (expected OBJECT "
              + "for nested *Config bean): " + c.shapeMismatches);
    }
    if (!c.orphans.isEmpty()) {
      throw new AssertionError(
          "reference.conf has " + label + ".* keys with no matching "
              + beanClass.getSimpleName() + " property (at any nesting level) "
              + "and not in allowlist: " + c.orphans);
    }
  }

  private static void walkHoconOrphans(
      Class<?> beanClass, Config section, String prefix,
      Set<String> allowed, OrphanCounters c) {
    Map<String, PropertyDescriptor> props = writablePropertyDescriptors(beanClass);
    for (String key : new TreeSet<>(section.root().keySet())) {
      c.total++;
      String qualified = prefix + key;
      PropertyDescriptor pd = props.get(key);
      if (pd == null) {
        if (allowed.contains(qualified)) {
          c.allowlisted.add(qualified);
        } else {
          c.orphans.add(qualified);
        }
        continue;
      }
      c.bound++;
      Class<?> type = pd.getPropertyType();
      if (isRecursiveConfigBean(type)) {
        ConfigValueType valueType = section.root().get(key).valueType();
        if (valueType != ConfigValueType.OBJECT) {
          c.shapeMismatches.add(qualified + " (bean type "
              + type.getSimpleName() + " requires OBJECT, got " + valueType + ")");
          continue;
        }
        walkHoconOrphans(type, section.getConfig(key), qualified + ".", allowed, c);
      }
    }
  }

  /**
   * Fails when a writable bean property (reachable from {@code beanClass}
   * through nested {@code *Config} recursion) has no HOCON key under
   * {@code sectionPath} and is not in {@code allowedBeanOrphans}.
   */
  static void assertNoBeanOrphans(
      String sectionPath, Class<?> beanClass, Set<String> allowedBeanOrphans) {
    Config section = ConfigFactory.defaultReference().getConfig(sectionPath);
    OrphanCounters c = walkAndLogBeanOrphans(
        sectionPath, section, beanClass, allowedBeanOrphans);
    AGGREGATES.beanKey += c.total;
    AGGREGATES.beanHasKey += c.bound;
    AGGREGATES.beanAllowlisted += c.allowlisted.size();
    AGGREGATES.beans.add(beanClass);
    failOnBeanOrphans(sectionPath, beanClass, c);
  }

  /** Overload for meta-tests: walks the supplied Config directly, skips AGGREGATES. */
  static void assertNoBeanOrphans(
      String label, Config section, Class<?> beanClass,
      Set<String> allowedBeanOrphans) {
    OrphanCounters c = walkAndLogBeanOrphans(
        label, section, beanClass, allowedBeanOrphans);
    failOnBeanOrphans(label, beanClass, c);
  }

  private static OrphanCounters walkAndLogBeanOrphans(
      String label, Config section, Class<?> beanClass, Set<String> allowed) {
    OrphanCounters c = new OrphanCounters();
    walkBeanOrphans(beanClass, section, "", allowed, c);
    logger.info("[parity-bean]  {} -> {}: beanKey={}, hasKey={}, allowlisted={}{}",
        label, beanClass.getSimpleName(), c.total, c.bound,
        c.allowlisted.size(), c.allowlisted.isEmpty() ? "" : " " + c.allowlisted);
    return c;
  }

  private static void failOnBeanOrphans(
      String label, Class<?> beanClass, OrphanCounters c) {
    if (!c.shapeMismatches.isEmpty()) {
      throw new AssertionError(
          beanClass.getSimpleName() + " has nested *Config properties whose "
              + "HOCON value shape under " + label + ".* is not OBJECT: "
              + c.shapeMismatches);
    }
    if (!c.orphans.isEmpty()) {
      throw new AssertionError(
          beanClass.getSimpleName() + " has properties with no matching "
              + label + ".* HOCON key (at any nesting level) "
              + "and not in allowlist: " + c.orphans);
    }
  }

  private static void walkBeanOrphans(
      Class<?> beanClass, Config section, String prefix,
      Set<String> allowed, OrphanCounters c) {
    Set<String> keys = section.root().keySet();
    Map<String, PropertyDescriptor> props = writablePropertyDescriptors(beanClass);
    for (Map.Entry<String, PropertyDescriptor> e : props.entrySet()) {
      c.total++;
      String name = e.getKey();
      String qualified = prefix + name;
      PropertyDescriptor pd = e.getValue();
      if (!keys.contains(name)) {
        if (allowed.contains(qualified)) {
          c.allowlisted.add(qualified);
        } else {
          c.orphans.add(qualified);
        }
        continue;
      }
      c.bound++;
      Class<?> type = pd.getPropertyType();
      if (isRecursiveConfigBean(type)) {
        ConfigValueType valueType = section.root().get(name).valueType();
        if (valueType != ConfigValueType.OBJECT) {
          c.shapeMismatches.add(qualified + " (bean type "
              + type.getSimpleName() + " requires OBJECT, got " + valueType + ")");
          continue;
        }
        walkBeanOrphans(type, section.getConfig(name), qualified + ".", allowed, c);
      }
    }
  }

  /** Build an immutable allowlist from string literals. */
  static Set<String> allowlist(String... names) {
    Set<String> s = new HashSet<>(Arrays.asList(names));
    return Collections.unmodifiableSet(s);
  }

  /**
   * Fails when an allowlist entry no longer resolves to a live target. Prevents
   * allowlist rot: a renamed/removed key/property must drop its grandfathering
   * entry in the same PR (cf. Cassandra's PROPERTIES_TO_IGNORE long-term decay).
   */
  static void assertAllowlistEntriesAreLive(
      String sectionPath, Class<?> beanClass,
      Set<String> allowedHoconOrphans,
      Set<String> allowedBeanOrphans,
      Set<String> allowedDivergent) {
    Config section = ConfigFactory.defaultReference().getConfig(sectionPath);
    runAllowlistEntriesAreLive(sectionPath, section, beanClass,
        allowedHoconOrphans, allowedBeanOrphans, allowedDivergent);
  }

  /** Overload for meta-tests: see {@link #assertNoHoconOrphans(String, Config, Class, Set)}. */
  static void assertAllowlistEntriesAreLive(
      String label, Config section, Class<?> beanClass,
      Set<String> allowedHoconOrphans,
      Set<String> allowedBeanOrphans,
      Set<String> allowedDivergent) {
    runAllowlistEntriesAreLive(label, section, beanClass,
        allowedHoconOrphans, allowedBeanOrphans, allowedDivergent);
  }

  private static void runAllowlistEntriesAreLive(
      String label, Config section, Class<?> beanClass,
      Set<String> allowedHoconOrphans,
      Set<String> allowedBeanOrphans,
      Set<String> allowedDivergent) {
    List<String> dead = new ArrayList<>();

    for (String k : allowedHoconOrphans) {
      if (!section.hasPath(k)) {
        dead.add("hoconOrphan: " + k + " (no longer in reference.conf[" + label + "])");
      }
    }
    for (String k : allowedBeanOrphans) {
      if (!beanPropertyExists(beanClass, k)) {
        dead.add("beanOrphan: " + k + " (no longer a writable property of "
            + beanClass.getSimpleName() + ")");
      }
    }
    for (String k : allowedDivergent) {
      boolean hoconLive = section.hasPath(k);
      boolean beanLive = beanPropertyExists(beanClass, k);
      if (!hoconLive || !beanLive) {
        dead.add("divergent: " + k
            + " (hocon=" + (hoconLive ? "live" : "dead")
            + ", bean=" + (beanLive ? "live" : "dead") + ")");
      }
    }

    logger.info("[parity-sweep]  {} -> {}: hoconOrphans={}, beanOrphans={}, divergent={}, dead={}",
        label, beanClass.getSimpleName(),
        allowedHoconOrphans.size(), allowedBeanOrphans.size(),
        allowedDivergent.size(), dead.size());

    if (!dead.isEmpty()) {
      throw new AssertionError(
          "Dead allowlist entries on " + label + " / "
              + beanClass.getSimpleName() + " — drop them or restore the "
              + "underlying key/property:\n  " + String.join("\n  ", dead));
    }
  }

  /** True iff dotted {@code qualifiedName} resolves to a writable bean property. */
  private static boolean beanPropertyExists(Class<?> beanClass, String qualifiedName) {
    Class<?> cursor = beanClass;
    String[] segments = qualifiedName.split("\\.");
    for (int i = 0; i < segments.length; i++) {
      PropertyDescriptor pd = writablePropertyDescriptors(cursor).get(segments[i]);
      if (pd == null) {
        return false;
      }
      if (i == segments.length - 1) {
        return true;
      }
      Class<?> type = pd.getPropertyType();
      if (!isRecursiveConfigBean(type)) {
        return false;
      }
      cursor = type;
    }
    return true;
  }

  /** Sentinel: property type outside the dispatcher matrix — hard failure. */
  private static final Object SKIP = new Object();

  /** Sentinel: property type is a nested {@code *Config} bean to recurse into. */
  private static final Object RECURSE = new Object();

  /**
   * Asserts every writable bean property has a default value equal to its
   * reference.conf value. Supported scalar types: {@code int / long / boolean /
   * double / float} (and boxed forms), {@code String}, {@code List}. Nested
   * {@code *Config} beans are recursed into and matched by dotted name.
   * <p>
   * Skipped: properties with no HOCON key at the current scope, and properties
   * named in {@code allowedDivergent} (intentional asymmetry). Properties whose
   * type isn't in the dispatcher matrix fail — no silent escape; extend the
   * dispatcher or (if genuinely uncomparable) re-introduce a per-section
   * {@code typeSkip} allowlist.
   */
  static void assertDefaultValuesMatch(
      String sectionPath, Class<?> beanClass, Set<String> allowedDivergent) {
    Config section = ConfigFactory.defaultReference().getConfig(sectionPath);
    Counters c = new Counters();
    List<String> mismatches = runDefaultValuesMatch(
        sectionPath, section, beanClass, allowedDivergent, c);

    AGGREGATES.defBeanKey += c.total;
    AGGREGATES.defMatched += c.matched;
    AGGREGATES.defHoconRecursedKey += c.recursed;
    AGGREGATES.defSkipAllow += c.skipAllow.size();
    AGGREGATES.defSkipNoKey += c.skipNoKey.size();
    AGGREGATES.beans.add(beanClass);

    failOnDefaultValueMismatches(sectionPath, beanClass, mismatches);
  }

  /** Overload for meta-tests: see {@link #assertNoHoconOrphans(String, Config, Class, Set)}. */
  static void assertDefaultValuesMatch(
      String label, Config section, Class<?> beanClass,
      Set<String> allowedDivergent) {
    Counters c = new Counters();
    List<String> mismatches = runDefaultValuesMatch(
        label, section, beanClass, allowedDivergent, c);
    failOnDefaultValueMismatches(label, beanClass, mismatches);
  }

  private static List<String> runDefaultValuesMatch(
      String label, Config section, Class<?> beanClass,
      Set<String> allowedDivergent, Counters c) {
    Object bean;
    try {
      bean = beanClass.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("cannot instantiate " + beanClass.getName(), e);
    }
    List<String> mismatches = new ArrayList<>();
    compareBean(beanClass, bean, section, "", allowedDivergent, mismatches, c);
    logger.info("[parity-default] {} -> {}: beanKey={}, matched={}, hoconRecursedKey={}, "
            + "divergent-allow={}{}, skip-no-key={}{}",
        label, beanClass.getSimpleName(),
        c.total, c.matched, c.recursed,
        c.skipAllow.size(), c.skipAllow.isEmpty() ? "" : " " + c.skipAllow,
        c.skipNoKey.size(), c.skipNoKey.isEmpty() ? "" : " " + c.skipNoKey);
    return mismatches;
  }

  private static void failOnDefaultValueMismatches(
      String label, Class<?> beanClass, List<String> mismatches) {
    if (!mismatches.isEmpty()) {
      throw new AssertionError(
          "Default-value drift between " + beanClass.getSimpleName()
              + " and reference.conf[" + label + "]:\n  "
              + String.join("\n  ", mismatches));
    }
  }

  /**
   * Per-walk accounting. Invariant: {@code total == matched + recursed +
   * skipAllow.size() + skipNoKey.size() + mismatches.size()}. Adding a loop
   * exit without bumping a counter silently hides coverage drift.
   */
  private static final class Counters {
    int total;
    int matched;
    int recursed;
    final Set<String> skipAllow = new TreeSet<>();
    final Set<String> skipNoKey = new TreeSet<>();
  }

  private static void compareBean(
      Class<?> beanClass, Object beanDefault, Config section, String prefix,
      Set<String> allowedDivergent, List<String> mismatches, Counters c) {
    PropertyDescriptor[] pds;
    try {
      pds = Introspector.getBeanInfo(beanClass, Object.class).getPropertyDescriptors();
    } catch (java.beans.IntrospectionException e) {
      throw new AssertionError(e);
    }
    for (PropertyDescriptor pd : pds) {
      if (pd.getWriteMethod() == null) {
        // @Setter(NONE) for manual post-bind reads — orphan checks cover this side.
        continue;
      }
      c.total++;
      String name = pd.getName();
      String qualified = prefix + name;
      if (pd.getReadMethod() == null) {
        // Write-only property: ConfigBeanFactory binds but nothing reads it back.
        mismatches.add(qualified + ": bean property is write-only "
            + "(setter present, no getter) — default value cannot be verified "
            + "and the bound value cannot be observed; add a getter or drop the field");
        continue;
      }
      if (allowedDivergent.contains(qualified)) {
        c.skipAllow.add(qualified);
        continue;
      }
      if (!section.hasPath(name)) {
        c.skipNoKey.add(qualified);
        continue;
      }
      Class<?> type = pd.getPropertyType();
      // Shape guard: nested *Config bean expects HOCON OBJECT; surface as a
      // clean mismatch instead of letting getConfig(name) throw WrongType.
      if (isRecursiveConfigBean(type)) {
        ConfigValueType valueType = section.root().get(name).valueType();
        if (valueType != ConfigValueType.OBJECT) {
          mismatches.add(qualified + ": bean type " + type.getSimpleName()
              + " requires HOCON OBJECT, got " + valueType);
          continue;
        }
      }
      Object hoconValue;
      try {
        hoconValue = readTypedHoconValue(section, name, type);
      } catch (RuntimeException e) {
        mismatches.add(qualified + ": type-incompatible HOCON value ("
            + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        continue;
      }
      if (hoconValue == RECURSE) {
        c.recursed++;
        Object nested;
        try {
          nested = pd.getReadMethod().invoke(beanDefault);
        } catch (ReflectiveOperationException e) {
          throw new AssertionError(
              "cannot read " + qualified + " on " + beanClass.getName(), e);
        }
        if (nested == null) {
          mismatches.add(qualified + ": nested " + type.getSimpleName()
              + " field is null on a freshly-constructed " + beanClass.getSimpleName()
              + " — initialize the field inline (= new " + type.getSimpleName() + "())");
          continue;
        }
        compareBean(type, nested, section.getConfig(name), qualified + ".",
            allowedDivergent, mismatches, c);
        continue;
      }
      if (hoconValue == SKIP) {
        mismatches.add(qualified + ": Java type " + type.getSimpleName()
            + " not in readTypedHoconValue dispatcher — extend the dispatcher, "
            + "or re-introduce a per-section typeSkip allowlist if the type "
            + "genuinely cannot be value-compared");
        continue;
      }
      Object actualDefault;
      try {
        actualDefault = pd.getReadMethod().invoke(beanDefault);
      } catch (ReflectiveOperationException e) {
        throw new AssertionError(
            "cannot read " + qualified + " on " + beanClass.getName(), e);
      }
      if (!Objects.equals(actualDefault, hoconValue)) {
        // Stamp the runtime type on each side so e.g. Integer(10) vs Long(10)
        // doesn't look like `bean=10, reference.conf=10`.
        mismatches.add(qualified + ": bean=" + format(actualDefault)
            + " (" + typeOf(actualDefault) + ")"
            + ", reference.conf=" + format(hoconValue)
            + " (" + typeOf(hoconValue) + ")");
        continue;
      }
      c.matched++;
    }
  }

  /** Type dispatcher. Returns {@link #RECURSE} for nested *Config, {@link #SKIP} otherwise. */
  private static Object readTypedHoconValue(Config cfg, String path, Class<?> type) {
    if (type == int.class || type == Integer.class) {
      return cfg.getInt(path);
    }
    if (type == long.class || type == Long.class) {
      return cfg.getLong(path);
    }
    if (type == boolean.class || type == Boolean.class) {
      return cfg.getBoolean(path);
    }
    if (type == double.class || type == Double.class) {
      return cfg.getDouble(path);
    }
    if (type == float.class || type == Float.class) {
      return (float) cfg.getDouble(path);
    }
    if (type == String.class) {
      return cfg.getString(path);
    }
    if (type == List.class) {
      return cfg.getList(path).unwrapped();
    }
    if (isRecursiveConfigBean(type) && cfg.hasPath(path)) {
      return RECURSE;
    }
    return SKIP;
  }

  /**
   * Recursion gate: a non-array/enum/interface class under {@code org.tron.*}
   * with a default constructor. Keeps the walker inside project-owned beans.
   */
  private static boolean isRecursiveConfigBean(Class<?> type) {
    if (type.isPrimitive() || type.isArray() || type.isEnum() || type.isInterface()) {
      return false;
    }
    if (!type.getName().startsWith("org.tron.")) {
      return false;
    }
    try {
      type.getDeclaredConstructor();
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  /**
   * Cross-section accumulators. Bumped by each helper at the end of its work
   * (before throwing) so partial coverage is still reflected.
   * {@link #logAggregateSummary} emits one summary line per gate plus
   * independently-computed reference totals as a sanity check.
   */
  private static final class Aggregates {
    int hoconKey;
    int hoconBound;
    int hoconAllowlisted;
    int beanKey;
    int beanHasKey;
    int beanAllowlisted;
    int defBeanKey;
    int defMatched;
    int defHoconRecursedKey;
    int defSkipAllow;
    int defSkipNoKey;
    // root-level bean classes touched by any helper; recursion walks nested *Config on its own.
    final Set<Class<?>> beans = new LinkedHashSet<>();
  }

  private static final Aggregates AGGREGATES = new Aggregates();

  /** Reset accumulators. Call from {@code @BeforeClass} for clean re-runs in the same JVM. */
  static void resetAggregates() {
    AGGREGATES.hoconKey = 0;
    AGGREGATES.hoconBound = 0;
    AGGREGATES.hoconAllowlisted = 0;
    AGGREGATES.beanKey = 0;
    AGGREGATES.beanHasKey = 0;
    AGGREGATES.beanAllowlisted = 0;
    AGGREGATES.defBeanKey = 0;
    AGGREGATES.defMatched = 0;
    AGGREGATES.defHoconRecursedKey = 0;
    AGGREGATES.defSkipAllow = 0;
    AGGREGATES.defSkipNoKey = 0;
    AGGREGATES.beans.clear();
  }

  /**
   * Emit per-gate totals + file-coverage alignment
   * {@code file-hoconKey == checkSection + cantCheckSection} and bean-tree
   * alignment across {@code parity-bean} / {@code parity-default} / the
   * independently-counted registry total. Reviewers can sum columns visually
   * to spot a walker that silently skipped a property.
   *
   * @param checkSectionTopLevels      top-level keys hosting a registered Section
   * @param cantCheckSectionTopLevels  remaining top-level keys (out of parity scope)
   */
  static void logAggregateSummary(
      Set<String> checkSectionTopLevels,
      Set<String> cantCheckSectionTopLevels) {
    ConfigObject refRoot = ConfigFactory.parseResources("reference.conf").root();
    int hoconKeyInFile = countHoconKeysRecursive(refRoot);
    int checkSectionKey = sumTopLevelSubtreeSize(refRoot, checkSectionTopLevels);
    int cantCheckSectionKey = sumTopLevelSubtreeSize(refRoot, cantCheckSectionTopLevels);

    int beanKeyInRegistry = 0;
    for (Class<?> b : AGGREGATES.beans) {
      beanKeyInRegistry += countBeanSettersRecursive(b);
    }

    logger.info("[parity-summary] parity-hocon  : hoconKey={}, bound={}, allowlisted={}",
        AGGREGATES.hoconKey, AGGREGATES.hoconBound, AGGREGATES.hoconAllowlisted);
    logger.info("[parity-summary] parity-bean   : beanKey={}, hasKey={}, allowlisted={}",
        AGGREGATES.beanKey, AGGREGATES.beanHasKey, AGGREGATES.beanAllowlisted);
    logger.info("[parity-summary] parity-default: beanKey={}, matched={}, hoconRecursedKey={}, "
            + "divergent-allow={}, skip-no-key={}",
        AGGREGATES.defBeanKey, AGGREGATES.defMatched, AGGREGATES.defHoconRecursedKey,
        AGGREGATES.defSkipAllow, AGGREGATES.defSkipNoKey);
    logger.info("[parity-summary] checkSection     {} top-levels {}: hoconKey={} "
            + "(= parity-hocon-walked({}) + path-segments-and-internal({}))",
        checkSectionTopLevels.size(), checkSectionTopLevels, checkSectionKey,
        AGGREGATES.hoconKey, checkSectionKey - AGGREGATES.hoconKey);
    logger.info("[parity-summary] cantCheckSection {} top-levels {}: hoconKey={} "
            + "(validation skipped; not in checkSection scope)",
        cantCheckSectionTopLevels.size(), cantCheckSectionTopLevels,
        cantCheckSectionKey);
    logger.info("[parity-summary] hocon-align   : file-hoconKey({}) = "
            + "checkSection({}) + cantCheckSection({})",
        hoconKeyInFile, checkSectionKey, cantCheckSectionKey);
    logger.info("[parity-summary] bean-align    : registry-beanKey({}, across {} bean classes) "
            + "= parity-bean({}) = parity-default({})",
        beanKeyInRegistry, AGGREGATES.beans.size(),
        AGGREGATES.beanKey, AGGREGATES.defBeanKey);
  }

  private static int sumTopLevelSubtreeSize(ConfigObject refRoot, Set<String> topLevelKeys) {
    int n = 0;
    for (String k : topLevelKeys) {
      if (!refRoot.containsKey(k)) {
        continue;
      }
      n++;
      ConfigValue v = refRoot.get(k);
      if (v.valueType() == ConfigValueType.OBJECT) {
        n += countHoconKeysRecursive((ConfigObject) v);
      }
    }
    return n;
  }

  private static int countHoconKeysRecursive(ConfigObject obj) {
    int n = 0;
    for (String k : obj.keySet()) {
      n++;
      ConfigValue v = obj.get(k);
      if (v.valueType() == ConfigValueType.OBJECT) {
        n += countHoconKeysRecursive((ConfigObject) v);
      }
    }
    return n;
  }

  private static int countBeanSettersRecursive(Class<?> beanClass) {
    int n = 0;
    for (PropertyDescriptor pd : writablePropertyDescriptors(beanClass).values()) {
      n++;
      Class<?> t = pd.getPropertyType();
      if (isRecursiveConfigBean(t)) {
        n += countBeanSettersRecursive(t);
      }
    }
    return n;
  }

  private static String typeOf(Object o) {
    return o == null ? "null" : o.getClass().getSimpleName();
  }

  private static String format(Object o) {
    if (o == null) {
      return "null";
    }
    if (o instanceof String) {
      return "\"" + o + "\"";
    }
    if (o instanceof List) {
      List<?> list = (List<?>) o;
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < list.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(format(list.get(i)));
      }
      sb.append("]");
      return sb.toString();
    }
    return String.valueOf(o);
  }
}
