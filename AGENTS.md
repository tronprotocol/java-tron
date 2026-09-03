# AGENTS.md

Guidance for AI coding assistants and new contributors working on java-tron: how to build, test, and navigate the codebase, plus the high-frequency constraints to respect. For running a node, see the [README](./README.md) and [`docs/`](./docs).

## Working principles

- Keep changes minimal and focused: only touch code related to the task. Do not refactor unrelated code, rename for style, or bundle unrelated fixes into one commit/PR.
- Do not add, remove, or upgrade dependencies unless the task requires it — dependency changes in a consensus node are high-risk and need separate review.

## Build & Test

Supported platforms: **Linux** and **macOS** only. JDK requirement is by CPU architecture: **JDK 8** on x86_64, **JDK 17** on ARM64/aarch64 (e.g. Apple Silicon Macs, or Linux aarch64 servers such as AWS Graviton). The build fails fast if the JDK major version does not match the architecture.

```bash
./gradlew clean build -x test                # build without tests
./gradlew build                              # build with tests
./gradlew test                               # run all tests
./gradlew :framework:test                    # test one module
./gradlew test --tests "org.tron.core.db.TronDatabaseTest"           # one class
./gradlew test --tests "org.tron.core.db.TronDatabaseTest.testX"     # one method
./gradlew :framework:testWithRocksDb         # RocksDB tests (x86 only)
./gradlew lint                               # Checkstyle (framework main only)
./gradlew checkstyleMain checkstyleTest      # Checkstyle main + test (as CI runs)
./gradlew jacocoTestReport                   # coverage report
```

- Main entry point: `org.tron.program.FullNode`.
- Tests run in parallel locally, serially in CI (detected via the `CI` env var); the test-retry plugin retries up to 5 times.
- On ARM64/aarch64, only the RocksDB storage engine is supported; the build forces RocksDB and skips the LevelDB tests.
- Protobuf / gRPC Java stubs are generated at build time from the `.proto` files under `protocol/src/main/protos/` (subdirectories `core/`, `api/`; via the `com.google.protobuf` Gradle plugin) and are git-ignored — rebuild after changing a `.proto`; never hand-edit or commit generated sources.

**Before pushing:**
- `./gradlew checkstyleMain checkstyleTest` and `./gradlew test` must pass.
- Do not commit build artifacts or byproducts — `*.jar`, `build/`, logs, or database files.

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

**Module dependency direction is one-way — do not introduce reverse dependencies:**

```text
framework → chainbase → common → protocol
actuator  → chainbase
consensus → chainbase / common   (only via ConsensusDelegate; never call Manager directly)
crypto    → common
```

`platform` is a leaf module (no project dependencies of its own) that `common`, `framework`, and `plugins` depend on for architecture-specific code.

## Hard Constraints

**Cross-JVM determinism** (consensus, state transition, block ordering):
- Never use `float` / `double`.
- Never depend on `HashMap` iteration order for a business decision.
- Use the DPoS slot time for produced-block timestamps, not `System.currentTimeMillis()`.

**DB / Store:**
- All writes must happen inside a `Session` / `Dialog` — no bare `put()`.
- A new store must extend `TronStoreWithRevoking<T>` and register with the `RevokingDatabase`.
- Multi-store updates must roll back fully on exception.

**Actuator:**
- Register new actuators in `ActuatorFactory`.
- Charge fees before `execute()`.
- `validate()` must not mutate state.

**Protobuf:**
- Fields may only be added — never removed or renumbered.
- Message field numbers start at `1`; the first enum value must be `0`.

**API / Threads:**
- New HTTP servlets must go through `HttpApiAccessFilter` and use `Wallet` (never inject `Manager` directly).
- New gRPC methods must join the `LiteFnQueryGrpcInterceptor` chain.
- No bare `new Thread()` — use a named Executor, shut down via `shutdown()` → `awaitTermination()` → `shutdownNow()`.

## Authoritative Documentation

- **Build / run / node operation:** [README](./README.md)
- **Configuration:** [`docs/configuration.md`](./docs/configuration.md), [`docs/configuration-conventions.md`](./docs/configuration-conventions.md)
- **Protobuf protocol:** [`docs/protobuf-protocol-document.md`](./docs/protobuf-protocol-document.md) is the maintained reference (the copies under `protocol/src/main/protos/` are outdated).
- **Extending / deployment:** the [`docs/`](./docs) directory (customized actuator, modular deployment).
- **Contributing:** [CONTRIBUTING.md](./CONTRIBUTING.md) (workflow, coding style, commit/PR conventions).
- **Security policy:** [SECURITY.md](./SECURITY.md) (supported versions, vulnerability disclosure).

## Commit Convention

`type(scope): description` (Conventional Commits), 10–72 chars, no trailing period.

- **type:** `feat` `fix` `refactor` `docs` `style` `test` `chore` `ci` `perf` `build` `revert`
- **scope:** `framework` `chainbase` `actuator` `consensus` `common` `crypto` `plugins` `protocol` `net` `db` `vm` `tvm` `api` `jsonrpc` `rpc` `http` `event` `config` `block` `proposal` `trie` `log` `metrics` `test` `docker` `version`
- **PR title:** same `type(scope): description` convention; fill in `.github/PULL_REQUEST_TEMPLATE.md`.
