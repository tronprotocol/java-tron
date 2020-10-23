package stest.tron.wallet.common.client;

import java.util.Comparator;
import org.tron.protos.Protocol.Witness;

class WitnessComparator implements Comparator {

  public int compare(Object o1, Object o2) {
    return Long.compare(((Witness) o2).getVoteCount(), ((Witness) o1).getVoteCount());
  }
}