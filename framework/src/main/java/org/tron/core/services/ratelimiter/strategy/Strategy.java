package org.tron.core.services.ratelimiter.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class Strategy {

  @Getter
  protected Map<String, ParamItem> mapParams = new HashMap<>();

  protected Strategy(String paramString) {
    parseStrategyParams(paramString);
  }

  public Map<String, ParamItem> parseStrategyParams(String argString) {

    mapParams = defaultParam();
    try {
      if (!StringUtils.isEmpty(argString)) {
        String pattern = "\\b([^\\s]+)=([^\\s]+)\\b";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(argString);

        while (m.find()) {
          String key = m.group(1);
          String value = m.group(2);
          ParamItem item = mapParams.get(key);
          if (item == null) {
            continue;
          }

          if (item.type == Double.class) {
            double doubleValue = Double.valueOf(value);
            if (doubleValue <= 0) {
              throw new IllegalArgumentException();
            }
            item.setValue(doubleValue);
          } else if (item.type == String.class) {
            item.setValue(value);
          } else if (item.type == Integer.class) {
            item.setValue(Integer.valueOf(value));
          }
        }
      }
    } catch (Exception e) {
      mapParams = defaultParam();
      logger.warn("Strategy params parse failure, use the default strategy params.");
    }

    return mapParams;
  }


  protected abstract Map<String, ParamItem> defaultParam();

  class ParamItem<T> {

    protected Class<T> type;

    @Setter
    protected T value;


    ParamItem(Class<T> type, T value) {
      this.type = type;
      this.value = value;
    }
  }
}