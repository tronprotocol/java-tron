# TRON Prometheus Infrastructure Metrics

**PR for [java-tron Issue #6590](https://github.com/tronprotocol/java-tron/issues/6590)** |
**Penn Blockchain Conference Hackathon 2026 — TRON Bounty 2**

---

## What This PR Does

Adds two new Prometheus counters to java-tron's metrics system, enabling node operators to monitor chain health events that are otherwise invisible at the metrics layer.

## New Metrics Reference

| Metric Name | Type | Labels | Description |
|-------------|------|--------|-------------|
| `tron:block_empty_total` | Counter | `type="empty"` | Increments each time a block with zero transactions is applied to the chain |
| `tron:sr_set_change_total` | Counter | `witness`, `change_type` | Increments when the Super Representative set changes during a maintenance period |

### Label Values for `tron:sr_set_change_total`

| Label | Value | Description |
|-------|-------|-------------|
| `change_type` | `added` | A new SR entered the active set |
| `change_type` | `removed` | An existing SR left the active set |
| `witness` | hex address | The SR address affected |

## Setup Instructions

### Enable Prometheus Metrics

In your node's `config.conf`:

```hocon
node {
  metricsPrometheusEnable = true
}
```

Or via CLI flag:

```bash
java -jar FullNode.jar --metrics-prometheus-enable
```

### Prometheus Endpoint

When enabled, metrics are available at:

```
http://localhost:9527/metrics
```

### Prometheus Configuration

Add to your `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'tron-node'
    static_configs:
      - targets: ['localhost:9527']
    metrics_path: '/metrics'
```

## PromQL Example Queries

### Empty Block Rate (per minute)

```promql
rate(tron:block_empty_total[1m])
```

### Total Empty Blocks

```promql
tron:block_empty_total
```

### SR Set Changes Over Time

```promql
rate(tron:sr_set_change_total[5m])
```

### SRs Added vs Removed

```promql
# Added
sum by (change_type) (tron:sr_set_change_total{change_type="added"})

# Removed
sum by (change_type) (tron:sr_set_change_total{change_type="removed"})
```

### Alert: High Empty Block Rate

```promql
rate(tron:block_empty_total[5m]) > 10
```

### Alert: SR Set Changed

```promql
increase(tron:sr_set_change_total[1h]) > 0
```

## Files Modified

| File | Change |
|------|--------|
| `common/src/main/java/org/tron/common/prometheus/MetricKeys.java` | Added `BLOCK_EMPTY` and `SR_SET_CHANGE` constants |
| `common/src/main/java/org/tron/common/prometheus/MetricLabels.java` | Added `BLOCK_EMPTY`, `SR_ADDED`, `SR_REMOVED` label value constants |
| `common/src/main/java/org/tron/common/prometheus/MetricsCounter.java` | Registered both counters with Prometheus |
| `framework/src/main/java/org/tron/core/metrics/blockchain/BlockChainMetricManager.java` | Added empty block and SR set change detection in `applyBlock()` |
| `framework/src/test/java/org/tron/core/metrics/prometheus/PrometheusApiServiceTest.java` | Added `testEmptyBlockMetric()` and `testSrSetChangeMetric()` tests |

## Build & Test Commands

```bash
# Build without tests (fast)
./gradlew clean build -x test

# Compile modified source
./gradlew :framework:compileJava :common:compileJava

# Run Prometheus metric tests only
./gradlew :framework:test --tests \
  "org.tron.core.metrics.prometheus.PrometheusApiServiceTest"

# Run all metrics tests
./gradlew :framework:test --tests "org.tron.core.metrics.*"

# Full test suite
./gradlew test

# Coverage report
./gradlew :framework:jacocoTestReport
# Report: framework/build/reports/jacoco/test/html/index.html
```

## Implementation Details

### Empty Block Detection

In `BlockChainMetricManager.applyBlock()`, after the existing TPS counter logic:

```java
if (block.getTransactions().isEmpty()) {
  Metrics.counterInc(MetricKeys.Counter.BLOCK_EMPTY, 1,
      MetricLabels.Counter.BLOCK_EMPTY);
}
```

### SR Set Change Detection

Compares the current active witness list against the previous set, emitting `added` and `removed` labels for each diff:

```java
List<ByteString> currentSrList =
    chainBaseManager.getWitnessScheduleStore().getActiveWitnesses();
Set<String> currentSrSet = currentSrList.stream()
    .map(bs -> Hex.toHexString(bs.toByteArray()))
    .collect(Collectors.toSet());

if (!previousSrSet.isEmpty() && !currentSrSet.equals(previousSrSet)) {
  for (String sr : Sets.difference(currentSrSet, previousSrSet)) {
    Metrics.counterInc(MetricKeys.Counter.SR_SET_CHANGE, 1,
        sr, MetricLabels.Counter.SR_ADDED);
  }
  for (String sr : Sets.difference(previousSrSet, currentSrSet)) {
    Metrics.counterInc(MetricKeys.Counter.SR_SET_CHANGE, 1,
        sr, MetricLabels.Counter.SR_REMOVED);
  }
}
previousSrSet = currentSrSet;
```

### Code Style

- Purely additive — zero protocol changes, zero API changes, zero backward compatibility issues
- Uses existing `Metrics.counterInc()` pattern throughout
- All constants defined in `MetricKeys.java` and `MetricLabels.java` (no hardcoded strings)
- Java 8 compatible (no lambdas, no records)
- No new Gradle dependencies

## Related Issues

- [java-tron #6590](https://github.com/tronprotocol/java-tron/issues/6590) — Prometheus metrics for empty blocks and SR changes
