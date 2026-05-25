# HOCON Configuration Conventions for Developers

This document covers the rules and patterns that developers must follow when adding or modifying configuration parameters in java-tron. Violations cause silent misreads, startup failures, or hard-to-diagnose defaults being applied instead of user-supplied values.

## Configuration Parameter vs. Constant: Which One to Use?

Before writing any code, decide whether the value belongs in a config file or in source code as a constant. Getting this wrong creates dead configuration surface (parameters that exist but are never tuned) or inflexibility (values that should be adjustable but aren't).

### Use a configuration parameter when

- **Different deployments legitimately need different values.** Port numbers, peer lists, storage paths, block-production timeouts, and rate limits vary by environment (mainnet / testnet / private chain) or by hardware capacity.
- **Operators may need to tune the value without rebuilding.** Examples: thread pool sizes, connection limits, QPS caps.
- **The value is an on/off feature flag with production-safe semantics.** The flag must be safe to flip while the rest of the system is unchanged (e.g. `node.rpc.reflectionService`, `vm.estimateEnergy`).
- **The default differs across deployment scenarios.** If the mainnet default and the private-chain default are different, it belongs in config so each can override.

### Use a constant when

- **No operator would ever need to change it.** Protocol-level numbers (address prefix bytes, transaction size ceilings, energy unit ratios) are part of the chain specification — changing them causes a fork.
- **The value is a technical limit determined by the implementation, not the deployment.** Jackson `StreamReadConstraints` (`MAX_NESTING_DEPTH`, `MAX_TOKEN_COUNT`) guard against malformed input; no legitimate request comes close to the limit and no operator tunes it.
- **The "configurability" is an illusion.** If the value is captured in a `static final` field at class-load time (before config is applied), a config key is misleading — it appears tunable but changes are silently ignored. Convert to a constant and document why.
- **The value is derived from other constants or from the Java runtime.** Use `Runtime.getRuntime().availableProcessors()` or arithmetic on existing constants; don't push the formula into a config file.
- **No code path reads the value after assignment.** A parameter that exists in `reference.conf` and propagates through `NodeConfig → Args → CommonParameter` but is never consumed by business logic is dead weight. Delete it entirely (see `receiveTcpMinDataLength` as a past example).

### The warning signs of a misplaced parameter

| Symptom | Likely problem |
|---------|---------------|
| Parameter exists in `reference.conf` but `grep` finds no call site beyond the binding chain | Dead parameter — delete it |
| Value is read from a `static final` field initialized before `Args.setParam()` | Config change is silently ignored — convert to constant |
| Operator sets the value and nothing changes | Same as above, or value is clamped away in `postProcess()` |
| Parameter controls something that would cause a network fork if mismatched across nodes | Must be a constant, not configurable |
| Parameter has been at its default value in every known deployment for over a year | Candidate for removal or promotion to constant |

## How Config Keys Bind to Java Fields

java-tron uses [Typesafe Config](https://github.com/lightbend/config)'s `ConfigBeanFactory` to map a HOCON section to a Java bean automatically. The mapping algorithm is:

1. For each field `fooBar` in the bean, `ConfigBeanFactory` looks for a HOCON key named `fooBar`.
2. The bean class must expose a public setter (`setFooBar`) — in practice this is provided by Lombok `@Setter`.
3. If the key is absent from the config, the field keeps its Java default value (the one assigned in the field declaration).
4. If the key is present but the type does not match, binding fails with a `ConfigException` at startup.

The binding entry point for each top-level section looks like:

```java
// "node" section → NodeConfig bean
Config section = config.getConfig("node");
NodeConfig nc = ConfigBeanFactory.create(section, NodeConfig.class);
```

## Key Naming: Use camelCase

**All keys in `reference.conf` and `config.conf` must use standard camelCase.**

`ConfigBeanFactory` derives the expected key name from the Java setter via the JavaBean Introspector: `setFooBar` → property name `fooBar` → expected HOCON key `fooBar`. If the key in the config file uses a different casing, the binding silently skips it and the field keeps its Java default.

```hocon
# Correct
node {
  maxConnections = 30
  syncFetchBatchNum = 2000
}

# Wrong — ConfigBeanFactory cannot find these
node {
  MaxConnections = 30        # PascalCase → ignored
  sync_fetch_batch_num = 2000 # snake_case → ignored
  max-connections = 30       # kebab-case → ignored
}
```

### The PBFT Exception

Two legacy keys under `committee` (`allowPBFT`, `pBFTExpireNum`) and the HTTP/RPC fields (`PBFTEnable`, `PBFTPort`) were introduced with non-standard casing before this rule was established. They are retained as-is in the config files for backward compatibility. **Do not model new keys after them.**

For `allowPBFT` and `pBFTExpireNum`, `CommitteeConfig.normalizeNonStandardKeys()` renames them to proper camelCase (`allowPbft`, `pbftExpireNum`) before handing the section to `ConfigBeanFactory`. If you ever need to accept a non-standard key from users while binding to a standard field, follow this same pattern.

### The `is` Prefix Exception

A HOCON key named `isOpenFullTcpDisconnect` produces the setter `setIsOpenFullTcpDisconnect`, but the JavaBean Introspector derives the property name as `openFullTcpDisconnect` (stripping `is`), so `ConfigBeanFactory` looks for key `openFullTcpDisconnect`. `NodeConfig.normalizeNonStandardKeys()` renames the key at read time for backward compatibility. **Do not add new keys with an `is` prefix.**

## Nesting Depth

The CI gate enforces a hard ceiling of **5 levels** (the historical maximum in `reference.conf`). New parameters should stay within **3 levels** from the top-level section. The gap between 3 and 5 is reserved for legacy paths that already exist — it is not a license to add new deep keys.

```
level 1:  node { ... }
level 2:  node { rpc { ... } }
level 3:  node { rpc { flowControl { ... } } }   ← limit for new keys
level 4+: node { rpc { flowControl { window { ... } } } }   ← legacy only; do not add new keys here
level 6+: rejected by CI gate unconditionally
```

Each level of nesting requires a corresponding inner static bean class. If you find yourself going beyond 3 levels deep, consider flattening by moving the leaf keys up one level or using a longer camelCase key at level 2.

## Configuration Loading Order

java-tron loads configuration in two layers at startup:

```
Priority (highest wins):
  1. User config file  — passed via -c; replaces the bundled config.conf entirely
  2. reference.conf    — always loaded from inside the jar; provides defaults for every key
```

When a user passes `-c /path/to/node.conf`, the bundled `config.conf` is **not loaded at all** — it is completely replaced by the user's file. `reference.conf` is the only built-in file that is guaranteed to be read in every deployment.

When `-c` is omitted (development or quick-start), the bundled `config.conf` fills the same role a user file would: it overrides `reference.conf` defaults for the keys it declares.

The practical consequence for developers: **the default value you put in `reference.conf` is the value every production node uses.** The bundled `config.conf` only matters for users who start the node without `-c`.

## Adding a New Parameter: Checklist

When adding a configuration parameter, all four steps are required in the same commit.

### Step 1 — Add the key to `reference.conf` with its default value

`reference.conf` (in `common/src/main/resources/`) must contain every key the code reads. This is the single source of truth for defaults. Add a brief inline comment explaining the key's purpose and valid range.

```hocon
node {
  # Maximum number of transaction verifier threads. 0 = auto (availableProcessors).
  myNewOption = 0
}
```

### Step 2 — Add the field to the corresponding bean class

Add a field whose name **exactly matches** the HOCON key, with the same default value as `reference.conf`. If the field is in a sub-bean, ensure the sub-bean is mapped correctly.

```java
// NodeConfig.java
private int myNewOption = 0;   // 0 = auto
```

Lombok `@Getter` and `@Setter` on the class provide the accessor methods that `ConfigBeanFactory` needs. Do not write them by hand.

### Step 3 — Add clamping / validation in `postProcess()` if needed

Every bean's `postProcess()` (called from `fromConfig()` after binding) is where out-of-range values are clamped and cross-field invariants are enforced. Do not add defensive checks scattered through the rest of the codebase.

```java
// in NodeConfig.postProcess()
if (myNewOption == 0) {
    myNewOption = Runtime.getRuntime().availableProcessors();
}
if (myNewOption > 64) {
    myNewOption = 64;
}
```

### Step 4 — Add the key to `config.conf` only if the default is intentionally different

`config.conf` (in `framework/src/main/resources/`) is the sample user config shipped with the distribution. Only add your new key there if the value users should start with differs from the `reference.conf` default, or if the key needs a visible comment for users.

Remember: in any real deployment the user passes `-c` and the bundled `config.conf` is bypassed entirely (see [Configuration Loading Order](#configuration-loading-order)). `reference.conf` is where your default actually takes effect — make sure it is safe and correct before touching `config.conf`.

## Field Types and HOCON Value Types

| Java field type | HOCON value | Notes |
|-------------------|-------------|-------|
| `boolean` | `true` / `false` | |
| `int` / `long` | numeric | Must be a plain integer; human-readable sizes (`4m`, `128MB`) are not supported |
| `double` | numeric | |
| `String` | `"value"` | Null HOCON values must be normalized to `""` before binding (see `normalizeNonStandardKeys`) |
| `List<String>` | `["a", "b"]` | Must be read manually; `ConfigBeanFactory` does not handle `List<String>` |
| Inner bean | `{ key = val }` | The Java field type must be the inner static class |

### List Fields

`ConfigBeanFactory` handles `List<BeanType>` but not `List<String>`. Read string-list fields manually after `ConfigBeanFactory.create()`:

```java
NodeConfig nc = ConfigBeanFactory.create(section, NodeConfig.class);
nc.active = section.getStringList("active");
```

## Backward Compatibility and Legacy Keys

When renaming a key, keep reading the old key as a fallback for at least one major release:

```java
// fromConfig() — after ConfigBeanFactory binding
if (section.hasPath("oldKeyName")) {
    nc.newFieldName = section.getInt("oldKeyName");
    logger.warn("Config key [section.oldKeyName] is deprecated; use [section.newKeyName].");
}
```

Never remove the old key from this fallback read without a deprecation period and a release note.

## Optional Keys (Not in `reference.conf`)

Most keys should be in `reference.conf`. Use optional keys (absent from `reference.conf`, only read if present) sparingly — only for parameters where the presence/absence itself carries meaning. Read them with `hasPath()` guards and annotate the Java field with `@Setter(lombok.AccessLevel.NONE)` to prevent `ConfigBeanFactory` from requiring the key:

```java
@Setter(lombok.AccessLevel.NONE)
private String shutdownBlockTime = "";   // "" = not set

// in fromConfig(), after ConfigBeanFactory.create():
nc.shutdownBlockTime = section.hasPath("shutdown.BlockTime")
    ? section.getString("shutdown.BlockTime") : "";
```

## Key Naming Conventions Summary

| Rule | Good | Bad |
|------|------|-----|
| Standard camelCase | `maxConnections` | `MaxConnections`, `max_connections`, `max-connections` |
| No `is` prefix | `openFullTcpDisconnect` | `isOpenFullTcpDisconnect` |
| No all-caps acronym prefix | `pbftExpireNum`, `pBFTPort`* | `PBFTExpireNum` |
| New keys: nesting ≤ 3 levels | `node.rpc.maxMessageSize` | `node.rpc.limits.size.max` |
| Java field name matches HOCON key exactly | field `maxConnections` ↔ key `maxConnections` | field `maxConns` ↔ key `maxConnections` |

\* `PBFTEnable` / `PBFTPort` are legacy exceptions; do not model new keys after them.

## Where to Find Existing Patterns

| Pattern | Reference location |
|---------|-------------------|
| Standard flat scalar binding | `VmConfig.java`, `BlockConfig.java` |
| Sub-bean nesting | `NodeConfig.HttpConfig`, `NodeConfig.RpcConfig` |
| Legacy key fallback | `NodeConfig.fromConfig()` (`maxActiveNodes`, `maxActiveNodesWithSameIp`) |
| Non-standard key normalization | `CommitteeConfig.normalizeNonStandardKeys()`, `NodeConfig.normalizeNonStandardKeys()` |
| Optional PascalCase keys | `NodeConfig.fromConfig()` (`shutdown.BlockTime/Height/Count`) |
| `postProcess()` clamping | `NodeConfig.postProcess()`, `CommitteeConfig.postProcess()` |
