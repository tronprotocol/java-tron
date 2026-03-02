package org.tron.core.config.args;

import java.net.InetSocketAddress;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class SeedNode {

  @Getter
  @Setter
  private List<InetSocketAddress> addressList;
}
