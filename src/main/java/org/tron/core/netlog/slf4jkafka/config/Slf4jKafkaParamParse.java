package org.tron.core.netlog.slf4jkafka.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.netlog.slf4jkafka.kafkaproducer.KafkaProducerContext;

@Slf4j(topic = "kafka-log")
public enum Slf4jKafkaParamParse {

  INSTANCE;

  public static Properties parse(String params) {
    Properties props = KafkaProducerContext.getDefaultProperties();

    try {
      if (!StringUtils.isEmpty(params)) {
        Map<String, String> map = new HashMap<>();
        String pattern = "\\b([^\\s]+)=([^\\s]+)\\b";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(params);

        while (m.find()) {
          map.put(m.group(1), m.group(2));
        }

        Set<String> keys = map.keySet();
        for (String key : keys) {
          props.setProperty(key, map.get(key));
        }
      }
    } catch (Exception e) {
      logger.warn("kafka params parse failure.");
    }

    return props;
  }
}
