package org.tron.core.consensus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockSettings;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.args.GenesisBlock;
import org.tron.common.crypto.SignUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.SRMetrics;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.StringUtil;
import org.tron.consensus.Consensus;
import org.tron.consensus.base.BlockHandle;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.base.PbftInterface;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.WitnessStore;

/**
 * Startup integration tests for the witness-identity publication added to
 * {@link ConsensusService#start()}. Pure JUnit4 + Mockito: no Spring, no real database, no
 * {@code BaseTest}. SRMetrics is static-mocked so nothing touches the global Prometheus registry.
 */
public class ConsensusServiceMetricsTest {

  // Two valid, distinct ECDSA private keys (scalars 1 and 2).
  private static final String KEY_ONE =
      "0000000000000000000000000000000000000000000000000000000000000001";
  private static final String KEY_TWO =
      "0000000000000000000000000000000000000000000000000000000000000002";

  private ConsensusService service;
  private Consensus consensus;
  private WitnessStore witnessStore;
  private BlockHandleImpl blockHandle;
  private PbftBaseImpl pbftBaseImpl;

  // Singleton state saved so each test leaves the process as it found it.
  private LocalWitnesses savedWitnesses;
  private boolean savedMetricsEnabled;
  private boolean savedEnable;
  private boolean savedNeedSyncCheck;
  private int savedMinParticipationRate;
  private int savedBlockProduceTimeoutPercent;
  private GenesisBlock savedGenesisBlock;
  private List<Miner> savedMiners;
  private BlockHandle savedBlockHandle;
  private int savedAgreeNodeCount;
  private PbftInterface savedPbftInterface;

  @Before
  public void setUp() throws Exception {
    savedWitnesses = Args.getLocalWitnesses();
    savedMetricsEnabled = CommonParameter.getInstance().isMetricsPrometheusEnable();

    Param param = Param.getInstance();
    savedEnable = param.isEnable();
    savedNeedSyncCheck = param.isNeedSyncCheck();
    savedMinParticipationRate = param.getMinParticipationRate();
    savedBlockProduceTimeoutPercent = param.getBlockProduceTimeoutPercent();
    savedGenesisBlock = param.getGenesisBlock();
    savedMiners = param.getMiners();
    savedBlockHandle = param.getBlockHandle();
    savedAgreeNodeCount = param.getAgreeNodeCount();
    savedPbftInterface = param.getPbftInterface();

    consensus = mock(Consensus.class);
    witnessStore = mock(WitnessStore.class);
    blockHandle = mock(BlockHandleImpl.class);
    pbftBaseImpl = mock(PbftBaseImpl.class);

    service = new ConsensusService();
    inject(service, "consensus", consensus);
    inject(service, "witnessStore", witnessStore);
    inject(service, "blockHandle", blockHandle);
    inject(service, "pbftBaseImpl", pbftBaseImpl);
  }

  @After
  public void tearDown() {
    Args.setLocalWitnesses(savedWitnesses);
    CommonParameter.getInstance().setMetricsPrometheusEnable(savedMetricsEnabled);

    Param param = Param.getInstance();
    param.setEnable(savedEnable);
    param.setNeedSyncCheck(savedNeedSyncCheck);
    param.setMinParticipationRate(savedMinParticipationRate);
    param.setBlockProduceTimeoutPercent(savedBlockProduceTimeoutPercent);
    param.setGenesisBlock(savedGenesisBlock);
    param.setMiners(savedMiners);
    param.setBlockHandle(savedBlockHandle);
    param.setAgreeNodeCount(savedAgreeNodeCount);
    param.setPbftInterface(savedPbftInterface);
  }

  @Test
  public void singleKeyPublishesWitnessAccountNotSigningKey() throws Exception {
    byte[] signingKey = derivedAddress(KEY_ONE);
    byte[] witnessAccount = derivedAddress(KEY_TWO);
    LocalWitnesses witnesses = witnesses(KEY_ONE);
    witnesses.initWitnessAccountAddress(witnessAccount);
    Args.setLocalWitnesses(witnesses);
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);
    when(witnessStore.get(witnessAccount)).thenReturn(mock(WitnessCapsule.class));

    try (MockedStatic<SRMetrics> sr = mockStatic(SRMetrics.class)) {
      service.start();

      sr.verify(() -> SRMetrics.recordWitnessAddress(StringUtil.encode58Check(witnessAccount)));
      sr.verify(() -> SRMetrics.recordWitnessAddress(StringUtil.encode58Check(signingKey)),
          never());
    }
    verify(consensus).start(Mockito.any(Param.class));
    verify(witnessStore).get(witnessAccount);
    verifyNoMoreInteractions(witnessStore);
  }

  @Test
  public void multiKeyPublishesEveryWitnessAddress() throws Exception {
    Args.setLocalWitnesses(witnesses(KEY_ONE, KEY_TWO));
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);
    byte[] addrOne = derivedAddress(KEY_ONE);
    byte[] addrTwo = derivedAddress(KEY_TWO);
    when(witnessStore.get(addrOne)).thenReturn(mock(WitnessCapsule.class));
    when(witnessStore.get(addrTwo)).thenReturn(mock(WitnessCapsule.class));

    try (MockedStatic<SRMetrics> sr = mockStatic(SRMetrics.class)) {
      service.start();

      sr.verify(() -> SRMetrics.recordWitnessAddress(StringUtil.encode58Check(addrOne)));
      sr.verify(() -> SRMetrics.recordWitnessAddress(StringUtil.encode58Check(addrTwo)));
    }
    verify(consensus).start(Mockito.any(Param.class));
    verify(witnessStore).get(addrOne);
    verify(witnessStore).get(addrTwo);
    verifyNoMoreInteractions(witnessStore);
  }

  @Test
  public void noKeysPublishesNothingAndQueriesNoWitness() throws Exception {
    Args.setLocalWitnesses(new LocalWitnesses());
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);

    try (MockedStatic<SRMetrics> sr = mockStatic(SRMetrics.class)) {
      service.start();
      sr.verifyNoInteractions();
    }
    verify(consensus).start(Mockito.any(Param.class));
    verifyNoInteractions(witnessStore);
  }

  @Test
  public void disabledMetricsDoesNotPublish() throws Exception {
    byte[] witnessAccount = derivedAddress(KEY_TWO);
    LocalWitnesses witnesses = witnesses(KEY_ONE);
    witnesses.initWitnessAccountAddress(witnessAccount);
    Args.setLocalWitnesses(witnesses);
    CommonParameter.getInstance().setMetricsPrometheusEnable(false);
    when(witnessStore.get(witnessAccount)).thenReturn(mock(WitnessCapsule.class));

    try (MockedStatic<SRMetrics> sr = mockStatic(SRMetrics.class)) {
      service.start();
      sr.verifyNoInteractions();
    }
    verify(consensus).start(Mockito.any(Param.class));
    verify(witnessStore).get(witnessAccount);
    verifyNoMoreInteractions(witnessStore);
  }

  @Test
  public void metricsFailureDoesNotAbortStart() throws Exception {
    Args.setLocalWitnesses(witnesses(KEY_ONE, KEY_TWO));
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);
    byte[] addrOne = derivedAddress(KEY_ONE);
    byte[] addrTwo = derivedAddress(KEY_TWO);
    when(witnessStore.get(addrOne)).thenReturn(mock(WitnessCapsule.class));
    when(witnessStore.get(addrTwo)).thenReturn(mock(WitnessCapsule.class));

    MockSettings throwing = Mockito.withSettings().defaultAnswer(invocation -> {
      throw new RuntimeException("metrics unavailable");
    });
    try (MockedStatic<SRMetrics> sr = Mockito.mockStatic(SRMetrics.class, throwing)) {
      service.start();

      // The per-miner try/catch must keep the loop going after the first failure.
      sr.verify(() -> SRMetrics.recordWitnessAddress(StringUtil.encode58Check(addrOne)));
      sr.verify(() -> SRMetrics.recordWitnessAddress(StringUtil.encode58Check(addrTwo)));
    }
    verify(consensus).start(Mockito.any(Param.class));
    verify(witnessStore).get(addrOne);
    verify(witnessStore).get(addrTwo);
    verifyNoMoreInteractions(witnessStore);
  }

  @Test
  public void nullWitnessAddressIsSkipped() throws Exception {
    CommonParameter.getInstance().setMetricsPrometheusEnable(true);
    Miner miner = Param.getInstance().new Miner(null, null, null);

    try (MockedStatic<SRMetrics> sr = mockStatic(SRMetrics.class)) {
      ReflectionTestUtils.invokeMethod(service, "publishWitnessIdentities", Arrays.asList(miner));
      sr.verifyNoInteractions();
    }
  }

  private static LocalWitnesses witnesses(String... keys) {
    LocalWitnesses witnesses = new LocalWitnesses();
    witnesses.setPrivateKeys(Arrays.asList(keys));
    return witnesses;
  }

  private static byte[] derivedAddress(String privateKey) {
    return SignUtils.fromPrivate(ByteArray.fromHexString(privateKey)).getAddress();
  }

  private static void inject(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
