package org.tron.common.zksnark.sapling.zip32;

import java.util.List;
import lombok.Getter;

public class HDSeed {

  @Getter
  public RawHDSeed seed;

  public static class RawHDSeed {

    @Getter
    public byte[] data ;
  }
}
