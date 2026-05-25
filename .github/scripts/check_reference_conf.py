#!/usr/bin/env python3
"""Validate java-tron reference.conf key names and hierarchy depth.

Rules enforced:
  1. Every user-defined segment of every key path must match ^[a-z][a-zA-Z0-9]*$:
     starts with a lowercase ASCII letter, then ASCII letters/digits only.
     Acronyms at position 1+ are accepted (e.g. `httpPBFTEnable`,
     `openHistoryQueryWhenLiteFN`, `allowShieldedTRC20Transaction`) — only the
     first character is constrained. This matches what java.beans.Introspector
     and ConfigBeanFactory actually require for bean-property auto-binding.
  2. Total path depth must be <= MAX_DEPTH (5). Each list/array step counts
     as one additional level. For example `rate.limiter.http[].component`
     is 5 levels deep (rate=1, limiter=2, http=3, []=4, component=5).
  3. ALLOWLIST entries are exempt from the format rule (legacy keys that ship
     in user configs; renaming would break compatibility).
  4. Service-binding port values must be unique. A leaf is a "service port"
     when its last segment is `port` or ends in `Port` (camelCase) AND its
     path contains no `[]` (list-element ports belong to per-element records,
     not to the local process). Two distinct paths binding the same int value
     would conflict at startup; reserved sentinels (0, -1) are exempt.

Parsing strategy: delegated to pyhocon (https://github.com/chimpler/pyhocon),
the reference Python HOCON implementation. This avoids hand-rolled scanner
pitfalls (key = { ... } prefix loss, triple-strings, substitutions, includes,
+= operator, block comments). pyhocon returns a fully-merged ConfigTree where
dotted-form keys are expanded into nested objects — i.e. the same canonical
key set Typesafe Config / ConfigBeanFactory will see at runtime.

Array handling: keys inside object-elements of arrays are also user-defined
config keys (e.g. each entry in `rate.limiter.rpc = [{ component=..., ... }]`
is parsed by RateLimiterConfig). The walker recurses into list elements and
treats the array step as a synthetic `[]` segment that contributes to depth
but is not itself validated as a name. Element keys are deduplicated across
list entries because well-formed arrays use homogeneous object shapes.

Debug mode: pass `--debug` to print every parsed key with its depth, in
walk order (which mirrors the file top-to-bottom). Use this to eyeball the
parser's view against reference.conf.

Exit code: 0 if clean, 1 if any violation remains after allowlist filtering,
2 on environment errors (missing pyhocon, file not found, parse failure).

CI integration: invoked by the `Validate reference.conf key names and depth`
step of the `checkstyle` job in `.github/workflows/pr-check.yml`. The non-zero
exit on violations is what makes that step fail — there is intentionally NO
extra `exit 1` in the workflow shell wrapper. A single GHA `::error` workflow
command is also emitted unconditionally (not gated on the GITHUB_ACTIONS env
var) so local runs produce the same output as CI; the leading `::` is
harmless noise locally.
"""
import re
import sys
from pathlib import Path

try:
    from pyhocon import ConfigFactory, ConfigTree
except ImportError:
    print(
        "error: pyhocon is required. Install with `pip install pyhocon`.",
        file=sys.stderr,
    )
    sys.exit(2)

# Set at the current max depth of reference.conf (5). No buffer: a mature
# project should not allow silent drift, so any new key going deeper must
# bump MAX_DEPTH via an explicit, reviewed change (deeper trees hurt
# readability and complicate ConfigBeanFactory mapping).
MAX_DEPTH = 5
KEY_REGEX = re.compile(r'^[a-z][a-zA-Z0-9]*$')
# Legacy keys grandfathered to keep user `config.conf` files compatible.
# Do NOT extend this list for new keys — every new key must satisfy KEY_REGEX.
# A future rename + deprecation cycle can shrink this set back to empty.
ALLOWLIST = {
    # PBFT acronym in capitals — predates the auto-binding convention.
    "node.http.PBFTEnable",
    "node.http.PBFTPort",
    "node.rpc.PBFTEnable",
    "node.rpc.PBFTPort",
    # PascalCase exceptions handled manually in NodeConfig.fromConfig (not via
    # ConfigBeanFactory). Currently commented out in reference.conf, so the
    # parser does not see them today — listed here so the gate stays green if
    # a future change uncomments them with defaults.
    "node.shutdown.BlockTime",
    "node.shutdown.BlockHeight",
    "node.shutdown.BlockCount",
}

# Sentinel port values exempt from the uniqueness check. 0 = disabled (the
# service does not bind); -1 = auto/unset placeholder. Any number of leaves
# may share these values.
PORT_SENTINELS = {0, -1}


def walk(node, path, depth):
    """Yield (full_path, depth, is_leaf) for every reachable user-defined key.

    - ConfigTree key adds one depth level and contributes a name segment.
    - list step adds one synthetic level rendered as `[]`. Element-internal
      keys are walked once per unique sub-path (homogeneous object arrays
      otherwise yield each field N times).
    - Scalars / null / list-of-scalars produce no further keys.

    `depth` includes the array `[]` steps. `is_leaf` is True when the value
    at this path is a scalar/list/null — i.e. not another ConfigTree — so
    callers can filter leaves vs namespace intermediates.
    """
    if isinstance(node, ConfigTree):
        for k, v in node.items():
            new_path = f"{path}.{k}" if path else k
            new_depth = depth + 1
            is_leaf = not isinstance(v, ConfigTree)
            yield new_path, new_depth, is_leaf
            yield from walk(v, new_path, new_depth)
    elif isinstance(node, list):
        array_path = f"{path}[]"
        array_depth = depth + 1
        seen = set()
        for elem in node:
            # Object element: walk its keys. Nested list element (HOCON allows
            # list-of-list, e.g. `a = [[{x=1}]]`): recurse so each inner [] step
            # also contributes to depth. Scalar elements have no sub-keys.
            if isinstance(elem, (ConfigTree, list)):
                for sub_path, sub_depth, sub_leaf in walk(elem, array_path, array_depth):
                    if sub_path in seen:
                        continue
                    seen.add(sub_path)
                    yield sub_path, sub_depth, sub_leaf


def _is_port_segment(seg):
    """Last-segment test for a service-binding port leaf.

    Matches `port` (exact) and any camelCase form ending in `Port`
    (e.g. `fullNodePort`, `solidityPort`, `PBFTPort`). Deliberately rejects
    lowercase `port` as a suffix inside a longer word (`transport`,
    `support`) — those are not port keys.
    """
    return seg == "port" or seg.endswith("Port")


def find_port_collisions(tree, keys):
    """Group service-binding port leaves by integer value; return collisions.

    A leaf qualifies when (a) its last segment matches `_is_port_segment`,
    and (b) its full path contains no `[]` step. Rule (b) excludes
    list-element ports — e.g. `genesis.block.witnesses[].port` is the
    advertised port of each genesis witness record, not a port the local
    process binds, so two witnesses sharing a value is expected.

    Returns sorted list of (value, sorted_paths) for any value bound by more
    than one path. Sentinel values in PORT_SENTINELS are excluded. Values
    that are not coercible to int (substitutions like `${PORT}` resolved to
    strings) are skipped silently — the format/depth gates do not look at
    values either, and a non-numeric port is a different class of error.
    """
    by_value = {}
    for full_path, _depth, is_leaf in keys:
        if not is_leaf:
            continue
        if "[]" in full_path:
            continue
        seg = full_path.split(".")[-1]
        if not _is_port_segment(seg):
            continue
        try:
            raw = tree.get(full_path)
        except Exception:
            continue
        try:
            value = int(raw)
        except (TypeError, ValueError):
            continue
        if value in PORT_SENTINELS:
            continue
        by_value.setdefault(value, []).append(full_path)
    return sorted(
        (v, sorted(paths)) for v, paths in by_value.items() if len(paths) > 1
    )


def main(argv):
    debug = False
    args = list(argv[1:])
    if args and args[0] == "--debug":
        debug = True
        args = args[1:]
    if len(args) != 1:
        print(f"usage: {argv[0]} [--debug] <path/to/reference.conf>", file=sys.stderr)
        return 2
    path = Path(args[0])
    if not path.is_file():
        print(f"error: file not found: {path}", file=sys.stderr)
        return 2

    try:
        tree = ConfigFactory.parse_file(str(path))
    except Exception as e:
        print(f"error: failed to parse {path}: {e}", file=sys.stderr)
        # Mirror the violation path: emit a single GHA annotation so the
        # parse failure surfaces in the PR check summary, not just the log.
        print(f"::error file={path},title=reference.conf::failed to parse: {e}")
        return 2

    keys = list(walk(tree, "", 0))

    if debug:
        # Keys are yielded in pyhocon insertion order, which mirrors the
        # source file top-to-bottom. Eyeball this against reference.conf to
        # confirm coverage; the depth column makes the array `[]` steps
        # explicit so MAX_DEPTH math is verifiable by inspection. Trailing
        # `/` marks namespace intermediates (have children); bare names are
        # leaves — `grep -v '/$'` filters to just leaves.
        leaf_count = sum(1 for _, _, lf in keys if lf)
        print(
            f"DEBUG: {len(keys)} parsed keys "
            f"({leaf_count} leaves + {len(keys) - leaf_count} intermediates), "
            f"walk order:"
        )
        for full_path, depth, is_leaf in keys:
            label = full_path if is_leaf else full_path + "/"
            print(f"  d={depth}  {label}")
        print()

    format_violations = []
    depth_violations = []

    # Only check leaves: pyhocon expands a dotted-form declaration like
    # `a.b.c = X` into intermediate ConfigTree nodes for `a` and `a.b`. A
    # single user-written bad key would otherwise be reported once per
    # intermediate AND once as the leaf, multiplying noise. The leaf path
    # carries every segment, so checking just leaves covers all segments.
    for full_path, depth, is_leaf in keys:
        if not is_leaf:
            continue
        if full_path not in ALLOWLIST:
            for seg in full_path.split('.'):
                # Strip any number of trailing `[]` markers — nested arrays
                # produce segments like `a[][]`.
                while seg.endswith('[]'):
                    seg = seg[:-2]
                if seg and not KEY_REGEX.match(seg):
                    format_violations.append((full_path, seg))
                    break

        if depth > MAX_DEPTH:
            depth_violations.append((full_path, depth))

    format_violations.sort()
    depth_violations.sort()

    port_collisions = find_port_collisions(tree, keys)

    if format_violations or depth_violations or port_collisions:
        lines_out = []
        if format_violations:
            lines_out.append(
                f"Format violations ({len(format_violations)}) — "
                f"each segment must match {KEY_REGEX.pattern}:"
            )
            for full_path, seg in format_violations:
                lines_out.append(f"  format: {full_path}   (segment: '{seg}')")
        if depth_violations:
            if lines_out:
                lines_out.append("")
            lines_out.append(
                f"Depth violations ({len(depth_violations)}) — max depth is {MAX_DEPTH} "
                f"(each `[]` array step counts as one level):"
            )
            for full_path, depth in depth_violations:
                lines_out.append(
                    f"  depth: {full_path}   (depth={depth}, max={MAX_DEPTH})"
                )
        if port_collisions:
            if lines_out:
                lines_out.append("")
            lines_out.append(
                f"Port collisions ({len(port_collisions)}) — distinct service "
                f"ports must bind distinct values (sentinels {sorted(PORT_SENTINELS)} exempt):"
            )
            for value, paths in port_collisions:
                lines_out.append(
                    f"  port:   value {value} bound by: {', '.join(paths)}"
                )
        print("\n".join(lines_out))
        print()

        # Emit ONE consolidated GHA workflow annotation. All offending entries
        # are packed into the annotation body via %0A (GHA's newline escape)
        # so the entries are visible in the annotation summary, not just in
        # the job log.
        entries = []
        for full_path, seg in format_violations:
            entries.append(f"format: {full_path} (segment '{seg}')")
        for full_path, depth in depth_violations:
            entries.append(f"depth: {full_path} (depth={depth}, max={MAX_DEPTH})")
        for value, paths in port_collisions:
            entries.append(f"port: value {value} bound by {', '.join(paths)}")
        body = (
            f"reference.conf has {len(format_violations)} format + "
            f"{len(depth_violations)} depth + {len(port_collisions)} port "
            f"violation(s):%0A" + "%0A".join(entries)
        )
        print(f"::error file={path},title=reference.conf::{body}")
        print(
            f"FAIL: {len(format_violations)} format + {len(depth_violations)} depth "
            f"+ {len(port_collisions)} port violation(s) in {path}",
            file=sys.stderr,
        )
        return 1

    print(
        f"OK: {path} — {len(keys)} keys, all lowerCamelCase, depth <= {MAX_DEPTH}, "
        f"service ports unique"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
