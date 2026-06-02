package org.tron.core.services.jsonrpc.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString
public class SimulateBlock {

  @Getter
  @Setter
  private List<CallArguments> calls;

  @Getter
  @Setter
  private Object blockOverrides;

  @Getter
  @Setter
  private Object stateOverrides;

  private final Map<String, Object> unknown = new HashMap<>();

  @JsonAnySetter
  public void putUnknown(String key, Object value) {
    unknown.put(key, value);
  }

  @JsonAnyGetter
  public Map<String, Object> getUnknown() {
    return unknown;
  }
}
