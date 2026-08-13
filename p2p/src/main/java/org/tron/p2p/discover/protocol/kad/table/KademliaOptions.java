package org.tron.p2p.discover.protocol.kad.table;

public class KademliaOptions {
  public static final int BUCKET_SIZE = 16;
  public static final int ALPHA = 3;
  public static final int BINS = 17;
  public static final int MAX_STEPS = 8;
  public static final int MAX_LOOP_NUM = 5;

  public static final long DISCOVER_CYCLE = 7200;       //discovery cycle interval in millis
  public static final long WAIT_TIME = 100;       //wait time in millis
}
