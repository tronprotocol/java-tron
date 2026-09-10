#!/usr/bin/env bash
set -u

if [[ $# -ne 3 ]]; then
  echo "usage: $0 TARGET_HEAD UNIT OUTPUT_TSV" >&2
  exit 2
fi

target=$1
unit=$2
output=$3
unit_cgroup=${unit%.service}.service
mkdir -p "$(dirname "$output")"
if [[ ! -s "$output" ]]; then
  printf 'epoch\thead\tprocess_count\tpush_count\tpid\trestarts\tmemory_current\tmemory_high\tmemory_max\tpsi_io_some_avg60\tpsi_io_full_avg60\tcpu_pct\trss_kib\n' > "$output"
fi

while :; do
  epoch=$(date +%s)
  metrics=$(curl -fsS --max-time 10 http://127.0.0.1:9527/metrics 2>/dev/null || true)
  head=$(printf '%s\n' "$metrics" | awk '/^tron:header_height / {printf "%.0f", $2; exit}')
  process_count=$(printf '%s\n' "$metrics" | awk '/^tron:block_process_latency_seconds_count\{sync="true"/ {print $2; exit}')
  push_count=$(printf '%s\n' "$metrics" | awk '/^tron:block_push_latency_seconds_count / {print $2; exit}')
  pid=$(systemctl show "$unit" -p MainPID --value 2>/dev/null || printf 0)
  restarts=$(systemctl show "$unit" -p NRestarts --value 2>/dev/null || printf 0)
  cg=/sys/fs/cgroup/system.slice/${unit_cgroup}
  current=$(cat "$cg/memory.current" 2>/dev/null || printf 0)
  high=$(cat "$cg/memory.high" 2>/dev/null || printf 0)
  max=$(cat "$cg/memory.max" 2>/dev/null || printf 0)
  psi_some=$(awk '/^some / {for (i=1;i<=NF;i++) if ($i ~ /^avg60=/) {sub("avg60=", "", $i); print $i}}' /proc/pressure/io)
  psi_full=$(awk '/^full / {for (i=1;i<=NF;i++) if ($i ~ /^avg60=/) {sub("avg60=", "", $i); print $i}}' /proc/pressure/io)
  cpu=0; rss=0
  if [[ "$pid" =~ ^[1-9][0-9]*$ ]]; then
    read -r cpu rss < <(ps -p "$pid" -o %cpu=,rss= 2>/dev/null || echo 0 0)
  fi
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$epoch" "$head" "$process_count" "$push_count" "$pid" "$restarts" \
    "$current" "$high" "$max" "$psi_some" "$psi_full" "$cpu" "$rss" >> "$output"
  if [[ "$head" =~ ^[0-9]+$ ]] && (( head >= target )); then
    break
  fi
  sleep 60
done
