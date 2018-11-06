package org.tron.common.zksnark.merkle;

import java.util.List;
import lombok.Getter;


public class MerklePath {

  @Getter
  List<List<Boolean>> authentication_path;

  @Getter
  List<Boolean> index;

  public MerklePath(List<List<Boolean>> authentication_path, List<Boolean> index) {
    this.authentication_path = authentication_path;
    this.index = index;
  }

  public List<List<Boolean>> getAuthenticationPath() {
    return this.authentication_path;
  }

  public List<Boolean> getIndex() {
    return this.index;
  }
}
