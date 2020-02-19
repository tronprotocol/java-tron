package org.tron.core.services.monitor;

import com.codahale.metrics.MetricRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitorConfig {
  @Bean
  public MetricRegistry metricRegistry() {
    return new MetricRegistry();
  }

}
