package org.tron.core.zen;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.core.services.http.FullNodeHttpApiService;

@Slf4j(topic = "API")
@Component
public class ZksnarkService {

  @PostConstruct
  private void init() {
    FullNodeHttpApiService.librustzcashInitZksnarkParams();
  }
}
