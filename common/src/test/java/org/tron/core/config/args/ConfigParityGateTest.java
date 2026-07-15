package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Build-time gate that pins every (section, bean) tuple in {@link #SECTIONS}
 * so the entire reference.conf &lt;-&gt; {@code *Config} contract is managed in
 * one place. Drift fails the build at PR time instead of waiting for
 * {@code ConfigBeanFactory} to throw at process startup.
 * <p>
 * Per-section {@code *ConfigTest} files cover behavioural tests (defaults,
 * clamps, alias fallbacks); they do not own parity. Adding a new
 * {@code *Config} bean: add a {@link Section} entry below. Adding an
 * allowlist entry: include an inline rationale comment; new keys are
 * expected to bind 1:1 via {@code ConfigBeanFactory} without exception.
 */
public class ConfigParityGateTest {

  private static final class Section {
    final String path;
    final Class<?> bean;
    final Set<String> hoconOrphans;
    final Set<String> beanOrphans;
    final Set<String> divergent;

    Section(String path, Class<?> bean,
            Set<String> hoconOrphans, Set<String> beanOrphans,
            Set<String> divergent) {
      this.path = path;
      this.bean = bean;
      this.hoconOrphans = hoconOrphans;
      this.beanOrphans = beanOrphans;
      this.divergent = divergent;
    }
  }

  // legacy acronym casing; normalizeNonStandardKeys renames PBFT -> Pbft before bind
  private static final Set<String> COMMITTEE_HOCON_ORPHANS =
      ConfigParityCheck.allowlist(
          "allowPBFT",
          "pBFTExpireNum"
      );

  private static final Set<String> COMMITTEE_BEAN_ORPHANS =
      ConfigParityCheck.allowlist(
          "allowPbft",     // bound from HOCON allowPBFT via normalize hook
          "pbftExpireNum"  // bound from HOCON pBFTExpireNum via normalize hook
      );

  // native: Java reserved word; bound to bean field nativeQueue, read manually after bind.
  // topics: list items have optional fields; EventConfig binds the list manually with
  //   TOPIC_DEFAULTS fallback (field uses @Setter(NONE)).
  private static final Set<String> EVENT_HOCON_ORPHANS =
      ConfigParityCheck.allowlist(
          "native",
          "topics"
      );

  // FilterConfig: reference.conf ships [""] as a schema placeholder so operators see
  // the expected element type; bean default is [] (genuinely empty). Both mean "no filter".
  private static final Set<String> EVENT_DIVERGENT_DEFAULTS =
      ConfigParityCheck.allowlist(
          "filter.contractAddress",  // bean=[] vs reference.conf=[""] schema placeholder
          "filter.contractTopic"     // bean=[] vs reference.conf=[""] schema placeholder
      );

  // Genesis fields are mainnet seed data with no sensible in-Java default.
  private static final Set<String> GENESIS_DIVERGENT_DEFAULTS =
      ConfigParityCheck.allowlist(
          "timestamp",   // mainnet genesis timestamp, no in-Java default
          "parentHash",  // mainnet genesis parentHash, no in-Java default
          "assets",      // seed accounts (Zion / Sun / Blackhole); bean ships empty list
          "witnesses"    // 27 standby witness nodes; bean ships empty list
      );

  // properties: List<PropertyConfig> parsed manually via StorageConfig.readProperties();
  // ConfigBeanFactory cannot bind list-of-object fields, so the gate sees it as unbound.
  private static final Set<String> STORAGE_HOCON_ORPHANS =
      ConfigParityCheck.allowlist(
          "properties"  // manually parsed by StorageConfig.readProperties()
      );

  private static final Set<String> NODE_HOCON_ORPHANS =
      ConfigParityCheck.allowlist(
          "isOpenFullTcpDisconnect",  // normalized to bean field openFullTcpDisconnect
          "metrics"                    // delegated to MetricsConfig.fromConfig
      );

  private static final Set<String> NODE_BEAN_ORPHANS =
      ConfigParityCheck.allowlist(
          "openFullTcpDisconnect"      // HOCON ships isOpenFullTcpDisconnect; renamed
      );

  private static final Set<String> NODE_DIVERGENT_DEFAULTS =
      ConfigParityCheck.allowlist(
          "fastForward"  // seed node list, no Java-side default
      );

  // Top-level meta-gate: every reference.conf top-level key must be covered by a
  // Section entry above or listed here with a rationale. Closes the "new section
  // sneaks in" hole. See everyReferenceConfTopLevelKeyIsCovered.
  private static final Set<String> TOP_LEVEL_NON_BEAN =
      ConfigParityCheck.allowlist(
          "crypto",       // MiscConfig.cryptoEngine manual-read root
          "enery",        // MiscConfig manual-read root (preserves historical typo of "energy")
          "localwitness", // bound by LocalWitnessConfig, not in the *ConfigBean factory pattern
          "net",          // deprecated wrapper for net.type; intentionally empty in reference.conf
          "seed",         // MiscConfig.seedNodeIpList manual-read root (seed.node.ip.list)
          "trx"           // MiscConfig.trxReferenceBlock manual-read root (trx.reference.block)
      );

  private static final List<Section> SECTIONS;

  static {
    Set<String> empty = Collections.emptySet();
    List<Section> s = new ArrayList<>();
    // ctor args: (path, beanClass, hoconOrphans, beanOrphans, divergent)
    s.add(new Section("block", BlockConfig.class,
        empty, empty, empty));
    s.add(new Section("committee", CommitteeConfig.class,
        COMMITTEE_HOCON_ORPHANS, COMMITTEE_BEAN_ORPHANS, empty));
    s.add(new Section("event.subscribe", EventConfig.class,
        EVENT_HOCON_ORPHANS, empty, EVENT_DIVERGENT_DEFAULTS));
    s.add(new Section("genesis.block", GenesisConfig.class,
        empty, empty, GENESIS_DIVERGENT_DEFAULTS));
    s.add(new Section("node", NodeConfig.class,
        NODE_HOCON_ORPHANS, NODE_BEAN_ORPHANS, NODE_DIVERGENT_DEFAULTS));
    s.add(new Section("node.metrics", MetricsConfig.class,
        empty, empty, empty));
    s.add(new Section("rate.limiter", RateLimiterConfig.class,
        empty, empty, empty));
    s.add(new Section("storage", StorageConfig.class,
        STORAGE_HOCON_ORPHANS, empty, empty));
    s.add(new Section("vm", VmConfig.class,
        empty, empty, empty));
    SECTIONS = Collections.unmodifiableList(s);
  }

  @BeforeClass
  public static void resetAggregates() {
    ConfigParityCheck.resetAggregates();
  }

  /** Emit cross-section [parity-summary] totals + file-coverage alignment. */
  @AfterClass
  public static void logAggregateSummary() {
    Set<String> checkSectionTopLevels = new TreeSet<>();
    for (Section s : SECTIONS) {
      checkSectionTopLevels.add(s.path.split("\\.", 2)[0]);
    }
    ConfigParityCheck.logAggregateSummary(
        checkSectionTopLevels, TOP_LEVEL_NON_BEAN);
  }

  @Test
  public void hoconKeysAreBound() {
    for (Section s : SECTIONS) {
      ConfigParityCheck.assertNoHoconOrphans(s.path, s.bean, s.hoconOrphans);
    }
  }

  @Test
  public void beanPropertiesHaveHoconKeys() {
    for (Section s : SECTIONS) {
      ConfigParityCheck.assertNoBeanOrphans(s.path, s.bean, s.beanOrphans);
    }
  }

  @Test
  public void defaultValuesMatch() {
    List<String> failures = new ArrayList<>();
    for (Section s : SECTIONS) {
      try {
        ConfigParityCheck.assertDefaultValuesMatch(
            s.path, s.bean, s.divergent);
      } catch (AssertionError e) {
        failures.add(e.getMessage());
      }
    }
    if (!failures.isEmpty()) {
      throw new AssertionError(
          failures.size() + " section(s) failed default-value parity:\n\n"
              + String.join("\n\n", failures));
    }
  }

  /**
   * Fails when any allowlist entry no longer resolves to a live HOCON path or
   * bean property — i.e. the underlying key/property was renamed or removed
   * but the grandfathering entry was left behind.
   */
  @Test
  public void allowlistEntriesAreLive() {
    List<String> failures = new ArrayList<>();
    for (Section s : SECTIONS) {
      try {
        ConfigParityCheck.assertAllowlistEntriesAreLive(
            s.path, s.bean, s.hoconOrphans, s.beanOrphans, s.divergent);
      } catch (AssertionError e) {
        failures.add(e.getMessage());
      }
    }
    if (!failures.isEmpty()) {
      throw new AssertionError(
          failures.size() + " section(s) have dead allowlist entries:\n\n"
              + String.join("\n\n", failures));
    }
  }

  /**
   * Fails when reference.conf grows a top-level key not covered by a Section
   * or {@link #TOP_LEVEL_NON_BEAN}. Uses {@code parseResources} so JVM system
   * properties don't pollute the top-level key set.
   */
  @Test
  public void everyReferenceConfTopLevelKeyIsCovered() {
    Config refFile = ConfigFactory.parseResources("reference.conf");
    Set<String> topKeys = new TreeSet<>(refFile.root().keySet());
    Set<String> covered = new TreeSet<>();
    for (Section s : SECTIONS) {
      covered.add(s.path.split("\\.", 2)[0]);
    }
    covered.addAll(TOP_LEVEL_NON_BEAN);

    Set<String> orphans = new TreeSet<>(topKeys);
    orphans.removeAll(covered);
    if (!orphans.isEmpty()) {
      throw new AssertionError(
          "reference.conf has top-level keys not covered by SECTIONS and not in "
              + "TOP_LEVEL_NON_BEAN: " + orphans
              + ". Either add a new Section entry (preferred — auto-binds via "
              + "*Config bean) or register the key under TOP_LEVEL_NON_BEAN with "
              + "an inline rationale.");
    }
  }
}
