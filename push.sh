#!/bin/bash

# 指标抓取源地址和目标地址
METRICS_URL="http://localhost:9527/metrics"
VICTORIA_METRICS_URL="http://34.238.245.38:8428/api/v1/import/prometheus"
EXTRA_LABELS="extra_label=group=tron-mainnet&extra_label=instance=solidity001&extra_label=job=java-tron"
while true; do
  # 直接推送完整数据（VictoriaMetrics 可以处理大体积数据）
  curl -s "$METRICS_URL" | \
  curl -X POST \
      --data-binary @- \
      -H "Content-Type: text/plain" \
      "${VICTORIA_METRICS_URL}?${EXTRA_LABELS}"
  sleep 1
done