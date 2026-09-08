package org.tron.core.db2.stateroot;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateParticipantDescriptor.StoreIdentity;

/**
 * Immutable, origin-free evidence for one block-final path-state transition.
 *
 * <p>The transition intentionally does not distinguish locally generated, normally pushed, or
 * fork-reapplied blocks. Once execution reaches the metadata-aware block commit boundary, equal
 * block identity, phase, and canonical mutations produce equal evidence. This type does not
 * capture sessions, apply a trie, publish a root, or retain historical state.
 */
public final class PathStateBlockTransition {

  public static final int FORMAT_VERSION = 1;
  public static final int HASH_LENGTH = 32;

  private static final byte DELETE_TAG = 0;
  private static final byte PUT_TAG = 1;
  private static final byte[] DOMAIN =
      "java-tron/path-state/block-transition".getBytes(StandardCharsets.US_ASCII);
  private static final Comparator<PreparedMutation> MUTATION_ORDER = (left, right) -> {
    int storeOrder = Integer.compare(left.store.getStoreId(), right.store.getStoreId());
    return storeOrder != 0 ? storeOrder
        : compareUnsigned(left.mutation.getCanonicalKey(), right.mutation.getCanonicalKey());
  };

  private final long blockNumber;
  private final byte[] blockHash;
  private final byte[] parentHash;
  private final long timestamp;
  private final P66Phase phase;
  private final List<PathStateMutation> mutations;
  private final byte[] payloadDigest;

  public PathStateBlockTransition(long blockNumber, byte[] blockHash, byte[] parentHash,
      long timestamp, P66Phase phase, Collection<PathStateMutation> mutations) {
    if (blockNumber < 0) {
      throw new IllegalArgumentException("blockNumber must not be negative");
    }
    this.blockNumber = blockNumber;
    this.blockHash = copyHash(blockHash, "blockHash");
    this.parentHash = copyHash(parentHash, "parentHash");
    this.timestamp = timestamp;
    this.phase = Objects.requireNonNull(phase, "phase");
    List<PreparedMutation> prepared = prepare(mutations);
    List<PathStateMutation> canonical = new ArrayList<>(prepared.size());
    for (PreparedMutation mutation : prepared) {
      canonical.add(mutation.mutation);
    }
    this.mutations = Collections.unmodifiableList(canonical);
    this.payloadDigest = sha256(encode(prepared));
  }

  public long getBlockNumber() {
    return blockNumber;
  }

  public byte[] getBlockHash() {
    return Arrays.copyOf(blockHash, blockHash.length);
  }

  public byte[] getParentHash() {
    return Arrays.copyOf(parentHash, parentHash.length);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public P66Phase getPhase() {
    return phase;
  }

  public String getScopeId() {
    return PathStateParticipantDescriptor.SCOPE_ID;
  }

  public List<PathStateMutation> getMutations() {
    return mutations;
  }

  public byte[] getPayloadDigest() {
    return Arrays.copyOf(payloadDigest, payloadDigest.length);
  }

  private List<PreparedMutation> prepare(Collection<PathStateMutation> supplied) {
    PathStateParticipantDescriptor descriptor = PathStateParticipantDescriptor.current();
    List<PreparedMutation> prepared = new ArrayList<>();
    Set<MutationKey> unique = new LinkedHashSet<>();
    for (PathStateMutation candidate : Objects.requireNonNull(supplied, "mutations")) {
      PathStateMutation mutation = copyMutation(Objects.requireNonNull(candidate, "mutation"));
      StoreIdentity store = descriptor.require(mutation.getDbName());
      byte[] key = mutation.getCanonicalKey();
      if (!unique.add(new MutationKey(store.getStoreId(), key))) {
        throw new IllegalArgumentException("duplicate path-state mutation key");
      }
      prepared.add(new PreparedMutation(store, mutation));
    }
    prepared.sort(MUTATION_ORDER);
    return prepared;
  }

  private byte[] encode(List<PreparedMutation> prepared) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream output = new DataOutputStream(bytes);
      writeBytes(output, DOMAIN);
      output.writeShort(FORMAT_VERSION);
      writeBytes(output, PathStateParticipantDescriptor.SCOPE_ID
          .getBytes(StandardCharsets.UTF_8));
      output.writeByte(phaseTag(phase));
      output.writeLong(blockNumber);
      output.write(blockHash);
      output.write(parentHash);
      output.writeLong(timestamp);
      output.writeInt(prepared.size());
      for (PreparedMutation current : prepared) {
        PathStateMutation mutation = current.mutation;
        output.writeInt(current.store.getStoreId());
        writeBytes(output, current.store.getDbName().getBytes(StandardCharsets.UTF_8));
        output.writeByte(mutation.isDelete() ? DELETE_TAG : PUT_TAG);
        writeSizedBytes(output, mutation.getCanonicalKey());
        byte[] value = mutation.getCanonicalValue();
        if (value == null) {
          output.writeInt(-1);
        } else {
          writeSizedBytes(output, value);
        }
      }
      output.flush();
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("in-memory path-state transition encoding failed", impossible);
    }
  }

  private static PathStateMutation copyMutation(PathStateMutation mutation) {
    PathStateMutation copy = mutation.isDelete()
        ? PathStateMutation.delete(mutation.getDbName(), mutation.getCanonicalKey())
        : PathStateMutation.put(mutation.getDbName(), mutation.getCanonicalKey(),
            mutation.getCanonicalValue());
    return mutation.isPreviousValueKnown()
        ? copy.withPreviousPhysicalValue(mutation.getPreviousPhysicalValue()) : copy;
  }

  private static byte[] copyHash(byte[] value, String name) {
    byte[] hash = Arrays.copyOf(Objects.requireNonNull(value, name), value.length);
    if (hash.length != HASH_LENGTH) {
      throw new IllegalArgumentException(name + " must be exactly " + HASH_LENGTH + " bytes");
    }
    return hash;
  }

  private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
    if (value.length > 0xffff) {
      throw new IllegalArgumentException("identity field exceeds unsigned-short length");
    }
    output.writeShort(value.length);
    output.write(value);
  }

  private static void writeSizedBytes(DataOutputStream output, byte[] value) throws IOException {
    output.writeInt(value.length);
    output.write(value);
  }

  private static int phaseTag(P66Phase phase) {
    switch (phase) {
      case P66_OFF:
        return 0;
      case P66_ACTIVATION:
        return 1;
      case P66_ON:
        return 2;
      default:
        throw new IllegalArgumentException("unknown P66 phase: " + phase);
    }
  }

  private static byte[] sha256(byte[] value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
  }

  private static int compareUnsigned(byte[] left, byte[] right) {
    for (int i = 0; i < Math.min(left.length, right.length); i++) {
      int comparison = Integer.compare(left[i] & 0xff, right[i] & 0xff);
      if (comparison != 0) {
        return comparison;
      }
    }
    return Integer.compare(left.length, right.length);
  }

  private static final class PreparedMutation {

    private final StoreIdentity store;
    private final PathStateMutation mutation;

    private PreparedMutation(StoreIdentity store, PathStateMutation mutation) {
      this.store = store;
      this.mutation = mutation;
    }
  }

  private static final class MutationKey {

    private final int storeId;
    private final byte[] key;

    private MutationKey(int storeId, byte[] key) {
      this.storeId = storeId;
      this.key = Arrays.copyOf(key, key.length);
    }

    @Override
    public boolean equals(Object other) {
      return this == other || other instanceof MutationKey
          && storeId == ((MutationKey) other).storeId
          && Arrays.equals(key, ((MutationKey) other).key);
    }

    @Override
    public int hashCode() {
      return 31 * storeId + Arrays.hashCode(key);
    }
  }
}
