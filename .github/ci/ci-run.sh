#!/usr/bin/env bash
# Run one CI stage and keep enough evidence to explain failures that have no
# failing test case: JVM crashes, Java or kernel OOM, killed Gradle workers,
# non-test Gradle task failures, timeouts and cancellations.
#
# Usage: ci-run.sh <stage-name> <command> [args...]
#
# Evidence for each stage goes to its own directory, $CI_DIAG_ROOT/<stage>
# (default: ./ci-logs/<stage>), so a later stage never reports an earlier
# stage's crash files. Set CI_DIAG_ANNOTATION=warning for steps whose failures
# are tolerated.
#
# The command's exit status is preserved: diagnostics never turn a failure
# into a success or the other way around. The report separates what was
# observed from what it may mean. Compatible with bash 3.2 (macOS runners) and
# minimal images without findutils or procps.

set -uo pipefail

if [ "$#" -lt 2 ]; then
  echo "usage: $0 <stage-name> <command> [args...]" >&2
  exit 2
fi

STAGE=$1
shift
case "$STAGE" in
  *[!A-Za-z0-9_.-]* | '' | .*)
    echo "invalid stage name: $STAGE" >&2
    exit 2 ;;
esac

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=${CI_DIAG_ROOT:-$PWD/ci-logs}
DIAG_DIR=$ROOT/$STAGE
n=2
while [ -e "$DIAG_DIR" ]; do
  DIAG_DIR=$ROOT/$STAGE-$n
  n=$((n + 1))
done
mkdir -p "$DIAG_DIR"
DIAG_DIR=$(cd "$DIAG_DIR" && pwd)
export CI_DIAG_DIR=$DIAG_DIR

CONSOLE_LOG=$DIAG_DIR/console.log
MEM_LOG=$DIAG_DIR/memory.log
JVM_LOG=$DIAG_DIR/jvms.log
JVM_LAST=$DIAG_DIR/jvms-last.txt
STATUS_FILE=$DIAG_DIR/status.txt
REPORT=$DIAG_DIR/diagnosis.md
MARKER=$DIAG_DIR/.start
SAMPLE_SECONDS=${CI_MEM_SAMPLE_SECONDS:-15}
OS=$(uname -s)

# Write HotSpot fatal error logs to this stage's directory. JAVA_TOOL_OPTIONS
# reaches every JVM started below, including the single-use Gradle daemon,
# Gradle test workers and compiler daemons.
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-XX:ErrorFile=$DIAG_DIR/hs_err_pid%p.log"

log() {
  echo "[ci-diag] $*"
}

warn_diagnostics() {
  local message=$1
  log "warning: $message" >&2
  if [ -n "${GITHUB_ACTIONS:-}" ]; then
    printf '::warning title=CI diagnostics (%s)::%s\n' "$STAGE" "$message"
  fi
}

now() {
  date -u +%H:%M:%S
}

# ------------------------------------------------------------------ cgroups

# Resolve this shell's memory cgroup through /proc/self/mountinfo so nested
# cgroups and non-root mounts map to the right directory. Leaves CG_DIR empty
# when the mapping is ambiguous; the report then says "unavailable" rather
# than reading an unrelated cgroup.
CG_DIR=""
CG_VERSION=""

cgroup_path_under_mount() {
  local path=$1 root=$2 mnt=$3
  if [ "$path" = "$root" ]; then
    echo "$mnt"
  elif [ "$root" = / ]; then
    echo "$mnt$path"
  else
    case "$path" in
      "$root"/*) echo "$mnt${path#"$root"}" ;;
    esac
  fi
}

resolve_cgroup() {
  [ -r /proc/self/cgroup ] && [ -r /proc/self/mountinfo ] || return 0
  local path mount dir
  path=$(awk -F: '$1 == "0" && $2 == "" { print $3; exit }' /proc/self/cgroup)
  if [ -n "$path" ]; then
    mount=$(awk '{ for (i = 7; i <= NF; i++) if ($i == "-") {
        if ($(i + 1) == "cgroup2") print $4, $5; break } }' /proc/self/mountinfo | head -1)
    if [ -n "$mount" ]; then
      dir=$(cgroup_path_under_mount "$path" "${mount%% *}" "${mount#* }")
      if [ -n "$dir" ] && [ -f "$dir/memory.current" ]; then
        CG_DIR=$dir
        CG_VERSION=2
        return 0
      fi
    fi
  fi
  # cgroup v1, or a hybrid host whose v2 hierarchy has no memory controller.
  path=$(awk -F: '$2 ~ /(^|,)memory(,|$)/ { print $3; exit }' /proc/self/cgroup)
  [ -n "$path" ] || return 0
  mount=$(awk '{ for (i = 7; i <= NF; i++) if ($i == "-") {
      if ($(i + 1) == "cgroup" && $(i + 3) ~ /(^|,)memory(,|$)/) print $4, $5; break } }' \
      /proc/self/mountinfo | head -1)
  [ -n "$mount" ] || return 0
  dir=$(cgroup_path_under_mount "$path" "${mount%% *}" "${mount#* }")
  if [ -n "$dir" ] && [ -f "$dir/memory.usage_in_bytes" ]; then
    CG_DIR=$dir
    CG_VERSION=1
  fi
}

read_mb() {
  awk '{ if ($1 == "max" || $1 >= 9e18) print "max"; else print int($1 / 1048576) "M" }' \
    "$1" 2>/dev/null
}

cg_usage() {
  case "$CG_VERSION" in
    2) read_mb "$CG_DIR/memory.current" ;;
    1) read_mb "$CG_DIR/memory.usage_in_bytes" ;;
    *) echo "-" ;;
  esac
}

cg_limit() {
  case "$CG_VERSION" in
    2) read_mb "$CG_DIR/memory.max" ;;
    1) read_mb "$CG_DIR/memory.limit_in_bytes" ;;
    *) echo "-" ;;
  esac
}

cg_oom_kill() {
  local v=""
  case "$CG_VERSION" in
    2) v=$(awk '$1 == "oom_kill" { print $2 }' "$CG_DIR/memory.events" 2>/dev/null) ;;
    1) v=$(awk '$1 == "oom_kill" { print $2 }' "$CG_DIR/memory.oom_control" 2>/dev/null) ;;
  esac
  echo "${v:--}"
}

# Kernel-wide counter: on a hosted runner it covers the whole VM, so it may
# include processes outside this job.
host_oom_kill() {
  local v
  v=$(awk '$1 == "oom_kill" { print $2 }' /proc/vmstat 2>/dev/null)
  echo "${v:--}"
}

snapshot_memory() {
  local f
  {
    echo "== $1 $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    if [ "$OS" = Linux ]; then
      grep -E '^(MemTotal|MemAvailable|SwapTotal|SwapFree):' /proc/meminfo
      echo "host oom_kill: $(host_oom_kill)"
      if [ -n "$CG_DIR" ]; then
        echo "cgroup v$CG_VERSION: $CG_DIR"
        for f in memory.max memory.current memory.peak memory.events memory.swap.max \
            memory.swap.current memory.limit_in_bytes memory.usage_in_bytes \
            memory.max_usage_in_bytes memory.failcnt memory.oom_control; do
          [ -r "$CG_DIR/$f" ] && echo "$f: $(tr '\n' ' ' < "$CG_DIR/$f")"
        done
      else
        echo "cgroup: unavailable (memory cgroup of this process could not be resolved)"
      fi
    else
      sysctl -n hw.memsize 2>/dev/null | awk '{ printf "hw.memsize: %d MB\n", $1 / 1048576 }'
      sysctl vm.swapusage 2>/dev/null
      memory_pressure 2>/dev/null | tail -1
    fi
  } >> "$MEM_LOG" 2>&1
}

# ----------------------------------------------------------------- JVM list

java_role() {
  case "$1" in
    *"Gradle Test Executor"*)
      echo "$1" | grep -o 'Gradle Test Executor [0-9]*' | head -1 | tr ' ' '_' ;;
    *GradleDaemon*) echo GradleDaemon ;;
    *GradleWrapperMain*) echo GradleWrapper ;;
    *GradleWorkerMain*) echo GradleWorker ;;
    *KotlinCompileDaemon*) echo KotlinDaemon ;;
    *) echo java ;;
  esac
}

# Prints "<pid> <rss-kb> <role>" for every running JVM.
java_processes() {
  if [ "$OS" = Linux ]; then
    local d rss cmd
    for d in /proc/[0-9]*; do
      [ "$(cat "$d/comm" 2>/dev/null)" = java ] || continue
      rss=$(awk '/^VmRSS:/ { print $2 }' "$d/status" 2>/dev/null)
      cmd=$(tr '\0' ' ' < "$d/cmdline" 2>/dev/null)
      echo "${d#/proc/} ${rss:-0} $(java_role "$cmd")"
    done
  else
    ps -axo pid=,rss=,command= 2>/dev/null | while read -r pid rss cmd; do
      case "$cmd" in
        */bin/java\ * | java\ *) echo "$pid $rss $(java_role "$cmd")" ;;
      esac
    done
  fi
}

memory_sampler() {
  local prev=$DIAG_DIR/.jvms.prev cur=$DIAG_DIR/.jvms.cur i=0 ts line every
  every=$((60 / SAMPLE_SECONDS))
  [ "$every" -gt 0 ] || every=1
  : > "$prev"
  while :; do
    ts=$(now)
    java_processes | LC_ALL=C sort > "$cur"
    # Full PID/role/RSS map on every sample, so a JVM that vanishes can be
    # identified even if it died right after the previous sample.
    awk -v ts="$ts" '{ printf "%s %s %s %dM\n", ts, $1, $3, $2 / 1024 }' "$cur" >> "$JVM_LOG"
    { echo "# $ts"; cat "$cur"; } > "$JVM_LAST"
    LC_ALL=C join -v1 "$prev" "$cur" 2>/dev/null | while read -r pid rss role; do
      echo "$ts EXIT pid=$pid role=$role last_rss=$((rss / 1024))M" >> "$MEM_LOG"
    done
    line="$ts"
    if [ "$OS" = Linux ]; then
      line="$line avail=$(awk '/^MemAvailable:/ { print int($2 / 1024) }' /proc/meminfo)M"
      line="$line cgroup=$(cg_usage)/$(cg_limit) cgroup_oom_kill=$(cg_oom_kill)"
      line="$line host_oom_kill=$(host_oom_kill)"
    fi
    line="$line jvm_total=$(awk '{ s += $2 } END { print int(s / 1024) }' "$cur")M"
    line="$line top=$(sort -k2 -nr "$cur" | head -4 \
      | awk '{ printf "%s%s:%dM", sep, $3, $2 / 1024; sep = "," }')"
    echo "$line" >> "$MEM_LOG"
    # Stream a trend line to the job log once a minute; it survives even when
    # the artifact upload never runs.
    [ $((i % every)) -eq 0 ] && log "mem $line"
    i=$((i + 1))
    mv "$cur" "$prev"
    sleep "$SAMPLE_SECONDS"
  done
}

# --------------------------------------------------------------- diagnosis

collect_files() {
  local f gradle_home=${GRADLE_USER_HOME:-$HOME/.gradle}
  # Fallback locations HotSpot uses when ErrorFile cannot be written.
  for f in hs_err_pid*.log */hs_err_pid*.log /tmp/hs_err_pid*.log; do
    [ -f "$f" ] && [ "$f" -nt "$MARKER" ] && cp -p "$f" "$DIAG_DIR/" 2>/dev/null
  done
  # Daemon logs of this stage only; tail them because they repeat build output.
  for f in "$gradle_home"/daemon/*/daemon-*.out.log; do
    [ -f "$f" ] && [ "$f" -nt "$MARKER" ] \
      && tail -n 20000 "$f" > "$DIAG_DIR/gradle-$(basename "$f")"
  done
}

signal_name() {
  case "$1" in
    6) echo SIGABRT ;;
    9) echo SIGKILL ;;
    11) echo SIGSEGV ;;
    15) echo SIGTERM ;;
    *) echo "signal $1" ;;
  esac
}

exit_text() {
  if [ "$1" -gt 128 ] && [ "$1" -lt 160 ]; then
    echo "$1 (128+$(($1 - 128)), $(signal_name $(($1 - 128))))"
  else
    echo "$1"
  fi
}

# One line per fatal-error log: owning JVM role, fatal reason, problematic frame.
hs_err_summary() {
  local f role
  for f in "$DIAG_DIR"/hs_err_pid*.log; do
    [ -f "$f" ] || continue
    role=$(java_role "$(grep -m1 '^Command Line:' "$f" 2>/dev/null)")
    awk -v file="${f##*/}" -v role="$role" '
      !/^#/ { exit }
      frame_next { frame = substr($0, 3); frame_next = 0; next }
      /^# Problematic frame:/ { frame_next = 1; next }
      /^#  (Possible|  )/ { next }
      /^#  [^ ]/ || /^# (There is insufficient memory|Native memory allocation|Out of Memory Error)/ {
        if (n < 2) { sub(/^# +/, ""); reason = reason (n++ ? "; " : "") $0 }
      }
      END {
        printf "%s [%s]: %s%s\n", file, role, (reason == "" ? "reason not found" : reason),
          (frame == "" ? "" : "; frame: " frame)
      }' "$f"
  done
}

diagnose() {
  local rc=$1 reason=$2 observed="" hints="" f n
  local cg_after host_after hs_errs workers exits roles oom_lines wrong unfinished samples
  local killed=0 cg_oom=0 host_oom=0 cg_known=0 daemon_gone=0 evidence=0

  collect_files
  snapshot_memory "after ($reason)"

  obs() { observed="${observed}- $*"$'\n'; }
  hint() { hints="${hints}- $*"$'\n'; }

  obs "Command exit status: $(exit_text "$rc")."
  [ "$rc" -eq 137 ] && killed=1
  if [ "$reason" = interrupted ]; then
    obs "The wrapper received $SIGNAL_SEEN before the command finished."
    evidence=1
  fi

  if [ "$OS" = Linux ]; then
    cg_after=$(cg_oom_kill)
    host_after=$(host_oom_kill)
    if [ -z "$CG_DIR" ]; then
      obs "cgroup OOM kills: unavailable (memory cgroup not resolvable)."
    elif [ "$CG_OOM_BEFORE" = - ] || [ "$cg_after" = - ]; then
      obs "cgroup OOM kills: counter not exposed by cgroup v$CG_VERSION."
    else
      cg_known=1
      obs "cgroup OOM kills during stage: $((cg_after - CG_OOM_BEFORE)) ($CG_DIR)."
      [ "$cg_after" -gt "$CG_OOM_BEFORE" ] && cg_oom=1
    fi
    if [ "$HOST_OOM_BEFORE" != - ] && [ "$host_after" != - ]; then
      obs "Kernel-wide OOM kills during stage: $((host_after - HOST_OOM_BEFORE)) (may include processes outside this job)."
      [ "$host_after" -gt "$HOST_OOM_BEFORE" ] && host_oom=1
    fi
  fi

  hs_errs=$(hs_err_summary)
  if [ -n "$hs_errs" ]; then
    obs "JVM fatal error log(s): $(echo "$hs_errs" | paste -sd ';' -)."
    evidence=1
  fi

  workers=$(grep -oE "Process 'Gradle Test Executor [0-9]+' finished with non-zero exit value [0-9]+" \
    "$CONSOLE_LOG" 2>/dev/null | LC_ALL=C sort -u | head -5)
  if [ -n "$workers" ]; then
    obs "Gradle test worker(s) exited abnormally: $(echo "$workers" | sed "s/^Process '//; s/' finished with non-zero exit value / -> exit /" | paste -sd ';' -)."
    echo "$workers" | grep -q 'exit value 137$' && killed=1
    evidence=1
  fi
  if grep -q 'daemon disappeared unexpectedly' "$CONSOLE_LOG" 2>/dev/null; then
    obs "Gradle reported that its daemon disappeared unexpectedly."
    daemon_gone=1
    evidence=1
  fi

  # Test workers are recycled routinely (forkEvery), so prefer exits of the
  # abnormal workers and of the Gradle daemon over the normal rotation.
  roles=$( { echo "$workers" | grep -oE 'Executor [0-9]+' | sed 's/^Executor /Gradle_Test_Executor_/'; echo GradleDaemon; } \
    | sed 's/.*/role=&( |$)/' | paste -sd '|' -)
  exits=$(grep ' EXIT ' "$MEM_LOG" 2>/dev/null | grep -E "$roles" | tail -5)
  if [ -n "$exits" ]; then
    obs "Sampler saw these JVMs disappear: $(echo "$exits" | sed 's/ EXIT / /' | paste -sd ';' -)."
  else
    exits=$(grep ' EXIT ' "$MEM_LOG" 2>/dev/null | tail -5)
    [ -n "$exits" ] && obs "Last JVM exits seen by the sampler (routine worker recycling also appears here): $(echo "$exits" | sed 's/ EXIT / /' | paste -sd ';' -)."
  fi

  oom_lines=$(grep -c 'OutOfMemoryError' "$CONSOLE_LOG" 2>/dev/null)
  if [ "${oom_lines:-0}" -gt 0 ]; then
    obs "Console lines mentioning OutOfMemoryError: $oom_lines, e.g. \`$(grep -m1 'OutOfMemoryError' "$CONSOLE_LOG" | cut -c1-160)\`."
    evidence=1
  fi

  if [ "$IS_GRADLE" -eq 1 ]; then
    if grep -qE '^BUILD (SUCCESSFUL|FAILED)' "$CONSOLE_LOG" 2>/dev/null; then
      obs "Gradle completion marker: $(grep -E '^BUILD (SUCCESSFUL|FAILED)' "$CONSOLE_LOG" | tail -1)."
    else
      obs "Gradle completion marker (BUILD SUCCESSFUL/FAILED) not observed; read the exit status and console log together."
    fi
  fi

  wrong=$(awk '/^\* What went wrong:/ { on = 1; next } /^\* Try:/ { on = 0 }
    on && NF { print }' "$CONSOLE_LOG" 2>/dev/null | head -15)

  unfinished=$(unfinished_suites | head -10)
  [ -n "$unfinished" ] && obs "Test classes with a start but no end record: $(echo "$unfinished" | paste -sd ';' -)."

  [ "$killed" -eq 1 ] || [ "$cg_oom" -eq 1 ] || [ "$host_oom" -eq 1 ] && evidence=1

  # Interpretation, kept separate from the facts above. Only this job's cgroup
  # counter ties an OOM kill to this stage; the kernel-wide one may not.
  if [ "$cg_oom" -eq 1 ] && [ "$killed" -eq 1 ]; then
    hint "A process was SIGKILLed and the OOM killer fired in this job's cgroup: memory exhaustion is the likely cause. Check memory.log and jvms.log around the EXIT time."
  elif [ "$cg_oom" -eq 1 ]; then
    hint "The OOM killer killed a process in this job's cgroup during the stage; which process is not confirmed. A killed Gradle daemon or worker would explain a plain failure exit; check the EXIT records in memory.log and jvms.log."
  elif [ "$host_oom" -eq 1 ] && [ "$cg_known" -eq 1 ]; then
    hint "The kernel OOM killer fired outside this job's cgroup; it may be unrelated to this failure."
  elif [ "$host_oom" -eq 1 ]; then
    hint "The kernel OOM killer fired, but this job's cgroup could not be checked, so its link to this failure is unconfirmed."
  fi
  if [ "$killed" -eq 1 ] && [ "$cg_oom" -eq 0 ] && [ "$host_oom" -eq 0 ]; then
    if [ "$cg_known" -eq 1 ]; then
      hint "SIGKILL without OOM-killer evidence in this cgroup: the sender is unknown (runner, timeout handling or another process)."
    else
      hint "SIGKILL observed, but OOM-killer counters were not available to confirm or rule out OOM."
    fi
  fi
  [ -n "$hs_errs" ] && hint "A JVM hit a fatal error; the reason and problematic frame above come from its hs_err header."
  [ "$daemon_gone" -eq 1 ] && [ -z "$hs_errs" ] && [ "$cg_oom" -eq 0 ] \
    && hint "The Gradle daemon vanished without a fatal-error log or cgroup OOM kill: an external kill or a crash that could not write hs_err."
  [ "$reason" = interrupted ] && hint "Signals usually come from a step timeout or run cancellation."
  [ -n "$workers" ] && [ -z "$hs_errs" ] && [ "$killed" -eq 0 ] \
    && hint "A test worker exited with a non-zero status without a fatal-error log: possible System.exit/halt in a test, or a crash that could not write hs_err."
  [ "${oom_lines:-0}" -gt 0 ] \
    && hint "OutOfMemoryError text is a clue only; confirm which JVM threw it and whether it was caught by a test."
  if [ "$evidence" -eq 0 ] && [ "$IS_GRADLE" -eq 1 ]; then
    hint "No crash, OOM-kill, signal or worker-exit evidence was found; the failing Gradle task in console.log is the next place to look."
  elif [ "$evidence" -eq 0 ]; then
    hint "No crash, OOM-kill, signal or worker-exit evidence was found; console.log shows the failing command's output."
  elif [ -z "$hints" ]; then
    hint "No single cause can be inferred; read the observed facts together."
  fi

  {
    echo "### CI diagnosis: \`$STAGE\`"
    echo
    echo "**Observed**"
    echo
    printf '%s' "$observed"
    if [ -n "$wrong" ]; then
      echo
      echo "Gradle \"What went wrong\":"
      echo '```text'
      echo "$wrong"
      echo '```'
    fi
    if [ -n "$workers" ]; then
      echo
      echo "Last test records of the abnormal worker(s); a worker crash can still"
      echo "produce end events, so read these together with the exit status:"
      echo '```text'
      echo "$workers" | grep -oE 'Executor [0-9]+' | awk '{ print $2 }' | while read -r n; do
        recent_records "Gradle_Test_Executor_$n"
      done
      echo '```'
    fi
    samples=$(grep -E '^[0-9]{2}:[0-9]{2}:[0-9]{2} ' "$MEM_LOG" | tail -6)
    if [ -n "$samples" ]; then
      echo
      echo "Last memory samples:"
      echo '```text'
      echo "$samples"
      echo '```'
    fi
    echo
    echo "**Possible causes (inferred, not confirmed)**"
    echo
    printf '%s' "$hints"
    echo
    echo "Evidence: artifact \`tron-test-logs-*\` → \`ci-logs/${DIAG_DIR##*/}/\`."
  } > "$REPORT"

  [ -n "${GITHUB_STEP_SUMMARY:-}" ] && cat "$REPORT" >> "$GITHUB_STEP_SUMMARY"
  # One annotation, shown on the run page and in the PR checks list.
  if [ -n "${GITHUB_ACTIONS:-}" ]; then
    echo "::${CI_DIAG_ANNOTATION:-error} title=CI diagnosis ($STAGE)::$(printf '%sPossible causes:\n%s' \
      "$observed" "$hints" | sed 's/%/%25/g' | awk '{ printf "%s%%0A", $0 }')"
  fi
  {
    echo "== diagnosis"
    printf 'Observed:\n%sPossible causes:\n%s' "$observed" "$hints"
  } >> "$STATUS_FILE"
  log "diagnosis written to $REPORT"
  printf 'Observed:\n%sPossible causes:\n%s' "$observed" "$hints"
}

# Test progress records: "<time> <EVENT> <executor> <name> [result ms]".
unfinished_suites() {
  local f
  for f in "$DIAG_DIR"/test-progress-*.log; do
    [ -f "$f" ] || continue
    awk '$2 == "SUITE_START" { open[$3 " " $4] = 1 }
      $2 == "SUITE_END" { delete open[$3 " " $4] }
      END { for (k in open) { split(k, p, " "); printf "%s (%s)\n", p[2], p[1] } }' "$f"
  done
}

recent_records() {
  local f
  for f in "$DIAG_DIR"/test-progress-*.log; do
    [ -f "$f" ] || continue
    awk -v ex="$1" '$3 == ex' "$f" | tail -8
  done
}

# ------------------------------------------------------------------- main

touch "$MARKER"
: > "$MEM_LOG"
{
  echo "stage=$STAGE"
  echo "dir=$DIAG_DIR"
  echo "sha=$(git rev-parse HEAD 2>/dev/null || echo unknown)"
  echo "start=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "command=$*"
} > "$STATUS_FILE"

[ "$OS" = Linux ] && resolve_cgroup
snapshot_memory before
CG_OOM_BEFORE=$(cg_oom_kill)
HOST_OOM_BEFORE=$(host_oom_kill)

memory_sampler &
SAMPLER_PID=$!
FINISHED=0
SIGNAL_SEEN=""

finish() {
  local rc=$1 reason=$2
  [ "$FINISHED" -eq 1 ] && return
  FINISHED=1
  kill "$SAMPLER_PID" 2>/dev/null
  wait "$SAMPLER_PID" 2>/dev/null
  rm -f "$DIAG_DIR/.jvms.prev" "$DIAG_DIR/.jvms.cur"
  echo "end=$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$STATUS_FILE"
  if [ "$rc" -ne 0 ]; then
    diagnose "$rc" "$reason" || true
  else
    echo "result=success" >> "$STATUS_FILE"
  fi
}

# shellcheck disable=SC2329 # invoked by trap
on_signal() {
  # The runner signals the whole process group, so the command is stopping too.
  SIGNAL_SEEN=$1
  log "received $1; collecting diagnostics before exit"
  finish "$2" interrupted
  exit "$2"
}
trap 'on_signal SIGTERM 143' TERM
trap 'on_signal SIGINT 130' INT

# The test-progress init script records which test classes and methods ran on
# which worker, so a worker that dies without a failing test points at suspects.
INIT_ARGS=()
IS_GRADLE=0
case "$1" in
  ./gradlew | gradle | */gradlew)
    IS_GRADLE=1
    INIT_ARGS=(--init-script "$SCRIPT_DIR/test-progress.init.gradle") ;;
esac

# Keep exit statuses in memory, independent of whether diagnostic files can
# be written. FD 3 sends command output to the original console; the command
# substitution captures only PIPESTATUS, after both pipeline stages finish.
run_logged() {
  local statuses command_rc logger_rc
  statuses=$(
    "$@" 2>&1 | (
      tee "$CONSOLE_LOG" >&3
      logger_rc=$?
      if [ "$logger_rc" -ne 0 ]; then
        # Retain the pipe reader after tee fails, so a logging failure does
        # not interrupt the command with SIGPIPE.
        cat >/dev/null
      fi
      exit "$logger_rc"
    )
    printf '%s %s\n' "${PIPESTATUS[0]}" "${PIPESTATUS[1]}"
  )
  case "$statuses" in
    *' '*)
      command_rc=${statuses%% *}
      logger_rc=${statuses#* } ;;
    *)
      warn_diagnostics "Could not obtain the command exit status; failing the stage."
      return 1 ;;
  esac
  if [ "$logger_rc" -ne 0 ]; then
    warn_diagnostics "Console logging failed (tee exit $logger_rc); command exit status remains $command_rc."
  fi
  printf 'command_exit=%s tee_exit=%s\n' "$command_rc" "$logger_rc" >> "$STATUS_FILE" \
    || warn_diagnostics "Could not save exit-status metadata; command exit status remains $command_rc."
  return "$command_rc"
}

# Waiting on the background function keeps signal traps responsive, while its
# return value is the command's status rather than the logger's status.
run_logged "$@" ${INIT_ARGS[@]+"${INIT_ARGS[@]}"} 3>&1 &
COMMAND_PID=$!
wait "$COMMAND_PID"
RC=$?

finish "$RC" exited
exit "$RC"
