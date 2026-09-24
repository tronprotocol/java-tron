package org.tron.common.logsfilter.queue;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.tron.common.logsfilter.capsule.FilterTriggerCapsule;

/**
 * Queue between the block-processing producer (Manager) and the json-rpc filter
 * consumer (TronJsonRpcImpl), so that neither side references the other.
 */
@Component
public class FilterCapsuleQueue {

  private final BlockingQueue<FilterTriggerCapsule> queue = new LinkedBlockingQueue<>();

  public boolean offer(FilterTriggerCapsule capsule) {
    return queue.offer(capsule);
  }

  public FilterTriggerCapsule poll(long timeout, TimeUnit unit) throws InterruptedException {
    return queue.poll(timeout, unit);
  }

  @VisibleForTesting
  public Stream<FilterTriggerCapsule> stream() {
    return queue.stream();
  }
}
