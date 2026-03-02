package org.tron.core.services.event.bo;

import lombok.Getter;
import lombok.Setter;

public class Event {
  @Getter
  @Setter
  private boolean isRemove;
  @Getter
  @Setter
  private BlockEvent blockEvent;

  public Event(BlockEvent blockEvent, boolean isRemove) {
    this.blockEvent = blockEvent;
    this.isRemove = isRemove;
  }
}
