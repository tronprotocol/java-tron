package org.tron.core.metrics;

import com.codahale.metrics.MetricRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
  @Bean
  public MetricRegistry metricRegistry() {
    return new MetricRegistry();
  }

}
