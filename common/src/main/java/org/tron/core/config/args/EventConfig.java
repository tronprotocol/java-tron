package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Event subscribe configuration bean.
 * Field names match config.conf keys under "event.subscribe".
 */
@Slf4j
@Getter
@Setter
public class EventConfig {

  private boolean enable = false;
  private int version = 0;
  private long startSyncBlockNum = 0;
  private String path = "";
  private String server = "";
  private String dbconfig = "";
  private boolean contractParse = true;
  // "native" is a Java reserved word; config key cannot match field name directly.
  // @Setter(NONE) prevents ConfigBeanFactory from requiring a "nativeQueue" key.
  @Setter(lombok.AccessLevel.NONE)
  private NativeConfig nativeQueue = new NativeConfig();

  // Topics list items have optional fields; excluded from auto-binding.
  // @Setter(NONE) prevents ConfigBeanFactory from requiring a "topics" key.
  @Setter(lombok.AccessLevel.NONE)
  private List<TopicConfig> topics = new ArrayList<>();
  private FilterConfig filter = new FilterConfig();

  @Getter
  @Setter
  public static class NativeConfig {
    private boolean useNativeQueue = false;
    private int bindport = 5555;
    private int sendqueuelength = 1000;
  }

  @Getter
  @Setter
  public static class TopicConfig {
    private String triggerName = "";
    private boolean enable = false;
    private String topic = "";
    private boolean solidified = false;
    private boolean ethCompatible = false;
    private boolean redundancy = false;
  }

  @Getter
  @Setter
  public static class FilterConfig {
    private String fromblock = "";
    private String toblock = "";
    private List<String> contractAddress = new ArrayList<>();
    private List<String> contractTopic = new ArrayList<>();
  }

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  // TopicConfig fields are optional per item; this fallback ensures all keys exist
  // for ConfigBeanFactory binding. Values must match TopicConfig field defaults.
  private static final Config TOPIC_DEFAULTS = ConfigFactory.parseString(
      "triggerName=\"\", enable=false, topic=\"\", "
          + "solidified=false, ethCompatible=false, redundancy=false");

  /**
   * Create EventConfig from the "event.subscribe" section of the application config.
   *
   * <p>"native" is a Java reserved word, so the field is named "nativeQueue" and the
   * sub-section is read directly after binding. "topics" items may omit optional fields;
   * TOPIC_DEFAULTS provides fallback values so ConfigBeanFactory can bind each item.
   */
  public static EventConfig fromConfig(Config config) {
    Config section = config.getConfig("event.subscribe");

    String nativeKey = "native";
    String topicsKey = "topics";
    // remove two keys to construct EventConfig because they cannot be bind automatically,
    // we can bind them manually later
    Config bindable = section.withoutPath(nativeKey).withoutPath(topicsKey);
    EventConfig ec = ConfigBeanFactory.create(bindable, EventConfig.class);

    // "native" sub-section: bind via ConfigBeanFactory when present, use defaults otherwise
    ec.nativeQueue = section.hasPath(nativeKey)
        ? ConfigBeanFactory.create(section.getConfig(nativeKey), NativeConfig.class)
        : new NativeConfig();

    // topics: withFallback fills optional fields so ConfigBeanFactory can bind each item
    if (section.hasPath(topicsKey)) {
      ec.topics = new ArrayList<>();
      for (com.typesafe.config.ConfigObject obj : section.getObjectList(topicsKey)) {
        ec.topics.add(ConfigBeanFactory.create(
            obj.toConfig().withFallback(TOPIC_DEFAULTS), TopicConfig.class));
      }
    }

    return ec;
  }
}
