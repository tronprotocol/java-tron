package org.tron.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "discover")
public class MessageCount {

  private static int SIZE = 60;

  private int[] szCount = new int[SIZE];

  private long indexTime = System.currentTimeMillis() / 1000;

  private int index = (int) (indexTime % SIZE);

  private long totalCount = 0;

  private void update() {
    long time = System.currentTimeMillis() / 1000;
    long gap = time - indexTime;
    int k = gap > SIZE ? SIZE : (int) gap;
    if (k > 0) {
      for (int i = 1; i <= k; i++) {
        szCount[(index + i) % SIZE] = 0;
      }
      index = (int) (time % SIZE);
      indexTime = time;
    }
  }

  public void add() {
    update();
    szCount[index]++;
    totalCount++;
  }

  public void add(int count) {
    update();
    szCount[index] += count;
    totalCount += count;
  }

  public int getCount(int interval) {
    if (interval > SIZE) {
      logger.warn("Param interval({}) is gt SIZE({})", interval, SIZE);
      return 0;
    }
    update();
    int count = 0;
    for (int i = 0; i < interval; i++) {
      count += szCount[(SIZE + index - i) % SIZE];
    }
    return count;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public void reset() {
    totalCount = 0;
  }

  @Override
  public String toString() {
    return String.valueOf(totalCount);
  }

}
