package org.tron.common.zksnark.merkle;

import java.util.List;


public class MerklePath {

  List<List<Boolean>> authentication_path;
  List<Boolean> index;

  public MerklePath(List<List<Boolean>> authentication_path, List<Boolean> index) {
    this.authentication_path = authentication_path;
    this.index = index;
  }

}
