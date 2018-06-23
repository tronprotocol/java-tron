package org.rovak.events;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol;

import java.util.List;

public abstract class Event {

  @Getter
  @Setter
  private Long block;


  @Getter
  @Setter
  private Long timestamp;
}
