package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.WitnessStore;

@Slf4j
public class WitnessStoreTest extends BaseTest {

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Resource
  private WitnessStore witnessStore;

  @Test
  public void putAndGetWitness() {
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFromUtf8("100000000x"), 100L,
        "");

    this.witnessStore.put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
    WitnessCapsule witnessSource = this.witnessStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(witnessCapsule.getAddress(), witnessSource.getAddress());
    Assert.assertEquals(witnessCapsule.getVoteCount(), witnessSource.getVoteCount());

    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), witnessSource.getAddress());
    Assert.assertEquals(100L, witnessSource.getVoteCount());

    witnessCapsule = new WitnessCapsule(ByteString.copyFromUtf8(""), 100L, "");

    this.witnessStore.put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
    witnessSource = this.witnessStore.get(ByteString.copyFromUtf8("").toByteArray());
    Assert.assertEquals(witnessCapsule.getAddress(), witnessSource.getAddress());
    Assert.assertEquals(witnessCapsule.getVoteCount(), witnessSource.getVoteCount());

    Assert.assertEquals(ByteString.copyFromUtf8(""), witnessSource.getAddress());
    Assert.assertEquals(100L, witnessSource.getVoteCount());
  }

  @Test
  public void testSortWitness() {
    this.witnessStore.reset();
    WitnessCapsule s1 = new WitnessCapsule(
        ByteString.copyFrom(new byte[]{1, 2, 3}), 100L, "URL-1");
    this.witnessStore.put(s1.getAddress().toByteArray(), s1);
    WitnessCapsule s2 = new WitnessCapsule(
        ByteString.copyFrom(new byte[]{1, 1, 34}), 100L, "URL-2");
    this.witnessStore.put(s2.getAddress().toByteArray(), s2);
    List<WitnessCapsule> allWitnesses = this.witnessStore.getAllWitnesses();
    List<ByteString> witnessAddress = allWitnesses.stream().map(WitnessCapsule::getAddress)
        .collect(Collectors.toList());
    this.witnessStore.sortWitness(witnessAddress, false);
    this.witnessStore.sortWitnesses(allWitnesses, false);
    Assert.assertEquals(witnessAddress, allWitnesses.stream().map(WitnessCapsule::getAddress)
        .collect(Collectors.toList()));
    List<ByteString> pre = new ArrayList<>(witnessAddress);
    this.witnessStore.sortWitness(witnessAddress, true);
    this.witnessStore.sortWitnesses(allWitnesses, true);
    Assert.assertEquals(witnessAddress, allWitnesses.stream().map(WitnessCapsule::getAddress)
        .collect(Collectors.toList()));
    Assert.assertNotEquals(pre, witnessAddress);
  }
}