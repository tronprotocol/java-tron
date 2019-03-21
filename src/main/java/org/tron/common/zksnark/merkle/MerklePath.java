package org.tron.common.zksnark.merkle;

import java.util.List;
import lombok.Getter;

public class MerklePath {

  @Getter
  List<List<Boolean>> authenticationPath;

  @Getter List<Boolean> index;

  public MerklePath(List<List<Boolean>> authenticationPath, List<Boolean> index) {
    this.authenticationPath = authenticationPath;
    this.index = index;
  }

  public List<List<Boolean>> getAuthenticationPath() {
    return this.authenticationPath;
  }

  public List<Boolean> getIndex() {
    return this.index;
  }

  //todo:
  public byte[] encode() {

    return null;
  }
}
