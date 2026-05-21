package org.tron.core.services.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.UInt64Value;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.tron.protos.Protocol;

/**
 * Tests for {@link JsonFormat#setInt64AsString(boolean)} /
 * {@link JsonFormat#clearInt64AsString()} / {@link JsonFormat#isInt64AsString()}.
 *
 * <p>Tron protos do not define uint64/fixed64 fields directly; all 64-bit values use int64.
 * The uint64 branch is exercised using {@link com.google.protobuf.UInt64Value}, a protobuf
 * well-known wrapper with a single {@code uint64 value} field.
 */
public class JsonFormatInt64AsStringTest {

  /** Defensive cleanup in case a test leaves the ThreadLocal dirty. */
  @After
  public void clearState() {
    JsonFormat.clearInt64AsString();
  }

  @Test
  public void defaultBehaviorUnchangedWhenUnset() {
    Protocol.Account account = Protocol.Account.newBuilder()
        .setBalance(123456789012345L)
        .build();
    String out = JsonFormat.printToString(account, true);
    assertTrue("expected unquoted balance, got: " + out,
        out.contains("\"balance\":123456789012345")
            || out.contains("\"balance\": 123456789012345"));
    assertFalse("balance should not be quoted by default, got: " + out,
        out.contains("\"balance\":\"123456789012345\"")
            || out.contains("\"balance\": \"123456789012345\""));
  }

  @Test
  public void int64FieldQuotedWhenSet() {
    Protocol.Account account = Protocol.Account.newBuilder()
        .setBalance(123456789012345L)
        .build();
    JsonFormat.setInt64AsString(true);
    try {
      String out = JsonFormat.printToString(account, true);
      assertTrue("expected quoted balance, got: " + out,
          out.contains("\"123456789012345\""));
    } finally {
      JsonFormat.clearInt64AsString();
    }
  }

  @Test
  public void uint64FieldQuotedWhenSet() {
    UInt64Value v = UInt64Value.of(9007199254740993L); // 2^53 + 1
    JsonFormat.setInt64AsString(true);
    try {
      String out = JsonFormat.printToString(v, true);
      assertTrue("expected quoted uint64 value, got: " + out,
          out.contains("\"9007199254740993\""));
    } finally {
      JsonFormat.clearInt64AsString();
    }
  }

  @Test
  public void uint64DefaultUnquoted() {
    UInt64Value v = UInt64Value.of(9007199254740993L);
    String out = JsonFormat.printToString(v, true);
    assertTrue("expected unquoted uint64 value, got: " + out,
        out.contains("9007199254740993"));
    assertFalse("uint64 should not be quoted by default, got: " + out,
        out.contains("\"9007199254740993\""));
  }

  @Test
  public void stringBytesEnumNotAffected() {
    // Note: proto3 does not serialize default-valued fields, so enum/bytes fields are
    // set to non-default values to verify they appear in the output.
    Protocol.Account account = Protocol.Account.newBuilder()
        .setAccountName(ByteString.copyFromUtf8("alice"))
        .setType(Protocol.AccountType.AssetIssue)      // non-default enum value
        .setBalance(1L)
        .build();
    JsonFormat.setInt64AsString(true);
    try {
      String out = JsonFormat.printToString(account, true);
      // balance int64 should be quoted
      assertTrue("balance should be quoted, got: " + out, out.contains("\"1\""));
      // enum type serialized by name (not a number), not affected by int64_as_string
      assertTrue("enum type should appear as name, got: " + out,
          out.contains("AssetIssue"));
      // bytes account_name should still serialize normally
      assertTrue("account_name should appear, got: " + out, out.contains("account_name"));
    } finally {
      JsonFormat.clearInt64AsString();
    }
  }

  @Test
  public void nestedInt64FieldsQuoted() {
    Protocol.Block block = Protocol.Block.newBuilder()
        .setBlockHeader(Protocol.BlockHeader.newBuilder()
            .setRawData(Protocol.BlockHeader.raw.newBuilder()
                .setNumber(9007199254740993L)   // 2^53 + 1
                .setTimestamp(1700000000000L)
                .build())
            .build())
        .build();
    JsonFormat.setInt64AsString(true);
    try {
      String out = JsonFormat.printToString(block, true);
      assertTrue("nested number should be quoted, got: " + out,
          out.contains("\"9007199254740993\""));
      assertTrue("nested timestamp should be quoted, got: " + out,
          out.contains("\"1700000000000\""));
    } finally {
      JsonFormat.clearInt64AsString();
    }
  }

  @Test
  public void mapStringInt64ValuesQuoted() {
    Protocol.Account account = Protocol.Account.newBuilder()
        .putAsset("USDT", 123456789012345L)
        .build();
    JsonFormat.setInt64AsString(true);
    try {
      String out = JsonFormat.printToString(account, true);
      assertTrue("map<string,int64> value should be quoted, got: " + out,
          out.contains("\"123456789012345\""));
    } finally {
      JsonFormat.clearInt64AsString();
    }
  }

  @Test
  public void boundaryValuesAllQuoted() {
    // Note: proto3 does not serialize a field whose value equals its type default (0 for int64),
    // so 0L is covered separately via defaultBehaviorUnchangedWhenUnset / uint64DefaultUnquoted
    // (both use non-default values) and does not need an explicit quoted-output test.
    long[] values = {
        (1L << 53) - 1,         // max safe JS integer
        1L << 53,               // boundary
        (1L << 53) + 1,         // first unsafe
        Long.MAX_VALUE,
        Long.MIN_VALUE,
        -1L
    };
    for (long v : values) {
      Protocol.Account account = Protocol.Account.newBuilder().setBalance(v).build();
      JsonFormat.setInt64AsString(true);
      try {
        String out = JsonFormat.printToString(account, true);
        assertTrue("value=" + v + " expected quoted, got: " + out,
            out.contains("\"" + v + "\""));
      } finally {
        JsonFormat.clearInt64AsString();
      }
    }
  }

  @Test
  public void clearResetsState() {
    Protocol.Account account = Protocol.Account.newBuilder().setBalance(1L).build();
    JsonFormat.setInt64AsString(true);
    JsonFormat.clearInt64AsString();
    String out = JsonFormat.printToString(account, true);
    assertFalse("state should be cleared, got: " + out, out.contains("\"1\""));
  }

  @Test
  public void clearInFinallySurvivesException() {
    Protocol.Account account = Protocol.Account.newBuilder().setBalance(1L).build();
    JsonFormat.setInt64AsString(true);
    try {
      throw new RuntimeException("boom");
    } catch (RuntimeException expected) {
      // expected
    } finally {
      JsonFormat.clearInt64AsString();
    }
    String out = JsonFormat.printToString(account, true);
    assertFalse("state leaked after exception, got: " + out, out.contains("\"1\""));
  }

  @Test
  public void isInt64AsStringReflectsCurrentState() {
    assertFalse(JsonFormat.isInt64AsString());
    JsonFormat.setInt64AsString(true);
    try {
      assertTrue(JsonFormat.isInt64AsString());
    } finally {
      JsonFormat.clearInt64AsString();
    }
    assertFalse(JsonFormat.isInt64AsString());
  }

  @Test
  public void threadIsolation() throws Exception {
    final Protocol.Account account = Protocol.Account.newBuilder().setBalance(1L).build();
    final CountDownLatch barrier = new CountDownLatch(2);
    ExecutorService ex = Executors.newFixedThreadPool(2);
    try {
      Future<String> trueThread = ex.submit(() -> {
        JsonFormat.setInt64AsString(true);
        try {
          barrier.countDown();
          barrier.await();
          return JsonFormat.printToString(account, true);
        } finally {
          JsonFormat.clearInt64AsString();
        }
      });
      Future<String> falseThread = ex.submit(() -> {
        barrier.countDown();
        barrier.await();
        return JsonFormat.printToString(account, true);
      });
      String withSet = trueThread.get(5, TimeUnit.SECONDS);
      String noSet = falseThread.get(5, TimeUnit.SECONDS);
      assertTrue("trueThread should see quoted: " + withSet,
          withSet.contains("\"1\""));
      assertFalse("falseThread should see unquoted: " + noSet,
          noSet.contains("\"1\""));
    } finally {
      ex.shutdownNow();
    }
  }

  @Test
  public void noPollutionOnThreadReuse() throws Exception {
    final Protocol.Account account = Protocol.Account.newBuilder().setBalance(1L).build();
    ExecutorService single = Executors.newSingleThreadExecutor();
    try {
      Future<String> firstRun = single.submit(() -> {
        JsonFormat.setInt64AsString(true);
        try {
          return JsonFormat.printToString(account, true);
        } finally {
          JsonFormat.clearInt64AsString();
        }
      });
      assertTrue(firstRun.get(5, TimeUnit.SECONDS).contains("\"1\""));

      // Reuse the same thread; without a new set, state must be cleared.
      Future<String> secondRun = single.submit(() -> JsonFormat.printToString(account, true));
      String second = secondRun.get(5, TimeUnit.SECONDS);
      assertFalse("thread reuse leaked quoted state: " + second,
          second.contains("\"1\""));
    } finally {
      single.shutdownNow();
    }
  }
}
