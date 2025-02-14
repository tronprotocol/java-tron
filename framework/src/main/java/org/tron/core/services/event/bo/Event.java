package org.tron.core.services.event.bo;

import lombok.Data;

@Data
public class Event {
  private boolean isRemove;
  private BlockEvent blockEvent;

  public Event(BlockEvent blockEvent, boolean isRemove) {
    this.blockEvent = blockEvent;
    this.isRemove = isRemove;
  }
}
