# AGENTS.md

Guidance for AI coding assistants and new contributors working on java-tron: how to build, test, and navigate the codebase, plus the constraints that CI enforces and the invariants that must not be broken. For running a node, see the [README](./README.md) and [`docs/`](./docs).

## Guidelines

- **Keep changes minimal and focused.** Only modify code directly related to the task at hand. Do not refactor unrelated code, rename existing variables or functions for style, or bundle unrelated fixes into the same commit or PR.
- **Do not add, remove, or update dependencies** unless the task explicitly requires it. Dependency changes in a consensus node are high-risk, need separate review, and require regenerating dependency verification metadata (see checklist step 6).
- **Never hand-edit generated sources.** Protobuf / gRPC Java stubs are generated at build time from `protocol/src/main/protos/` (`core/`, `api/`) and are git-ignored. Rebuild after changing a `.proto`.

## Build & Test

Supported platforms: **Linux** and **macOS** only. The JDK requirement is determined by CPU architecture: **JDK 8** on x86_64, **JDK 17** on ARM64/aarch64 (Apple Silicon, AWS Graviton). The build fails fast if the JDK major version does not match the architecture.

```bash
./gradlew clean build -x test                # build without tests
./gradlew build                              # build with tests
./gradlew test                               # run all tests
./gradlew :framework:test                    # test one module
./gradlew :framework:test --tests "org.tron.core.db.TronDatabaseTest"           # one class
./gradlew :framework:test --tests "org.tron.core.db.TronDatabaseTest.testX"     # one method
./gradlew :framework:testWithRocksDb         # RocksDB tests (x86 only)
./gradlew jacocoTestReport                   # coverage report
```

- Main entry point: `org.tron.program.FullNode`.
- Tests run in parallel locally, serially in CI (detected via the `CI` env var); the test-retry plugin retries up to 5 times.
- On ARM64/aarch64, only the RocksDB storage engine is supported; the build forces RocksDB and skips the LevelDB tests.
- CI builds the full matrix: **JDK 8 / x86_64** (rockylinux, debian11) and **JDK 17 / aarch64** (macOS, ubuntu24). A change must compile on both.

## Pre-Commit Checklist

Run **all** applicable checks before committing. Each maps to a CI job that will otherwise fail the PR.

### 1. Build

```bash
./gradlew clean build -x test
```

### 2. Tests

```bash
./gradlew test
```

### 3. Checkstyle

Run exactly what CI runs (`.github/workflows/pr-check.yml`):

```bash
./gradlew :framework:checkstyleMain :framework:checkstyleTest :plugins:checkstyleMain
```

Checkstyle is configured only for `framework`, `protocol`, and `plugins`. A bare `./gradlew checkstyleMain` does not reproduce the CI gate.

### 4. Forbidden `Math` usage

CI (`.github/workflows/math-check.yml`) **fails the build on any use of `java.lang.Math`** anywhere in the repository. Only `StrictMathWrapper.java` and `MathWrapper.java` are exempt.

Use `org.tron.common.math.StrictMathWrapper` instead. Self-check before pushing (same matching logic as CI; `StrictMath.` and string/comment occurrences are correctly ignored):

```bash
find . -name '*.java' -not -path '*/build/*' | while IFS= read -r f; do
  case "$(basename "$f")" in StrictMathWrapper.java|MathWrapper.java) continue;; esac
  perl -0777 -ne 's/"([^"\\]|\\.)*"//g; s!/\*([^*]|\*[^/])*\*/!!g; s!//[^\n]*!!g;
    print "$ARGV\n" if /(?<![\w.])(?<!Strict)Math\s*\./;' "$f"
done | sort -u
```

No output means the gate passes. (`grep -E` cannot express this — the rule needs a negative lookbehind.)

This exists because `java.lang.Math` gives platform-dependent results for floating-point operations, which breaks cross-JVM determinism between the x86/JDK 8 and ARM64/JDK 17 builds.

### 5. Config validation — only if you touched `reference.conf`

```bash
pip install -r .github/scripts/requirements.txt          # pyhocon, required by the first script
python3 .github/scripts/check_reference_conf.py     common/src/main/resources/reference.conf
python3 .github/scripts/check_reference_comments.py common/src/main/resources/reference.conf
```

Both gates run in CI. The rules:

- Every key path segment must match `^[a-z][a-zA-Z0-9]*$` (only the first character is constrained; acronyms such as `httpPBFTEnable` are fine). This is what `ConfigBeanFactory` bean-binding requires.
- Total path depth ≤ **5**; each list/array step counts as one level.
- Service-binding port values (leaf named `port` or ending in `Port`, outside arrays) must be unique; `0` and `-1` are reserved sentinels.
- **Every key needs a comment** — inline on the same line, or on the immediately preceding line. Blank lines do not count.

See [`docs/configuration-conventions.md`](./docs/configuration-conventions.md).

### 6. Dependency verification — only if you touched dependencies

`gradle/verification-metadata.xml` pins checksums for every resolved artifact (`verify-metadata=true`). Adding, removing, or upgrading any dependency requires regenerating it, or the build fails for everyone:

```bash
./gradlew --write-verification-metadata sha256 help
```

Review the resulting diff — it must contain only the artifacts your change actually introduces.

### 7. Do not commit binaries

No `*.jar`, `build/`, logs, or database files — whether produced by the main build or as byproducts of investigation.

## Module Layout

| Module | Responsibility |
|--------|----------------|
| `framework` | Main entry (`org.tron.program.FullNode`); wires all modules; largest test suite |
| `protocol` | Protobuf / gRPC definitions |
| `chainbase` | Blockchain storage abstraction (LevelDB / RocksDB); snapshot & rollback |
| `consensus` | Pluggable DPoS consensus engine |
| `actuator` | Transaction execution; one Actuator class per transaction type |
| `crypto` | Cryptographic primitives (depends only on `common`) |
| `common` | Shared utilities |
| `platform` | Architecture-specific implementations selected at build time (separate `x86` / `arm` / `common` source sets): math wrappers, LevelDB/RocksDB order-price comparators — relevant to cross-JVM determinism |
| `plugins` | Standalone tools (`Toolkit.jar`, `ArchiveManifest.jar`) |

`errorprone` and `example:actuator-example` are build-support and sample modules, not part of the node.

**Module dependency direction is one-way — do not introduce reverse dependencies:**

```text
framework → chainbase → common → protocol
actuator  → chainbase
consensus → chainbase / common   (only via ConsensusDelegate; never call Manager directly)
crypto    → common
```

`platform` is a leaf module (no project dependencies of its own) that `common`, `framework`, and `plugins` depend on for architecture-specific code.

## Hard Constraints

**Cross-JVM determinism** (consensus, state transition, block ordering) — the same block must produce the same state on every supported platform:
- Never use `java.lang.Math` — use `org.tron.common.math.StrictMathWrapper` instead (CI-enforced, see checklist step 4).
- Never use `float` / `double` in consensus-relevant arithmetic.
- Never depend on `HashMap` iteration order for a business decision.
- Use the DPoS slot time for produced-block timestamps, not `System.currentTimeMillis()`.
- Never call `String.toLowerCase()` / `toUpperCase()` without an explicit `Locale` — ErrorProne enforces this as a compile error (`StringCaseLocaleUsage`).

**DB / Store:**
- All writes must happen inside a revocable session — `try (ISession session = revokingStore.buildSession())` — never a bare `put()`.
- A new store must extend `TronStoreWithRevoking<T>` and register with the `RevokingDatabase`.
- Multi-store updates must roll back fully on exception.

**Actuator:**
- New actuators are registered automatically: place the class in the `org.tron.core.actuator` package, extend `AbstractActuator`, and pass the `ContractType` to `super(...)` from a no-arg constructor. `TransactionRegister.registerActuator()` discovers it by reflection at startup — there is no manual registration step.
- Charge fees before `execute()`.
- `validate()` must not mutate state.

**Protobuf:**
- Fields may only be added — never removed or renumbered.
- Message field numbers start at `1`; the first enum value must be `0`.

**API / Threads:**
- New HTTP servlets must go through `HttpApiAccessFilter` and use `Wallet` (never inject `Manager` directly).
- A new gRPC or HTTP query that depends on historical data (unavailable on a lite fullnode) must be added to the deny-list in `LiteFnQueryGrpcInterceptor` / `LiteFnQueryHttpFilter`; other methods need no action — the interceptor and filter are installed server-wide.
- No bare `new Thread()` — use a named Executor, shut down via `shutdown()` → `awaitTermination()` → `shutdownNow()`.

## Common Pitfalls

1. **ErrorProne only runs on JDK 11+.** Building on x86/JDK 8 will *not* surface `StringCaseLocaleUsage` violations, but the aarch64/JDK 17 CI jobs will fail. If you only build on x86, you will not see these locally.
2. **`./gradlew checkstyleMain` is not the CI gate.** Use the exact module-scoped command in checklist step 3.
3. **Adding a config key without a comment fails CI**, even if the key itself is valid.
4. **Changing a dependency without regenerating `verification-metadata.xml` breaks the build for everyone**, not just you.
5. **Generated protobuf sources are git-ignored.** If a build error references a missing generated class, rebuild instead of creating the file.
6. **Consensus-affecting behaviour changes need a proposal / fork gate**, not just a code change. Changing how an existing transaction validates or executes will fork the network unless gated. When in doubt, ask before implementing.

## Authoritative Documentation

- **Build / run / node operation:** [README](./README.md)
- **Configuration:** [`docs/configuration.md`](./docs/configuration.md), [`docs/configuration-conventions.md`](./docs/configuration-conventions.md)
- **Protobuf protocol:** [`docs/protobuf-protocol-document.md`](./docs/protobuf-protocol-document.md) is the maintained reference (the copies under `protocol/src/main/protos/` are outdated).
- **Extending / deployment:** the [`docs/`](./docs) directory (customized actuator, modular deployment).
- **Contributing:** [CONTRIBUTING.md](./CONTRIBUTING.md) (workflow, coding style, commit/PR conventions).
- **Security policy:** [SECURITY.md](./SECURITY.md) (supported versions, vulnerability disclosure).

## Commit & PR Convention

Commit messages and PR titles follow `type(scope): description`. The allowed types, the full scope list and the subject rules are defined in [CONTRIBUTING.md](./CONTRIBUTING.md#commit-messages) — follow it there.

PR titles and descriptions are validated in CI by `.github/workflows/pr-check.yml`. Fill in `.github/PULL_REQUEST_TEMPLATE.md`.
