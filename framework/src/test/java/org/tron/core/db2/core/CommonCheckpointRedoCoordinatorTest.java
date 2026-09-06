package org.tron.core.db2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Status;
import org.tron.core.db2.core.CommonCheckpointRedoCoordinator.RecoveryAction;
import org.tron.core.db2.stateroot.PathStateFlushTarget;

public class CommonCheckpointRedoCoordinatorTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void appliesTwoBarriersThenRetiresAndSecondRecoveryDoesNothing() throws Exception {
    Fixture fixture = fixture("normal", null);

    assertEquals(RecoveryAction.COMPLETED_REDO, fixture.coordinator.apply(fixture.payload));
    assertEquals(list("materialize-CHAINBASE", "materialize-PATH_STATE",
        "materialize-STATE_ARCHIVE", "publish-CHAINBASE", "publish-PATH_STATE",
        "publish-STATE_ARCHIVE"), fixture.actions);
    assertFalse(Files.exists(fixture.file.getCheckpointPath()));
    int actionCount = fixture.actions.size();
    assertEquals(RecoveryAction.NO_CHECKPOINT, fixture.coordinator.recover());
    assertEquals(actionCount, fixture.actions.size());
  }

  @Test
  public void recordsDeterministicRedoPhaseTimingsWithoutChangingBarrierCalls()
      throws Exception {
    Fixture fixture = fixture("timings", null);
    AtomicLong clock = new AtomicLong();
    List<CommonCheckpointRedoCoordinator.Timing> timings = new ArrayList<>();
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        fixture.file, fixture.materializers.get(0), fixture.materializers.get(1),
        fixture.materializers.get(2), stage -> { }, () -> clock.addAndGet(1_000L),
        timings::add);

    assertEquals(RecoveryAction.COMPLETED_REDO, coordinator.apply(fixture.payload));
    assertEquals(1, timings.size());
    CommonCheckpointRedoCoordinator.Timing timing = timings.get(0);
    assertEquals("apply", timing.getMode());
    assertEquals(1, timing.getHead());
    assertEquals(1, timing.getBlocks());
    assertEquals(1, timing.getWalPublishUs());
    assertEquals(1, timing.getWalLoadUs());
    assertEquals(1, timing.getWalRetireUs());
    for (Authority authority : Authority.values()) {
      assertEquals(1, timing.begin(authority));
      assertEquals(5, timing.inspect(authority));
      assertEquals(5, timing.inspectCount(authority));
      assertEquals(1, timing.materialize(authority));
      assertEquals(1, timing.materializeCount(authority));
      assertEquals(1, timing.publish(authority));
      assertEquals(1, timing.publishCount(authority));
      assertEquals(1, timing.end(authority));
    }
    assertTrue(timing.getTotalUs() > 0);
  }

  @Test
  public void ignoresTimingSinkFailureAfterDurableCheckpointCompletes() throws Exception {
    Fixture fixture = fixture("timing-sink-failure", null);
    AtomicLong clock = new AtomicLong();
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        fixture.file, fixture.materializers.get(0), fixture.materializers.get(1),
        fixture.materializers.get(2), stage -> { }, () -> clock.addAndGet(1_000L),
        timing -> {
          throw new IllegalStateException("injected timing sink failure");
        });

    assertEquals(RecoveryAction.COMPLETED_REDO, coordinator.apply(fixture.payload));
    assertFalse(Files.exists(fixture.file.getCheckpointPath()));
    for (FakeMaterializer materializer : fixture.materializers) {
      assertEquals(Status.PUBLISHED, materializer.status);
    }
  }

  @Test
  public void recordsRecoveryTimingsWithoutRepeatingPublishedAuthorityWork() throws Exception {
    Fixture fixture = fixture("recovery-timings",
        CommonCheckpointRedoCoordinator.Stage.BEFORE_CHECKPOINT_RETIRE);
    assertThrows(IOException.class, () -> fixture.coordinator.apply(fixture.payload));
    assertTrue(Files.isRegularFile(fixture.file.getCheckpointPath()));

    AtomicLong clock = new AtomicLong();
    List<CommonCheckpointRedoCoordinator.Timing> timings = new ArrayList<>();
    CommonCheckpointRedoCoordinator recovered = new CommonCheckpointRedoCoordinator(
        fixture.file, fixture.materializers.get(0), fixture.materializers.get(1),
        fixture.materializers.get(2), stage -> { }, () -> clock.addAndGet(1_000L),
        timings::add);

    assertEquals(RecoveryAction.COMPLETED_REDO, recovered.recover());
    assertEquals(1, timings.size());
    CommonCheckpointRedoCoordinator.Timing timing = timings.get(0);
    assertEquals("recover", timing.getMode());
    assertEquals(0, timing.getWalPublishUs());
    assertEquals(1, timing.getWalLoadUs());
    assertEquals(1, timing.getWalRetireUs());
    for (Authority authority : Authority.values()) {
      assertEquals(1, timing.begin(authority));
      assertEquals(3, timing.inspect(authority));
      assertEquals(3, timing.inspectCount(authority));
      assertEquals(0, timing.materialize(authority));
      assertEquals(0, timing.materializeCount(authority));
      assertEquals(0, timing.publish(authority));
      assertEquals(0, timing.publishCount(authority));
      assertEquals(1, timing.end(authority));
    }
    assertTrue(timing.getTotalUs() > 0);
    assertFalse(Files.exists(fixture.file.getCheckpointPath()));
  }

  @Test
  public void resumesEveryCoordinatorBoundaryAgainstTheSameTarget() throws Exception {
    for (CommonCheckpointRedoCoordinator.Stage stage
        : CommonCheckpointRedoCoordinator.Stage.values()) {
      Fixture interrupted = fixture("failure-" + stage, stage);
      assertThrows(IOException.class, () -> interrupted.coordinator.apply(interrupted.payload));

      CommonCheckpointRedoCoordinator recovered = interrupted.coordinator(null);
      RecoveryAction expected = stage
          == CommonCheckpointRedoCoordinator.Stage.AFTER_CHECKPOINT_RETIRE
          ? RecoveryAction.NO_CHECKPOINT : RecoveryAction.COMPLETED_REDO;
      assertEquals(expected, recovered.recover());
      assertFalse(Files.exists(interrupted.file.getCheckpointPath()));
      assertEquals(RecoveryAction.NO_CHECKPOINT, recovered.recover());
      for (FakeMaterializer materializer : interrupted.materializers) {
        assertEquals(Status.PUBLISHED, materializer.status);
        assertEquals(CommonCheckpointTarget.from(interrupted.payload), materializer.target);
      }
    }
  }

  @Test
  public void rejectsPublishedAuthorityBeforeGlobalMaterializationBarrier() throws Exception {
    Fixture fixture = fixture("invalid-partial-publish", null);
    fixture.materializers.get(0).status = Status.PUBLISHED;

    assertThrows(IOException.class, () -> fixture.coordinator.apply(fixture.payload));
    assertTrue(Files.isRegularFile(fixture.file.getCheckpointPath()));
    assertTrue(fixture.actions.isEmpty());
  }

  @Test
  public void rejectsWrongAuthorityAndMaterializerThatDoesNotReachExactStatus() throws Exception {
    Fixture fixture = fixture("invalid-materializer", null);
    assertThrows(IllegalArgumentException.class, () -> new CommonCheckpointRedoCoordinator(
        fixture.file, fixture.materializers.get(1), fixture.materializers.get(0),
        fixture.materializers.get(2)));

    fixture.materializers.get(0).advanceAfterMaterialize = false;
    assertThrows(IOException.class, () -> fixture.coordinator.apply(fixture.payload));
    assertTrue(Files.isRegularFile(fixture.file.getCheckpointPath()));
    assertEquals(Collections.singletonList("materialize-CHAINBASE"), fixture.actions);
  }

  @Test
  public void closesEveryCheckpointScopeAfterSuccessAndFailure() throws Exception {
    Fixture completed = fixture("scope-success", null);
    assertEquals(RecoveryAction.COMPLETED_REDO, completed.coordinator.apply(completed.payload));
    for (FakeMaterializer materializer : completed.materializers) {
      assertEquals(1, materializer.scopesStarted);
      assertEquals(1, materializer.scopesEnded);
      assertFalse(materializer.scopeOpen);
    }

    Fixture failed = fixture("scope-failure",
        CommonCheckpointRedoCoordinator.Stage.AFTER_CHAINBASE_MATERIALIZE);
    assertThrows(IOException.class, () -> failed.coordinator.apply(failed.payload));
    for (FakeMaterializer materializer : failed.materializers) {
      assertEquals(1, materializer.scopesStarted);
      assertEquals(1, materializer.scopesEnded);
      assertFalse(materializer.scopeOpen);
    }
  }

  @Test
  public void runtimeOwnerRequiresStartupRecoveryAndGatesReadsAroundApply() throws Exception {
    Fixture fixture = fixture("runtime-owner", null);
    CommonCheckpointRuntimeOwner owner = new CommonCheckpointRuntimeOwner(fixture.coordinator);

    assertEquals(CommonCheckpointRuntimeOwner.State.NEW, owner.getState());
    assertThrows(IOException.class, () -> owner.read(() -> "unreachable"));
    assertEquals(RecoveryAction.NO_CHECKPOINT, owner.recoverBeforeServing());
    assertEquals("ready", owner.read(() -> "ready"));
    assertEquals(RecoveryAction.COMPLETED_REDO, owner.apply(fixture.payload));
    assertEquals(CommonCheckpointRuntimeOwner.State.READY, owner.getState());
    assertEquals("published", owner.read(() -> "published"));
    owner.close();
    assertThrows(IOException.class, () -> owner.read(() -> "unreachable"));
  }

  @Test
  public void runtimeOwnerRequestLeaseBlocksCheckpointUntilClosed() throws Exception {
    Fixture fixture = fixture("runtime-owner-request-lease", null);
    CommonCheckpointRuntimeOwner owner = new CommonCheckpointRuntimeOwner(fixture.coordinator);
    assertEquals(RecoveryAction.NO_CHECKPOINT, owner.recoverBeforeServing());

    CommonCheckpointRuntimeOwner.ReadLease lease = owner.acquireReadLease();
    CountDownLatch started = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<RecoveryAction> checkpoint = executor.submit(() -> {
      started.countDown();
      return owner.apply(fixture.payload);
    });
    try {
      assertTrue(started.await(5, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class,
          () -> checkpoint.get(100, TimeUnit.MILLISECONDS));
    } finally {
      lease.close();
    }
    try {
      assertEquals(RecoveryAction.COMPLETED_REDO, checkpoint.get(5, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void runtimeOwnerFailsClosedThenFreshOwnerRedoesDurableCheckpoint() throws Exception {
    Fixture fixture = fixture("runtime-owner-failure",
        CommonCheckpointRedoCoordinator.Stage.AFTER_CHAINBASE_MATERIALIZE);
    CommonCheckpointRuntimeOwner failed = new CommonCheckpointRuntimeOwner(fixture.coordinator);
    assertEquals(RecoveryAction.NO_CHECKPOINT, failed.recoverBeforeServing());
    assertThrows(IOException.class, () -> failed.apply(fixture.payload));
    assertEquals(CommonCheckpointRuntimeOwner.State.FAILED, failed.getState());
    assertThrows(IOException.class, () -> failed.read(() -> "unreachable"));

    CommonCheckpointRuntimeOwner recovered = new CommonCheckpointRuntimeOwner(
        fixture.coordinator(null));
    assertEquals(RecoveryAction.COMPLETED_REDO, recovered.recoverBeforeServing());
    assertEquals(CommonCheckpointRuntimeOwner.State.READY, recovered.getState());
    assertEquals("recovered", recovered.read(() -> "recovered"));
  }

  private Fixture fixture(String name, CommonCheckpointRedoCoordinator.Stage failure) {
    CommonCheckpointPayload payload = payload();
    CommonCheckpointFile file = new CommonCheckpointFile(
        temporaryFolder.getRoot().toPath().resolve(name));
    List<String> actions = new ArrayList<>();
    List<FakeMaterializer> materializers = new ArrayList<>();
    for (Authority authority : Authority.values()) {
      materializers.add(new FakeMaterializer(authority, actions));
    }
    return new Fixture(payload, file, actions, materializers, failure);
  }

  private static CommonCheckpointPayload payload() {
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    byte[] viewDigest = hash(4);
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(hash(5));
    when(binding.getStateRoot()).thenReturn(hash(6));
    when(binding.getTransitionPayloadDigest()).thenReturn(hash(3));
    when(binding.getMutationViewDigest()).thenReturn(viewDigest);
    PathStateFlushTarget target = mock(PathStateFlushTarget.class);
    when(target.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(target.getParentStateRoot()).thenReturn(hash(5));
    when(target.getStateRoot()).thenReturn(hash(6));
    when(target.getStores()).thenReturn(Collections.emptyList());
    when(target.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return CommonCheckpointPayload.create(hash(7), target,
        Collections.singletonList(new BlockReverseDiff(meta, Collections.emptyList(), viewDigest)),
        Collections.emptyList());
  }

  private static List<String> list(String... values) {
    List<String> result = new ArrayList<>();
    Collections.addAll(result, values);
    return result;
  }

  private static byte[] hash(int seed) {
    byte[] hash = new byte[32];
    for (int index = 0; index < hash.length; index++) {
      hash[index] = (byte) (seed + index);
    }
    return hash;
  }

  private static final class Fixture {

    private final CommonCheckpointPayload payload;
    private final CommonCheckpointFile file;
    private final List<String> actions;
    private final List<FakeMaterializer> materializers;
    private final CommonCheckpointRedoCoordinator coordinator;

    private Fixture(CommonCheckpointPayload payload, CommonCheckpointFile file,
        List<String> actions, List<FakeMaterializer> materializers,
        CommonCheckpointRedoCoordinator.Stage failure) {
      this.payload = payload;
      this.file = file;
      this.actions = actions;
      this.materializers = materializers;
      this.coordinator = coordinator(failure);
    }

    private CommonCheckpointRedoCoordinator coordinator(
        CommonCheckpointRedoCoordinator.Stage failedStage) {
      return new CommonCheckpointRedoCoordinator(file, materializers.get(0),
          materializers.get(1), materializers.get(2), failAt(failedStage));
    }
  }

  private static CommonCheckpointRedoCoordinator.FaultHook failAt(
      CommonCheckpointRedoCoordinator.Stage failedStage) {
    return stage -> {
      if (stage == failedStage) {
        throw new IOException("injected " + stage);
      }
    };
  }

  private static final class FakeMaterializer implements CommonCheckpointMaterializer {

    private final Authority authority;
    private final List<String> actions;
    private Status status = Status.NEEDS_MATERIALIZATION;
    private CommonCheckpointTarget target;
    private boolean advanceAfterMaterialize = true;
    private int scopesStarted;
    private int scopesEnded;
    private boolean scopeOpen;

    private FakeMaterializer(Authority authority, List<String> actions) {
      this.authority = authority;
      this.actions = actions;
    }

    @Override
    public Authority authority() {
      return authority;
    }

    @Override
    public void beginCheckpoint(CommonCheckpointTarget expected) {
      scopesStarted++;
      scopeOpen = true;
    }

    @Override
    public void endCheckpoint(CommonCheckpointTarget expected) {
      scopesEnded++;
      scopeOpen = false;
    }

    @Override
    public Status inspect(CommonCheckpointTarget expected) throws IOException {
      if (target != null && !target.equals(expected)) {
        throw new IOException("target mismatch");
      }
      return status;
    }

    @Override
    public void materialize(CommonCheckpointPayload payload, CommonCheckpointTarget expected) {
      actions.add("materialize-" + authority);
      target = expected;
      if (advanceAfterMaterialize) {
        status = Status.MATERIALIZED;
      }
    }

    @Override
    public void publish(CommonCheckpointTarget expected) throws IOException {
      if (status != Status.MATERIALIZED || !expected.equals(target)) {
        throw new IOException("publish without exact materialization");
      }
      actions.add("publish-" + authority);
      status = Status.PUBLISHED;
    }
  }
}
