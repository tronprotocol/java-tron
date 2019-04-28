package org.tron.core.zen.note;

import java.util.List;
import java.util.Optional;
import org.tron.core.zen.merkle.IncrementalMerkleVoucherContainer;
import org.tron.core.zen.address.IncomingViewingKey;

public class NoteData {

  public List<IncrementalMerkleVoucherContainer> vouchers;
  public int witnessHeight;
  public IncomingViewingKey ivk;
  public Optional<byte[]> nullifier; // 256
}
