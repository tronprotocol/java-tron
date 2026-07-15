#!/usr/bin/env python3
"""Validate reference.conf comment coverage.

Rules enforced:
  1. Every user-defined key line must have a comment.
  2. A key is documented by either an inline comment on the same line or a
     comment on the immediately preceding line. Blank lines do not count.
  3. Object fields repeated across array elements are checked only on their
     first occurrence within that array.

Design scope — basic coverage gate, not a full HOCON parser
-----------------------------------------------------------
This script is deliberately line-oriented. pyhocon is not used because it
discards comments, and this gate only needs enough structure to track braces
and arrays.

As a consequence, several HOCON constructs are handled in a simplified way.
Each known limitation is listed below together with its practical risk level
for reference.conf.  The gate is intentionally kept simple: reference.conf
uses a small, stable subset of HOCON syntax, and the constructs below are
either forbidden by the project's config conventions or have never appeared
in the file.

Known limitations (all rated LOW risk for reference.conf):

  A. Silent miss — keys matched by none of the patterns below are neither
     checked nor flagged; they pass silently:

     * Quoted keys:      "my-key" = value
       KEY_LINE requires [A-Za-z_] at the start; a leading '"' never matches.
       reference.conf uses only plain lowerCamelCase keys — risk: none.

     * Hyphenated keys:  my-key = value
       KEY_LINE allows only [A-Za-z0-9_]; '-' is excluded.
       reference.conf has no hyphenated keys — risk: none.

     * Append operator:  foo += bar
       KEY_LINE ends with [:={]; '+' before '=' is not in that set.
       reference.conf does not use '+=' — risk: none.

     * Inline-object sub-keys:  outer = {inner = 1}
       KEY_LINE.match() anchors to the line start, so only the first key on
       each line ('outer') is detected; 'inner' inside the braces is missed.
       reference.conf expands every block across multiple lines — risk: none.

     * Second key on a bare-value line:  a = 1, b = 2
       re.match() matches only at the start; 'b' is invisible to KEY_LINE.
       reference.conf never puts two assignments on one line — risk: none.

  B. False positive — non-key content incorrectly flagged as a missing key:

     * Triple-quoted multi-line strings  (key = \"\"\" ... \"\"\")
       strip_quoted() is line-oriented and does not track triple-quote spans
       across lines.  Lines inside the string body that look like 'word = ...'
       are matched by KEY_LINE and reported as keys lacking comments.
       reference.conf contains no triple-quoted strings — risk: none.
       If triple-quoted strings are ever introduced, add a triple-quote span
       tracker at the top of the collect_keys() loop (see inline comment there).

  C. False pass — a key with no real comment is incorrectly classified as
     documented:

     * Block opened on the next line:  key =\n{
       opening_after_key() only scans the current line for '{' or '['.
       If the opening brace appears on the next line, no named frame is
       pushed for the key, so array-element deduplication silently stops
       working for that block's contents.
       reference.conf always opens blocks on the same line as the key
       (e.g. "genesis.block = {") — risk: none.

     * Bare URL value:  key = http://example.com
       has_inline_comment() sees '//' in the URL and returns True, treating
       the URL as an inline comment.  Quoting the URL ("http://...") avoids
       this because strip_quoted() removes the string contents before the
       comment scan.  reference.conf contains no bare (unquoted) URLs and all
       such values are either quoted or absent — risk: none.
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


def pop_frame(stack, closer):
    target_type = "object" if closer == "}" else "array"
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
    """Scan *path* line by line and classify every HOCON key.

    Returns
    -------
    missing : list of (line_no, key)
        Keys that lack a comment and are not exempt.  Empty means the file
        passes the gate.
    seen_rows : list of (line_no, key, status)
        One entry per matched key line, in file order.  Populated only when
        *list_all* is True (``--list`` flag); always empty otherwise.
        status is one of: "commented" | "dedup" | "missing".
    """
    lines = path.read_text(encoding="utf-8").splitlines()

    # stack — bracket-nesting context, one frame per open { or [.
    # Each frame is a dict:
    #   "type" : "object" | "array"
    #   "name" : str | None  — the key that opened this block, or None for
    #                          anonymous braces/brackets.
    #   "seen" : set          — only meaningful on array frames: the set of
    #                          key names already encountered inside this array.
    #                          Enables deduplication so that repeated keys in
    #                          homogeneous array elements (e.g. rate.limiter
    #                          entries) are only checked on their first
    #                          occurrence.
    stack = []

    # missing — accumulates (line_no, key) for every key that is neither
    # exempt nor deduplicated yet has no comment.  Drives the exit-1 path.
    missing = []

    # seen_rows — full audit log for --list mode: (line_no, key, status).
    # Built only when list_all=True to avoid wasting memory in normal runs.
    seen_rows = []

    for index, raw in enumerate(lines):
        line_no = index + 1

        # code: raw line with comment text removed.  Used for KEY_LINE
        # matching and bracket counting so that "#" / "//" inside values
        # do not confuse the structural parser.
        code = strip_comments(raw)

        stripped = raw.lstrip()
        is_comment = stripped.startswith("#") or stripped.startswith("//")

        # Skip pure comment lines; never treat them as key lines.
        match = None if is_comment else KEY_LINE.match(code)

        key = None
        status = "non-key"
        key_open_pos = None  # position in `code` of the { or [ that this key opens
        if match:
            key = match.group(1)

            # opener: "{" or "[" when the key introduces a block/array on
            # the same line (e.g. "node {" or "active = [").
            # key_open_pos: char index of that opener inside `code`, passed
            # to scan_structure so it is not counted a second time.
            opener, key_open_pos = opening_after_key(code, match)

            # --- Array deduplication ---
            # Find the innermost enclosing array frame (if any).  Within an
            # array, all elements share the same schema, so only the first
            # occurrence of each key name needs a comment.
            deduped = False
            array_frame = nearest_array_frame(stack)
            if array_frame is not None:
                if key in array_frame["seen"]:
                    # Already checked on an earlier array element — skip.
                    deduped = True
                else:
                    # First time we see this key in this array; record it and
                    # fall through to the normal comment check below.
                    array_frame["seen"].add(key)

            # --- Comment check ---
            # A key is considered documented if it has an inline comment on
            # the same line *or* a non-blank comment on the immediately
            # preceding line (blank lines between comment and key do NOT
            # count as "preceding").
            commented = has_inline_comment(raw) or has_prevline_comment(lines, index)

            # Assign the final status in priority order.
            if deduped:
                status = "dedup"
            elif commented:
                status = "commented"
            else:
                status = "missing"
                missing.append((line_no, key))

            # If this key opens a new block or array, push a fresh frame so
            # that nested keys and future deduplication operate in the correct
            # scope.  We push *after* classifying the key itself so that the
            # key is judged in its *parent* scope, not inside itself.
            if opener:
                stack.append({
                    "type": "object" if opener == "{" else "array",
                    "name": key,
                    "seen": set(),
                })

        # Walk any remaining { } [ ] characters in `code` that were NOT the
        # opener just pushed above.  This keeps the stack in sync for lines
        # that contain multiple brackets (e.g. closing braces after a value).
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
            f"Comment coverage violations ({len(missing)}) — each key "
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

    print(f"OK: {path} — all keys have comments")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
