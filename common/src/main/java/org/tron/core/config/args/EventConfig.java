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
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private NativeConfig nativeQueue = new NativeConfig();

  public NativeConfig getNativeQueue() { return nativeQueue; }
  // Topics list has optional fields (ethCompatible, redundancy, solidified) that
  // not all items have. ConfigBeanFactory requires all bean fields to exist in config.
  // Excluded from auto-binding, read manually in fromConfig().
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private List<TopicConfig> topics = new ArrayList<>();

  public List<TopicConfig> getTopics() { return topics; }
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

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  /**
   * Create EventConfig from the "event.subscribe" section of the application config.
   *
   * <p>Note: HOCON key "native" is a Java reserved word, so the bean field is named
   * "nativeQueue" but config key is "native". We handle this manually after binding.
   */
  public static EventConfig fromConfig(Config config) {
    Config section = config.getConfig("event.subscribe");

    // "native" is a Java reserved word, "topics" has optional fields per item —
    // strip both before binding, read manually
    String nativeKey = "native";
    String topicsKey = "topics";
    Config bindable = section.withoutPath(nativeKey).withoutPath(topicsKey)
        .withoutPath("topicDefaults");
    EventConfig ec = ConfigBeanFactory.create(bindable, EventConfig.class);

    // manually bind "native" sub-section
    Config nativeSection = section.hasPath(nativeKey)
        ? section.getConfig(nativeKey) : ConfigFactory.empty();
    ec.nativeQueue = new NativeConfig();
    if (nativeSection.hasPath("useNativeQueue")) {
      ec.nativeQueue.useNativeQueue = nativeSection.getBoolean("useNativeQueue");
    }
    if (nativeSection.hasPath("bindport")) {
      ec.nativeQueue.bindport = nativeSection.getInt("bindport");
    }
    if (nativeSection.hasPath("sendqueuelength")) {
      ec.nativeQueue.sendqueuelength = nativeSection.getInt("sendqueuelength");
    }

    // manually bind topics — each item may have optional fields
    if (section.hasPath(topicsKey)) {
      ec.topics = new ArrayList<>();
      for (com.typesafe.config.ConfigObject obj : section.getObjectList(topicsKey)) {
        Config tc = obj.toConfig();
        TopicConfig topic = new TopicConfig();
        if (tc.hasPath("triggerName")) {
          topic.triggerName = tc.getString("triggerName");
        }
        if (tc.hasPath("enable")) {
          topic.enable = tc.getBoolean("enable");
        }
        if (tc.hasPath("topic")) {
          topic.topic = tc.getString("topic");
        }
        if (tc.hasPath("solidified")) {
          topic.solidified = tc.getBoolean("solidified");
        }
        if (tc.hasPath("ethCompatible")) {
          topic.ethCompatible = tc.getBoolean("ethCompatible");
        }
        if (tc.hasPath("redundancy")) {
          topic.redundancy = tc.getBoolean("redundancy");
        }
        ec.topics.add(topic);
      }
    }

    return ec;
  }
}
