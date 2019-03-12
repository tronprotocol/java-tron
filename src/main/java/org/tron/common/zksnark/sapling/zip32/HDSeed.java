package org.tron.common.zksnark.sapling.zip32;

import java.util.List;
import lombok.Getter;

public class HDSeed {

  @Getter
  public RawHDSeed seed;

  public class RawHDSeed {

    @Getter
    public List data;
  }
}
