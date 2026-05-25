# Node Configuration Guide

This guide explains the two-layer configuration system used by java-tron and walks through the most common customizations a node operator needs.

## How Configuration Files Work Together

java-tron uses [Typesafe Config](https://github.com/lightbend/config) and applies configuration in priority order at startup:

| File | Location | Purpose |
|------|----------|---------|
| `reference.conf` | Bundled inside the jar (`common` module) | Declares every parameter with its default value |
| Bundled `config.conf` | Bundled inside the jar (`framework` module) | Shipped template; active only when `-c` is omitted |
| Your config file (e.g. `node.conf`) | Operator-supplied, passed via `-c` | Overrides values that differ from defaults; replaces the bundled `config.conf` entirely |

**Loading priority:** values in your config file always win. Any parameter your file omits is automatically filled in from `reference.conf`. You never need to copy the entire `reference.conf` into your own file — only include the parameters you actually want to change.

```
startup resolution order (highest wins):
  1. your config file    (passed with -c; replaces bundled config.conf)
  2. bundled config.conf (only when -c is omitted)
  3. reference.conf      (always loaded; fallback for every key)
```

`reference.conf` is the authoritative source of truth for every parameter name and its default. When in doubt, consult that file to see what a parameter does and what value the node will use if you leave it out.

## Starting a Node with a Config File

```bash
# Using the distribution script
java-tron-1.0.0/bin/FullNode -c /path/to/node.conf

# Using the jar directly
java -jar FullNode.jar -c /path/to/node.conf

# SR (Super Representative) mode
java-tron-1.0.0/bin/FullNode -c /path/to/node.conf -w
```

If `-c` is omitted, the node loads the `config.conf` bundled inside the jar (the same file shipped with the distribution) merged with `reference.conf` as fallback. The bundled file already enables discovery/persist for mainnet operation. For production, copy it out, edit, and pass the edited copy via `-c` to make your configuration visible to operators.

## Minimal Config File

Your config file only needs to contain what you want to change. The following is sufficient for a mainnet full node:

```hocon
node.discovery = {
  enable = true
  persist = true
}

node {
  listen.port = 18888
  minParticipationRate = 15
  p2p.version = 11111   # mainnet
}

seed.node.ip.list = [
  "3.225.171.164:18888",
  "52.8.46.215:18888",
  # ... (see reference.conf for the full seed list)
]
```

## Common Configuration Sections

### Network and P2P (`node`, `node.discovery`, `seed.node`)

```hocon
node.discovery = {
  enable = true      # join the peer-discovery network
  persist = true     # save discovered peers across restarts
}

node {
  listen.port = 18888          # TCP port for peer connections
  maxConnections = 30          # maximum peer connections
  minConnections = 8           # minimum peer connections to maintain
  minParticipationRate = 15    # minimum % of active witnesses before producing blocks

  p2p {
    version = 11111            # Mainnet:11111  Nile:201910292  Shasta:1
  }
}

seed.node.ip.list = [
  "3.225.171.164:18888",
  # add more entries as needed
]
```

### HTTP and gRPC APIs (`node.http`, `node.rpc`)

```hocon
node {
  http {
    fullNodeEnable = true
    fullNodePort = 8090
    solidityEnable = true
    solidityPort = 8091
  }

  rpc {
    enable = true
    port = 50051
    solidityEnable = true
    solidityPort = 50061
    # Maximum concurrent calls per connection. 0 = no limit.
    maxConcurrentCallsPerConnection = 0
    # Idle connection timeout (ms). 0 = no limit.
    maxConnectionIdleInMillis = 0
    # Minimum active connections required before broadcasting transactions.
    minEffectiveConnection = 1
  }
}
```

To disable an API endpoint that you do not want to expose publicly, set its `Enable` flag to `false` or add endpoints to `node.disabledApi`:

```hocon
node.disabledApi = [
  "getaccount",
  "getnowblock2"
]
```

### Storage Engine (`storage`)

```hocon
storage {
  db.engine = "LEVELDB"    # "LEVELDB" or "ROCKSDB"; ARM64 requires "ROCKSDB"
  db.sync = false          # set true for maximum durability (slower writes)
  db.directory = "database"
}
```

To override the storage path for individual databases:

```hocon
storage.properties = [
  {
    name = "account",
    path = "/data/tron/account-db"
  }
]
```

### Block Production (Super Representatives)

```hocon
# Plain private key (use localwitnesskeystore for production)
localwitness = [
  "your-private-key-hex"
]

# Recommended: keystore file
# localwitnesskeystore = [
#   "/path/to/localwitnesskeystore.json"
# ]

# Required when the witness account has delegated block-signing to a separate key
# localWitnessAccountAddress = "T..."
```

### JSON-RPC (Ethereum-compatible, `node.jsonrpc`)

```hocon
node.jsonrpc {
  httpFullNodeEnable = true
  httpFullNodePort = 8545
  maxBlockRange = 5000        # max block range for eth_getLogs
  maxResponseSize = 26214400  # 25 MB
}
```

### Event Subscription (`event.subscribe`)

```hocon
event.subscribe = {
  enable = true
  native {
    useNativeQueue = true
    bindport = 5555
    sendqueuelength = 1000
  }
  topics = [
    { triggerName = "block",       enable = true,  topic = "block" },
    { triggerName = "transaction", enable = true,  topic = "transaction" },
    { triggerName = "solidity",    enable = true,  topic = "solidity" }
  ]
}
```

### Rate Limiting (`rate.limiter`)

```hocon
rate.limiter = {
  # Available strategies:
  #   GlobalPreemptibleAdapter  — semaphore-based, paramString = "permit=N"
  #   QpsRateLimiterAdapter     — node-wide QPS cap, paramString = "qps=N"
  #   IPQPSRateLimiterAdapter   — per-IP QPS cap, paramString = "qps=N"

  http = [
    {
      component = "GetAccountServlet",
      strategy = "IPQPSRateLimiterAdapter",
      paramString = "qps=10"
    }
  ]

  global.qps = 50000
  global.ip.qps = 10000
}
```

### Dynamic Config Reload (`node.dynamicConfig`)

When enabled, the node re-reads your config file periodically without restarting:

```hocon
node.dynamicConfig = {
  enable = true
  checkInterval = 600    # seconds between checks
}
```

Not all parameters support hot-reload. Parameters that affect node identity, genesis block, or database layout require a full restart.

## Parameters You Should Not Change

| Parameter | Reason |
|-----------|--------|
| `crypto.engine` | Changing the key-derivation algorithm will fork the node |
| `genesis.block.*` | Must be identical on every node in the network |
| `committee.*` | Controlled by on-chain governance proposals; manual overrides are for private chains only |
| `node.p2p.version` | Must match the network (11111 for mainnet) |
| `enery.limit.block.num` | Intentional typo preserved for backward compatibility; do not rename |

## Applying a Config Change

1. Edit your config file — only add or change the keys you need.
2. If `node.dynamicConfig.enable = true`, wait up to `checkInterval` seconds; the node picks up the change automatically.
3. Otherwise, restart the node: `kill <pid>` then relaunch with the same `-c` flag.
4. Check startup logs for a `[config]` line confirming the file was loaded and watch for any `ERROR` lines about unknown or invalid keys.

## Viewing Effective Configuration

At startup, the node unconditionally logs a summary of key parameters under `Net config`, `Backup config`, `Code version`, `DB config`, and `shutDown config` headers (see `Args.logConfig()` for the exact fields). For parameters not in this summary, you must inspect runtime behavior or consult `reference.conf` directly — the full merged configuration is never dumped.

Note: `node.openPrintLog` is a separate flag that controls runtime verbosity of P2P/inventory/pending-tx logs, not startup config logging.

## Full Reference

Every parameter with its default value and an inline comment is documented in:

```
common/src/main/resources/reference.conf
```

When you need the authoritative default for a parameter or want to understand what a key does, consult that file directly.
