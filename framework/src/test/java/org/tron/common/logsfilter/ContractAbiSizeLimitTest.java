package org.tron.common.logsfilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.tron.common.logsfilter.ContractEventParserAbi.MAX_ABI_ENTRIES;
import static org.tron.common.logsfilter.ContractEventParserAbi.MAX_ABI_EVENTS;
import static org.tron.common.logsfilter.ContractEventParserAbi.MAX_ABI_IDENTIFIER_LENGTH;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.tron.common.TestConstants;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.capsule.ContractTriggerCapsule;
import org.tron.common.logsfilter.capsule.RawData;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.config.args.Args;
import org.tron.core.services.event.BlockEventGet;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

/**
 * Behavior tests for the ABI-size guard on both event-subscription decode paths — version 0
 * ({@link ContractTriggerCapsule#processTrigger()}) and version 1
 * ({@link BlockEventGet} {@code processTrigger}). Each over-limit case carries a topic0 that
 * matches an event present in the ABI, so without the guard it would decode; the test therefore
 * fails if the call-site guard is removed, not merely if the predicate changes.
 */
public class ContractAbiSizeLimitTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[] {"--output-directory", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
    Args.getInstance().getStorage().setContractParseSwitch(true);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  private ABI.Entry event(String name, String... paramTypes) {
    ABI.Entry.Builder e = ABI.Entry.newBuilder()
        .setType(ABI.Entry.EntryType.Event).setAnonymous(false).setName(name);
    for (String t : paramTypes) {
      e.addInputs(ABI.Entry.Param.newBuilder().setType(t));
    }
    return e.build();
  }

  private ABI.Entry function(String name) {
    return ABI.Entry.newBuilder().setType(ABI.Entry.EntryType.Function).setName(name).build();
  }

  /** topic0 = sha3("name(type1,type2)"), i.e. what a log matching this event would carry. */
  private DataWord topic0(String name, String... paramTypes) {
    StringBuilder sig = new StringBuilder(name).append("(");
    for (int i = 0; i < paramTypes.length; i++) {
      if (i > 0) {
        sig.append(",");
      }
      sig.append(paramTypes[i]);
    }
    sig.append(")");
    return new DataWord(Hash.sha3(sig.toString().getBytes()));
  }

  private ContractTrigger trigger(ABI abi, DataWord topic0) {
    byte[] address = new byte[20];
    List<DataWord> topics = Lists.newArrayList(topic0);
    byte[] data = new byte[64];
    data[31] = 0x2a;
    data[63] = 0x7b;
    ContractTrigger t = new ContractTrigger();
    t.setBlockNumber(1L);
    t.setRemoved(false);
    t.setAbi(abi);
    t.setLogInfo(new LogInfo(address, topics, data));
    t.setRawData(new RawData(address, topics, data));
    return t;
  }

  /**
   * Runs the version-1 path: {@link BlockEventGet} {@code processTrigger} returns the decoded
   * event trigger, or the raw log trigger when the ABI is rejected.
   */
  private ContractTrigger v1Process(ContractTrigger input) throws Exception {
    Method m = BlockEventGet.class.getDeclaredMethod("processTrigger", ContractTrigger.class);
    m.setAccessible(true);
    return (ContractTrigger) m.invoke(new BlockEventGet(), input);
  }

  @Test
  public void v1_inLimitAbiIsDecodedToEvent() throws Exception {
    ABI abi = ABI.newBuilder().addEntrys(event("Transfer", "address", "uint256")).build();
    ContractTrigger out = v1Process(trigger(abi, topic0("Transfer", "address", "uint256")));
    Assert.assertTrue(out instanceof ContractEventTrigger);
    Assert.assertEquals("Transfer", ((ContractEventTrigger) out).getEventName());
  }

  @Test
  public void v1_overEventLimitFallsBackToRawLog() throws Exception {
    ABI.Builder b = ABI.newBuilder();
    for (int i = 0; i < MAX_ABI_EVENTS; i++) {
      b.addEntrys(event("Filler" + i));
    }
    b.addEntrys(event("Transfer", "address", "uint256")); // 513th event, matches topic0
    ContractTrigger in = trigger(b.build(), topic0("Transfer", "address", "uint256"));
    ContractTrigger out = v1Process(in);
    Assert.assertTrue(out instanceof ContractLogTrigger);
    Assert.assertFalse(out instanceof ContractEventTrigger);
    // raw data is preserved
    Assert.assertEquals(in.getLogInfo().getHexTopics(), ((ContractLogTrigger) out).getTopicList());
    Assert.assertEquals(in.getLogInfo().getHexData(), ((ContractLogTrigger) out).getData());
  }

  @Test
  public void v1_overEntryLimitFallsBackToRawLog() throws Exception {
    ABI.Builder b = ABI.newBuilder();
    for (int i = 0; i < MAX_ABI_ENTRIES; i++) {
      b.addEntrys(function("f" + i));
    }
    b.addEntrys(event("Transfer", "address", "uint256")); // 1025th entry, matches topic0
    ContractTrigger out = v1Process(trigger(b.build(), topic0("Transfer", "address", "uint256")));
    Assert.assertTrue(out instanceof ContractLogTrigger);
    Assert.assertFalse(out instanceof ContractEventTrigger);
  }

  @Test
  public void v1_oversizedEntrySkippedButValidEntryMatches() throws Exception {
    ABI abi = ABI.newBuilder()
        .addEntrys(event(repeatA(MAX_ABI_IDENTIFIER_LENGTH + 1))) // oversized name, skipped
        .addEntrys(event("Transfer", "address", "uint256"))       // valid, matches
        .build();
    ContractTrigger out = v1Process(trigger(abi, topic0("Transfer", "address", "uint256")));
    Assert.assertTrue(out instanceof ContractEventTrigger);
    Assert.assertEquals("Transfer", ((ContractEventTrigger) out).getEventName());
  }

  @Test
  public void v1_oversizedMatchingEntryIsSkippedAndFallsBackToRawLog() throws Exception {
    // The over-long entry is itself the one the log matches: the per-entry skip is the only
    // thing preventing it from being decoded, so dropping that guard would turn this into an
    // event instead of a raw log.
    String bigName = repeatA(MAX_ABI_IDENTIFIER_LENGTH + 1);
    ABI abi = ABI.newBuilder().addEntrys(event(bigName)).build();
    ContractTrigger out = v1Process(trigger(abi, topic0(bigName)));
    Assert.assertTrue(out instanceof ContractLogTrigger);
    Assert.assertFalse(out instanceof ContractEventTrigger);
  }

  @Test
  public void v0_inLimitAbiIsDecodedToEvent() throws Exception {
    EventPluginLoader mock = contractTriggersEnabled();
    EventPluginLoader original = swapLoader(mock);
    try {
      ABI abi = ABI.newBuilder().addEntrys(event("Transfer", "address", "uint256")).build();
      new ContractTriggerCapsule(trigger(abi, topic0("Transfer", "address", "uint256")))
          .processTrigger();
      ArgumentCaptor<ContractEventTrigger> captor =
          ArgumentCaptor.forClass(ContractEventTrigger.class);
      verify(mock).postContractEventTrigger(captor.capture());
      verify(mock, never()).postContractLogTrigger(any());
      Assert.assertEquals("Transfer", captor.getValue().getEventName());
    } finally {
      swapLoader(original);
    }
  }

  @Test
  public void v0_overEventLimitFallsBackToRawLog() throws Exception {
    ABI.Builder b = ABI.newBuilder();
    for (int i = 0; i < MAX_ABI_EVENTS; i++) {
      b.addEntrys(event("Filler" + i));
    }
    b.addEntrys(event("Transfer", "address", "uint256")); // would decode without the guard
    v0AssertFallsBackToRawLog(b.build(), topic0("Transfer", "address", "uint256"));
  }

  @Test
  public void v0_overEntryLimitFallsBackToRawLog() throws Exception {
    ABI.Builder b = ABI.newBuilder();
    for (int i = 0; i < MAX_ABI_ENTRIES; i++) {
      b.addEntrys(function("f" + i));
    }
    b.addEntrys(event("Transfer", "address", "uint256")); // would decode without the guard
    v0AssertFallsBackToRawLog(b.build(), topic0("Transfer", "address", "uint256"));
  }

  @Test
  public void v0_oversizedMatchingEntryFallsBackToRawLog() throws Exception {
    String bigName = repeatA(MAX_ABI_IDENTIFIER_LENGTH + 1);
    ABI abi = ABI.newBuilder().addEntrys(event(bigName)).build();
    v0AssertFallsBackToRawLog(abi, topic0(bigName));
  }

  @Test
  public void v0_oversizedEntrySkippedButValidEntryMatches() throws Exception {
    EventPluginLoader mock = contractTriggersEnabled();
    EventPluginLoader original = swapLoader(mock);
    try {
      ABI abi = ABI.newBuilder()
          .addEntrys(event(repeatA(MAX_ABI_IDENTIFIER_LENGTH + 1))) // oversized name, skipped
          .addEntrys(event("Transfer", "address", "uint256"))       // valid, matches
          .build();
      new ContractTriggerCapsule(trigger(abi, topic0("Transfer", "address", "uint256")))
          .processTrigger();
      ArgumentCaptor<ContractEventTrigger> captor =
          ArgumentCaptor.forClass(ContractEventTrigger.class);
      verify(mock).postContractEventTrigger(captor.capture());
      Assert.assertEquals("Transfer", captor.getValue().getEventName());
    } finally {
      swapLoader(original);
    }
  }

  private void v0AssertFallsBackToRawLog(ABI abi, DataWord topic0) throws Exception {
    EventPluginLoader mock = contractTriggersEnabled();
    EventPluginLoader original = swapLoader(mock);
    try {
      ContractTrigger in = trigger(abi, topic0);
      new ContractTriggerCapsule(in).processTrigger();
      ArgumentCaptor<ContractLogTrigger> captor =
          ArgumentCaptor.forClass(ContractLogTrigger.class);
      verify(mock, never()).postContractEventTrigger(any());
      verify(mock).postContractLogTrigger(captor.capture());
      // raw data is preserved
      Assert.assertEquals(in.getLogInfo().getHexTopics(), captor.getValue().getTopicList());
      Assert.assertEquals(in.getLogInfo().getHexData(), captor.getValue().getData());
    } finally {
      swapLoader(original);
    }
  }

  private EventPluginLoader contractTriggersEnabled() {
    EventPluginLoader mock = mock(EventPluginLoader.class);
    when(mock.isContractEventTriggerEnable()).thenReturn(true);
    when(mock.isContractLogTriggerEnable()).thenReturn(true);
    return mock;
  }

  private EventPluginLoader swapLoader(EventPluginLoader loader) throws Exception {
    Field f = EventPluginLoader.class.getDeclaredField("instance");
    f.setAccessible(true);
    EventPluginLoader previous = (EventPluginLoader) f.get(null);
    f.set(null, loader);
    return previous;
  }

  private String repeatA(int n) {
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) {
      sb.append('A');
    }
    return sb.toString();
  }
}
