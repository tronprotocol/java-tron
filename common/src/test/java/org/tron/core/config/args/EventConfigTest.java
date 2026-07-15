package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class EventConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    EventConfig ec = EventConfig.fromConfig(empty);
    // reference.conf has event.subscribe with enable=false, topics with 7 entries
    assertFalse(ec.isEnable());
    assertEquals(0, ec.getVersion());
    assertEquals("", ec.getPath());
    assertFalse(ec.getTopics().isEmpty()); // reference.conf has default topic entries
  }

  @Test
  public void testNativeQueue() {
    Config config = withRef(
        "event.subscribe { enable = true,"
            + " native { useNativeQueue = true, bindport = 6666, sendqueuelength = 2000 } }");
    EventConfig ec = EventConfig.fromConfig(config);
    assertTrue(ec.isEnable());
    assertTrue(ec.getNativeQueue().isUseNativeQueue());
    assertEquals(6666, ec.getNativeQueue().getBindport());
    assertEquals(2000, ec.getNativeQueue().getSendqueuelength());
  }

  @Test
  public void testTopicsWithOptionalFields() {
    Config config = withRef(
        "event.subscribe { enable = true, topics = ["
            + "{ triggerName = block, enable = true, topic = block },"
            + "{ triggerName = transaction, enable = false, topic = tx,"
            + "  ethCompatible = true, solidified = true, redundancy = true }"
            + "] }");
    EventConfig ec = EventConfig.fromConfig(config);
    assertEquals(2, ec.getTopics().size());

    EventConfig.TopicConfig t1 = ec.getTopics().get(0);
    assertEquals("block", t1.getTriggerName());
    assertTrue(t1.isEnable());
    assertFalse(t1.isEthCompatible()); // not set, default false
    assertFalse(t1.isSolidified());
    assertFalse(t1.isRedundancy());

    EventConfig.TopicConfig t2 = ec.getTopics().get(1);
    assertEquals("transaction", t2.getTriggerName());
    assertTrue(t2.isEthCompatible());
    assertTrue(t2.isSolidified());
    assertTrue(t2.isRedundancy());
  }

  @Test
  public void testFilter() {
    Config config = withRef(
        "event.subscribe { enable = true,"
            + " filter { fromblock = \"100\", toblock = \"200\","
            + " contractAddress = [\"addr1\", \"addr2\"],"
            + " contractTopic = [\"topic1\"] } }");
    EventConfig ec = EventConfig.fromConfig(config);
    assertEquals("100", ec.getFilter().getFromblock());
    assertEquals("200", ec.getFilter().getToblock());
    assertEquals(2, ec.getFilter().getContractAddress().size());
    assertEquals(1, ec.getFilter().getContractTopic().size());
  }

  @Test
  public void testTopicsEmptyList() {
    EventConfig ec = EventConfig.fromConfig(withRef(
        "event.subscribe.topics = []"));
    assertTrue(ec.getTopics().isEmpty());
  }
}
