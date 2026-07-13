Metrics Changelog
=================

This file tracks Prometheus metric additions, changes, and removals in java-tron. For the full set of metrics emitted today, see the references at the bottom.

**4.8.2**

### New Metrics

#### Core

- `tron:block_transaction_count` (Histogram, label `miner`) — per-block transaction count, sampled at the entry of `Manager#pushBlock` before any early return so duplicate, stale, and fork-switched pushes are observed alongside applied blocks. Primary use cases: empty-block detection per super representative, and per-SR TPS / throughput percentile interpolation. Default buckets `[0, 20, 50, 80, 100, 120, 140, 160, 180, 200, 230, 260, 300, 500, 2000, 5000, 10000]` are densified around 0–300 for percentile interpolation in the typical TPS range; 5000 and 10000 are retained as safety-net buckets to preserve resolution for outlier events such as stress tests or repush storms. ([#6624](https://github.com/tronprotocol/java-tron/pull/6624))

  > **Operational note:** The effective upper bound is 10000; blocks exceeding that land in `+Inf`. Monitor the overflow ratio — e.g. `(rate(tron_block_transaction_count_bucket{le="+Inf"}[5m]) - rate(tron_block_transaction_count_bucket{le="10000"}[5m])) / rate(tron_block_transaction_count_count[5m]) > 0.01` — as a signal to re-tune the upper bound.

#### Consensus

- `tron:sr_set_change` (Counter, labels `action`, `witness`) — incremented once per witness whenever the active SR set rotates at a maintenance boundary. `action` is one of `add` / `remove`. Cardinality grows with the number of distinct witnesses that have ever entered or left the active set, not with the active set size at any given moment. ([#6624](https://github.com/tronprotocol/java-tron/pull/6624))

**Pre-4.8.2 Baseline**

Snapshot of metrics emitted prior to this changelog. Per-version provenance is not tracked here; consult `git log` on [`common/src/main/java/org/tron/common/prometheus/`](common/src/main/java/org/tron/common/prometheus/) for exact origin of each metric.

### Existing Metrics

#### Core (block / transaction processing)

- `tron:header_height` (Gauge) — latest block height on this node.
- `tron:header_time` (Gauge) — latest block timestamp on this node.
- `tron:block_push_latency_seconds` (Histogram) — `Manager#pushBlock` latency.
- `tron:block_process_latency_seconds` (Histogram, label `sync`) — `TronNetDelegate#processBlock` latency.
- `tron:block_generate_latency_seconds` (Histogram, label `address`) — block generation latency per producer.
- `tron:block_fetch_latency_seconds` (Histogram) — block fetch latency.
- `tron:block_receive_delay_seconds` (Histogram) — `receiveTime - blockTime`.
- `tron:block_fork` (Counter, label `type`) — fork events by type.
- `tron:lock_acquire_latency_seconds` (Histogram, label `type`) — DB / chain lock acquisition latency.
- `tron:miner` (Counter, labels `miner`, `type`) — blocks produced by an SR.
- `tron:miner_latency_seconds` (Histogram, label `miner`) — block mining latency per producer.
- `tron:miner_delay_seconds` (Histogram, label `miner`) — `actualTime - planTime` for block production.
- `tron:txs` (Counter, labels `type`, `detail`) — transaction counts.
- `tron:process_transaction_latency_seconds` (Histogram, labels `type`, `contract`) — transaction processing latency.
- `tron:verify_sign_latency_seconds` (Histogram, label `type`) — signature verification latency for transactions and blocks.
- `tron:tx_cache` (Gauge, label `type`) — transaction cache stats.
- `tron:manager_queue_size` (Gauge, label `type`) — `Manager` queue sizes (pending / popped / queued / repush).

#### Net (P2P)

- `tron:peers` (Gauge, label `type`) — peer counts.
- `tron:p2p_error` (Counter, label `type`) — P2P error events.
- `tron:p2p_disconnect` (Counter, label `type`) — P2P disconnect events.
- `tron:ping_pong_latency_seconds` (Histogram) — peer ping-pong RTT.
- `tron:message_process_latency_seconds` (Histogram, label `type`) — peer message processing latency.
- `tron:tcp_bytes` (Histogram, label `type`) — TCP traffic.
- `tron:udp_bytes` (Histogram, label `type`) — UDP traffic.

#### API

- `tron:http_service_latency_seconds` (Histogram, label `url`) — HTTP endpoint latency.
- `tron:http_bytes` (Histogram, labels `url`, `status`) — HTTP traffic.
- `tron:grpc_service_latency_seconds` (Histogram, label `endpoint`) — gRPC endpoint latency.
- `tron:jsonrpc_service_latency_seconds` (Histogram, label `method`) — JSON-RPC method latency.
- `tron:internal_service_latency_seconds` (Histogram, labels `class`, `method`) — internal service-call latency.
- `tron:internal_service_fail` (Counter, labels `class`, `method`) — internal service-call failure count.

#### DB

- `tron:db_size_bytes` (Gauge, labels `type`, `db`, `level`) — storage size in bytes per engine, database, and level; `type` is the storage engine (`LEVELDB` or `ROCKSDB`) depending on node configuration.
- `tron:db_sst_level` (Gauge, labels `type`, `db`, `level`) — SST files per compaction level per engine and database; `type` is the storage engine (`LEVELDB` or `ROCKSDB`) depending on node configuration.
- `tron:guava_cache_hit_rate` (Gauge, label `type`) — hit rate of a Guava cache; `type` is the cache name.
- `tron:guava_cache_request` (Gauge, label `type`) — total request count of a Guava cache; `type` is the cache name.
- `tron:guava_cache_eviction_count` (Gauge, label `type`) — eviction count of a Guava cache; `type` is the cache name.
- (Registered via `GuavaCacheExports` for caches that opt in to `CacheManager`.)

#### Logging

- `tron:error_info` (Counter, labels `topic`, `type`) — incremented on every ERROR-level log line by `InstrumentedAppender`.

#### System

Emitted by `OperatingSystemExports` (no labels):

- `system_available_cpus`, `process_cpu_load`, `system_cpu_load`, `system_load_average`, `system_total_physical_memory_bytes`, `system_free_physical_memory_bytes`, `system_total_swap_spaces_bytes`, `system_free_swap_spaces_bytes`.

#### JVM / process

Auto-emitted by the Prometheus client library via `DefaultExports.initialize()` (`simpleclient_hotspot`). The full list is owned by the upstream library and not enumerated here; see the [client_java](https://github.com/prometheus/client_java) docs. Common ones: `jvm_memory_bytes_*`, `jvm_gc_collection_seconds_*`, `jvm_threads_*`, `process_cpu_seconds_total`, `process_open_fds`, `process_resident_memory_bytes`.

---

**References**

- [Official metrics documentation](https://tronprotocol.github.io/documentation-en/using_javatron/metrics/) — descriptions, configuration, and example queries.
- [tron-docker `metric_monitor/README.md`](https://github.com/tronprotocol/tron-docker/blob/main/metric_monitor/README.md) — operator-oriented overview with deployment guidance.
- [java-tron-server Grafana dashboard](https://github.com/tronprotocol/tron-docker/blob/main/metric_monitor/grafana_dashboard/java-tron-server.json) — maintained reference dashboard JSON.
