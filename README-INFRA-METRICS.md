# TRON Prometheus Infrastructure Metrics

**PR for [java-tron Issue #6590](https://github.com/tronprotocol/java-tron/issues/6590)** |
**Penn Blockchain Conference Hackathon 2026 — TRON Bounty 2**

---

## What This PR Does

Implements Prometheus metrics for empty block detection and SR set change monitoring, addressing critical operational blind spots in java-tron's monitoring infrastructure.

## New Metrics Reference

| Metric Name | Type | Labels | Description |
|-------------|------|--------|-------------|
| `tron:block_transaction_count` | Histogram | `miner` | Distribution of transaction counts per block |
| `tron:sr_set_change_total` | Counter | `witness`, `change_type` | SR set changes (added/removed) |

### Empty Blocks via Histogram

Query empty blocks using the histogram's `le="0.0"` bucket:

```promql
# Empty blocks count by miner
tron:block_transaction_count_bucket{le="0.0"}

# Empty block ratio
rate(tron:block_transaction_count_bucket{le="0.0"}[1h]) / rate(tron:block_transaction_count_count[1h])
```

### Histogram Buckets

`[0, 10, 50, 100, 200, 500, 1000, 2000, 5000, 10000]`

### Label Values for `tron:sr_set_change_total`

| Label | Value | Description |
|-------|-------|-------------|
| `change_type` | `added` | A new SR entered the active set |
| `change_type` | `removed` | An existing SR left the active set |
| `witness` | base58 address | The SR address affected |

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
rate(tron:block_transaction_count_bucket{le="0.0"}[1m])
```

### Empty Block Ratio (last hour)

```promql
rate(tron:block_transaction_count_bucket{le="0.0"}[1h]) / rate(tron:block_transaction_count_count[1h])
```

### Total Empty Blocks by Miner

```promql
tron:block_transaction_count_bucket{le="0.0"}
```

### Transaction Count Distribution

```promql
# Blocks with 0-10 transactions
tron:block_transaction_count_bucket{le="10"} - tron:block_transaction_count_bucket{le="0"}

# Average transactions per block
rate(tron:block_transaction_count_sum[5m]) / rate(tron:block_transaction_count_count[5m])
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
rate(tron:block_transaction_count_bucket{le="0.0"}[5m]) > 10
```

### Alert: SR Set Changed

```promql
increase(tron:sr_set_change_total[1h]) > 0
```

## Files Modified

| File | Change |
|------|--------|
| `common/src/main/java/org/tron/common/prometheus/MetricKeys.java` | Removed `BLOCK_EMPTY`, added `BLOCK_TRANSACTION_COUNT` histogram constant |
| `common/src/main/java/org/tron/common/prometheus/MetricsCounter.java` | Removed `BLOCK_EMPTY` counter registration |
| `common/src/main/java/org/tron/common/prometheus/MetricsHistogram.java` | Added overloaded `init()` for custom buckets, registered `BLOCK_TRANSACTION_COUNT` |
| `framework/src/main/java/org/tron/core/metrics/blockchain/BlockChainMetricManager.java` | Replaced counter with `histogramObserve()` for all blocks, kept SR counter |
| `framework/src/test/java/org/tron/core/metrics/prometheus/PrometheusApiServiceTest.java` | Updated tests for histogram bucket queries |

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

### Block Transaction Count Histogram

Records transaction count for **all blocks** (including empty blocks):

```java
int txCount = block.getTransactions().size();
Metrics.histogramObserve(MetricKeys.Histogram.BLOCK_TRANSACTION_COUNT, txCount,
    StringUtil.encode58Check(address));
```

Benefits over simple counter:
- **Rich insights**: Tracks full distribution of tx counts
- **Flexible queries**: Percentiles, trends, specific ranges
- **Empty block detection**: Via `le="0.0"` bucket

### SR Set Change Detection

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
- Uses existing `Metrics.histogramObserve()` pattern for histogram
- Uses existing `Metrics.counterInc()` pattern for counter
- All constants defined in `MetricKeys.java` (no hardcoded strings)
- Java 8 compatible
- No new Gradle dependencies

## Why Histogram for Empty Blocks?

The histogram approach (as suggested by Sunny6889) provides richer insights:

| Approach | Pros |
|----------|------|
| Counter | Simple, single-purpose |
| **Histogram** | Tracks distribution, enables ratio queries, supports percentiles |

Example queries enabled by histogram:
- Empty block ratio over any time window
- Transaction distribution patterns
- Block capacity utilization

## Related Issues

- [java-tron #6590](https://github.com/tronprotocol/java-tron/issues/6590) — Prometheus metrics for empty blocks and SR changes
