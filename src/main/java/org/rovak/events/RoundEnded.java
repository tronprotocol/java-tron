package org.rovak.events;

import lombok.Getter;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol;

import java.util.List;

public class RoundEnded extends Event {
  private String eventType = "round_ended";
}
