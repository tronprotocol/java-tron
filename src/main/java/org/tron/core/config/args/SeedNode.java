package org.tron.core.config.args;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class SeedNode {

  @Getter
  @Setter
  private List<String> ipList;
}
