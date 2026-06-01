#!/usr/bin/env python3
"""Validate reference.conf comment coverage.

Rules enforced:
  1. Every user-defined key line must have a comment, except keys under
     genesis.block.
  2. A key is documented by either an inline comment on the same line or a
     comment on the immediately preceding line. Blank lines do not count.
  3. Object fields repeated across array elements are checked only on their
     first occurrence within that array.

This is deliberately line-oriented. pyhocon is not used because it discards
comments, and this gate only needs enough structure to track braces, arrays,
and the genesis.block exemption.
"""
import re
import sys
from pathlib import Path

KEY_LINE = re.compile(r"^\s*([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*[:={]")
COMMENT_LINE = re.compile(r"^\s*(#|//)")


def strip_quoted(line):
    """Remove quoted string contents while preserving comments and delimiters."""
    out = []
    quote = None
    escaped = False
    i = 0
    while i < len(line):
        ch = line[i]
        if quote:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == quote:
                quote = None
                out.append(ch)
            i += 1
            continue
        if ch in ('"', "'"):
            quote = ch
            out.append(ch)
            i += 1
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def strip_comments(line):
    """Strip # and // comments outside quotes."""
    text = strip_quoted(line)
    i = 0
    while i < len(text):
        ch = text[i]
        if ch == "#":
            return text[:i]
        if ch == "/" and i + 1 < len(text) and text[i + 1] == "/":
            return text[:i]
        i += 1
    return text


def has_inline_comment(line):
    text = strip_quoted(line)
    i = 0
    while i < len(text):
        if text[i] == "#":
            return True
        if text[i] == "/" and i + 1 < len(text) and text[i + 1] == "/":
            return True
        i += 1
    return False


def has_prevline_comment(lines, index):
    if index == 0:
        return False
    prev = lines[index - 1]
    return bool(prev.strip()) and bool(COMMENT_LINE.match(prev))


def opening_after_key(code, match):
    pos = match.end() - 1
    ch = code[pos]
    if ch in "{[":
        return ch, pos
    if ch in ":=":
        i = pos + 1
        while i < len(code) and code[i].isspace():
            i += 1
        if i < len(code) and code[i] in "{[":
            return code[i], i
    return None, None


def nearest_array_frame(stack):
    for frame in reversed(stack):
        if frame["type"] == "array":
            return frame
    return None


def in_genesis_block(stack, key=None):
    return key == "genesis.block" or any(frame["name"] == "genesis.block" for frame in stack)


def pop_frame(stack, opener):
    target_type = "object" if opener == "}" else "array"
    while stack:
        frame = stack.pop()
        if frame["type"] == target_type:
            return


def scan_structure(code, stack, key_open_pos=None):
    i = 0
    while i < len(code):
        ch = code[i]
        if key_open_pos is not None and i == key_open_pos:
            i += 1
            continue
        if ch == "{":
            stack.append({"type": "object", "name": None, "seen": set()})
        elif ch == "[":
            stack.append({"type": "array", "name": None, "seen": set()})
        elif ch == "}":
            pop_frame(stack, "}")
        elif ch == "]":
            pop_frame(stack, "]")
        i += 1


def collect_keys(path, list_all=False):
    lines = path.read_text().splitlines()
    stack = []
    missing = []
    seen_rows = []

    for index, raw in enumerate(lines):
        line_no = index + 1
        code = strip_comments(raw)
        stripped = raw.lstrip()
        is_comment = stripped.startswith("#") or stripped.startswith("//")
        match = None if is_comment else KEY_LINE.match(code)

        key = None
        status = "non-key"
        key_open_pos = None
        if match:
            key = match.group(1)
            opener, key_open_pos = opening_after_key(code, match)
            exempt = in_genesis_block(stack, key)

            deduped = False
            array_frame = nearest_array_frame(stack)
            if array_frame is not None:
                if key in array_frame["seen"]:
                    deduped = True
                else:
                    array_frame["seen"].add(key)

            commented = has_inline_comment(raw) or has_prevline_comment(lines, index)
            if exempt:
                status = "exempt"
            elif deduped:
                status = "dedup"
            elif commented:
                status = "commented"
            else:
                status = "missing"
                missing.append((line_no, key))

            if opener:
                stack.append({
                    "type": "object" if opener == "{" else "array",
                    "name": key,
                    "seen": set(),
                })

        scan_structure(code, stack, key_open_pos)

        if list_all and match:
            seen_rows.append((line_no, key, status))

    return missing, seen_rows


def main(argv):
    list_all = False
    args = list(argv[1:])
    if "--list" in args:
        list_all = True
        args.remove("--list")
    if len(args) != 1:
        print(f"usage: {argv[0]} [--list] <path/to/reference.conf>", file=sys.stderr)
        return 2

    path = Path(args[0])
    if not path.is_file():
        print(f"error: file not found: {path}", file=sys.stderr)
        return 2

    missing, seen_rows = collect_keys(path, list_all)

    if list_all:
        for line_no, key, status in seen_rows:
            print(f"{line_no}: {key} [{status}]")
        print()

    if missing:
        lines_out = [
            f"Comment coverage violations ({len(missing)}) — each non-genesis.block key "
            "needs an inline or immediately preceding comment:"
        ]
        for line_no, key in missing:
            lines_out.append(f"  comment: line {line_no}: {key}")
        print("\n".join(lines_out))
        print()

        entries = [f"line {line_no}: {key}" for line_no, key in missing]
        body = (
            f"reference.conf has {len(missing)} comment coverage violation(s):%0A"
            + "%0A".join(entries)
        )
        print(f"::error file={path},title=reference.conf::{body}")
        print(
            f"FAIL: {len(missing)} comment coverage violation(s) in {path}",
            file=sys.stderr,
        )
        return 1

    print(f"OK: {path} — all non-genesis.block keys have comments")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
