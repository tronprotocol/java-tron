#!/usr/bin/env bash
# Prints every Java file that uses java.lang.Math: bare `Math.` calls, fully
# qualified `java.lang.Math.` calls (including static imports) and
# `import java.lang.Math`. No output means no forbidden usage.
# StrictMathWrapper.java and MathWrapper.java are exempt. String literals, char
# literals and comments are stripped before matching, so `StrictMath.` and
# mentions in text are not reported.
# Used by .github/workflows/math-check.yml; run it locally from any directory.
set -euo pipefail

cd "$(dirname "$0")/../.."

find . -type f -name '*.java' -not -path '*/build/*' | while IFS= read -r file; do
  case "$(basename "$file")" in
    StrictMathWrapper.java|MathWrapper.java) continue ;;
  esac

  perl -0777 -ne '
    s/"([^"\\]|\\.)*"//g;
    s/'\''([^'\''\\]|\\.)*'\''//g;
    s!/\*([^*]|\*[^/])*\*/!!g;
    s!//[^\n]*!!g;
    $hasMath = 0;
    $hasMath = 1 if /^[\s]*import[\s]+java\.lang\.Math\b/m;
    $hasMath = 1 if /\bjava\s*\.\s*lang\s*\.\s*Math\s*\./;
    $hasMath = 1 if /(?<![\w\.])(?<!Strict)Math\s*\./;
    print "$ARGV\n" if $hasMath;
  ' "$file"
done | sort -u
