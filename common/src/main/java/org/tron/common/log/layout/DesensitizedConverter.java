package org.tron.common.log.layout;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;

@Slf4j(topic = "Parser")
public class DesensitizedConverter extends ClassicConverter {

  private static final int SENSITIVE_WORD_SIZE = 1_000;

  private static final Pattern pattern = Pattern.compile(
      "(((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|"
          + "([1-9]?\\d))))");

  private static final Cache<String, String> sensitiveCache = CacheBuilder.newBuilder()
      .maximumSize(SENSITIVE_WORD_SIZE)
      .recordStats().build();

  public static void addSensitive(String key, String value) {
    sensitiveCache.put(key, value);
  }

  private String desensitization(String content) {
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      String key = matcher.group();
      String value = sensitiveCache.getIfPresent(key);
      if (value != null) {
        content = content.replaceAll(key, value);
      } else {
        content = content.replaceAll(key, "IP");
      }
    }

    return content;
  }

  @Override
  public String convert(ILoggingEvent iLoggingEvent) {
    String source = iLoggingEvent.getFormattedMessage();
    return CommonParameter.getInstance().isFastForward() ? desensitization(source) : source;
  }
}
