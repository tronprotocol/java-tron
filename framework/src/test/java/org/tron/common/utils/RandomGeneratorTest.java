package org.tron.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.beust.jcommander.internal.Lists;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.core.capsule.WitnessCapsule;

@Slf4j
public class RandomGeneratorTest {

  private RandomGenerator<Integer> randomGenerator;

  @Before
  public void setUp() {
    randomGenerator = new RandomGenerator<>();
  }

  @Test
  public void testShufflePreservesElements() {
    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
    List<Integer> shuffledList = randomGenerator.shuffle(list, System.currentTimeMillis());

    assertEquals(list.size(), shuffledList.size());
    for (Integer num : list) {
      assertTrue(shuffledList.contains(num));
    }
  }

  @Ignore
  @Test
  public void shuffle() {
    final List<WitnessCapsule> witnessCapsuleListBefore = this.getWitnessList();
    logger.info("updateWitnessSchedule,before: " + getWitnessStringList(witnessCapsuleListBefore));
    final List<WitnessCapsule> witnessCapsuleListAfter = new RandomGenerator<WitnessCapsule>()
        .shuffle(witnessCapsuleListBefore, DateTime.now().getMillis());
    logger.info("updateWitnessSchedule,after: " + getWitnessStringList(witnessCapsuleListAfter));
  }

  private List<WitnessCapsule> getWitnessList() {
    final List<WitnessCapsule> witnessCapsuleList = Lists.newArrayList();
    final WitnessCapsule witnessTron = new WitnessCapsule(
        ByteString.copyFrom("00000000001".getBytes()), 0, "");
    final WitnessCapsule witnessOlivier = new WitnessCapsule(
        ByteString.copyFrom("00000000003".getBytes()), 100, "");
    final WitnessCapsule witnessVivider = new WitnessCapsule(
        ByteString.copyFrom("00000000005".getBytes()), 200, "");
    final WitnessCapsule witnessSenaLiu = new WitnessCapsule(
        ByteString.copyFrom("00000000006".getBytes()), 300, "");
    witnessCapsuleList.add(witnessTron);
    witnessCapsuleList.add(witnessOlivier);
    witnessCapsuleList.add(witnessVivider);
    witnessCapsuleList.add(witnessSenaLiu);
    return witnessCapsuleList;
  }

  private List<String> getWitnessStringList(List<WitnessCapsule> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()))
        .collect(Collectors.toList());
  }
}