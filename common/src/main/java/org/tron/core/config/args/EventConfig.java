package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.config.BeanDefaults;

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
  // Config key is "native" (Java reserved word); remapped to this field in fromConfig().
  private NativeConfig nativeQueue = new NativeConfig();

  // Topics list has optional fields (ethCompatible, redundancy, solidified) that
  // not all items have. ConfigBeanFactory requires all bean fields to exist in config.
  // Excluded from auto-binding, read manually in fromConfig().
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private List<TopicConfig> topics = new ArrayList<>();

  public List<TopicConfig> getTopics() {
    return topics;
  }

  private FilterConfig filter = new FilterConfig();

  @Getter
  @Setter
  public static class NativeConfig {

    private boolean useNativeQueue = true;
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

  /**
   * Create EventConfig from the "event.subscribe" section of the application config.
   *
   * <p>Note: HOCON key "native" is a Java reserved word, so the bean field is named
   * "nativeQueue" but config key is "native". We handle this manually after binding.
   */
  public static EventConfig fromConfig(Config config) {
    Config defaults = BeanDefaults.toConfig(new EventConfig());
    Config userSection = config.hasPath("event.subscribe")
        ? BeanDefaults.stripNullLeaves(config.getConfig("event.subscribe"))
        : ConfigFactory.empty();

    // "native" is a Java reserved word — remap to the field name so ConfigBeanFactory
    // auto-binds it as NativeConfig nativeQueue. topics has optional fields per item
    // so it is excluded from auto-binding and populated manually below.
    Config bindable = BeanDefaults.remapKey(userSection, "native", "nativeQueue")
        .withoutPath("topics")
        .withoutPath("topicDefaults")
        .withFallback(defaults);
    EventConfig ec = ConfigBeanFactory.create(bindable, EventConfig.class);

    // topics: apply per-item BeanDefaults so optional fields (solidified, ethCompatible,
    // redundancy) don't require every item to declare them explicitly.
    if (userSection.hasPath("topics")) {
      Config topicDefaults = BeanDefaults.toConfig(new TopicConfig());
      ec.topics = new ArrayList<>();
      for (com.typesafe.config.ConfigObject obj : userSection.getObjectList("topics")) {
        ec.topics.add(ConfigBeanFactory.create(
            BeanDefaults.stripNullLeaves(obj.toConfig()).withFallback(topicDefaults),
            TopicConfig.class));
      }
    }

    return ec;
  }
}
