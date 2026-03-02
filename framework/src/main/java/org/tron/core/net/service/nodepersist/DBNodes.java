package org.tron.core.net.service.nodepersist;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class DBNodes {

  @Getter
  @Setter
  private List<DBNode> nodes = new ArrayList<>();
}