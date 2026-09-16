# Toolkit Manual

This package contains a set of tools for TRON, the followings are the documentation for each tool.

## DB Archive(Requires x86 + LevelDB)

DB archive provides the ability to reformat the manifest according to the current `database`, parameters are compatible with the previous `ArchiveManifest`.

### Available parameters:

- `-b | --batch-size`: Specify the batch manifest size, default: 80000.
- `-d | --database-directory`: Specify the database directory to be processed, default: output-directory/database.
- `-m | --manifest-size`: Specify the minimum required manifest file size, unit: M, default: 0.
- `-h | --help`: Provide the help info.

### Examples:

```shell script
# full command
  java -jar Toolkit.jar db archive [-h] [-b=<maxBatchSize>] [-d=<databaseDirectory>] [-m=<maxManifestSize>]
# examples
   java -jar Toolkit.jar db archive #1. use default settings
   java -jar Toolkit.jar db archive -d /tmp/db/database #2. specify the database directory as /tmp/db/database
   java -jar Toolkit.jar db archive -b 64000 #3. specify the batch size to 64000 when optimizing manifest
   java -jar Toolkit.jar db archive -m 128 #4. specify optimization only when Manifest exceeds 128M
```


## DB Convert(Requires x86 + LevelDB)

DB convert provides a helper which can convert LevelDB data to RocksDB data, parameters are compatible with previous `DBConvert`.

### Available parameters:

- `<src>`: Input path for leveldb, default: output-directory/database.
- `<dest>`: Output path for rocksdb, default: output-directory-dst/database.
- `-h | --help`: Provide the help info.

### Examples:

```shell script
# full command
  java -jar Toolkit.jar db convert [-h] <src> <dest>
# examples
  java -jar Toolkit.jar db convert  output-directory/database /tmp/database
```

## DB Copy

DB copy provides a helper which can copy LevelDB or RocksDB data quickly on the same file systems by creating hard links.

### Available parameters:

- `<src>`: Source path for database. Default: output-directory/database
- `<dest>`: Output path for database. Default: output-directory-cp/database
- `-h | --help`: provide the help info

### Examples:

```shell script
# full command
  java -jar Toolkit.jar db cp [-h] <src> <dest>
# examples
  java -jar Toolkit.jar db cp  output-directory/database /tmp/databse
```

## DB Lite(LevelDB unavailable on ARM)

DB lite provides lite database, parameters are compatible with previous `LiteFullNodeTool`.

### Available parameters:

- `-o | --operate`: [split,merge], default: split.
- `-t | --type`: Only used with operate=split: [snapshot,history], default: snapshot.
- `-fn | --fn-data-path`: The database path to be split or merged.
- `-ds | --dataset-path`: When operation is `split`,`dataset-path` is the path that store the `snapshot` or `history`, when
  operation is `split`, `dataset-path` is the `history` data path.
- `--exclude-historical-balance`: Only used with `operate=split -t snapshot`, default: false. When set to true, `balance-trace` and `account-trace` are excluded from the lite snapshot. The flag has functional impact only when the source full node ran with `historyBalanceLookup=true` (off by default; most operators are unaffected). **WARNING:** for nodes that had `historyBalanceLookup=true`, this loss is permanent — a lite node booted from such a snapshot cannot safely serve historical balance lookups (`getBlockBalance` may fail, and `getAccountBalance` may return `balance=0` when `account-trace` data is missing), and running `merge` afterwards will NOT restore the feature. If you need historical balance lookup on the resulting lite node, do **not** enable this flag. `split -t history` and `merge` ignore this flag.
- `-h | --help`: Provide the help info.

### Examples:

```shell script
# full command
  java -jar Toolkit.jar db lite [-h] -ds=<datasetPath> -fn=<fnDataPath> [-o=<operate>] [-t=<type>] [--exclude-historical-balance]
# examples
  #split and get a snapshot dataset
  java -jar Toolkit.jar db lite -o split -t snapshot --fn-data-path output-directory/database --dataset-path /tmp
  #split and get a snapshot dataset without balance-trace / account-trace (smaller snapshot;
  #historical balance lookup cannot be safely served on the resulting lite node)
  java -jar Toolkit.jar db lite -o split -t snapshot --fn-data-path output-directory/database --dataset-path /tmp --exclude-historical-balance
  #split and get a history dataset
  java -jar Toolkit.jar db lite -o split -t history --fn-data-path output-directory/database --dataset-path /tmp
  #merge history dataset and snapshot dataset
  java -jar Toolkit.jar db lite -o merge --fn-data-path /tmp/snapshot --dataset-path /tmp/history
```

## DB Move

DB move provides a helper to move some dbs to a pre-set new path. For example move `block`, `transactionRetStore` or `transactionHistoryStore` to HDD for reducing storage expenses.

### Available parameters:

- `-c | --config`: config file. Default: config.conf.
- `-d | --database-directory`: database directory path. Default: output-directory.
- `-h | --help`: provide the help info

### Examples:

Take the example of moving `block` and `trans`.


Set path for `block` and `trans`.

```conf
storage {
 ......
  properties = [
    {
     name = "block",
     path = "/data1/tron",
    },
    {
     name = "trans",
     path = "/data1/tron",
   }
  ]
 ......
}
```
Execute move command.
```shell script
# full command
  java -jar Toolkit.jar db mv [-h] [-c=<config>] [-d=<database>]
# examples
  java -jar Toolkit.jar db mv -c main_net_config.conf -d /data/tron/output-directory
```

## DB Root(LevelDB unavailable on ARM)

DB root provides a helper which can compute merkle root for tiny db.

NOTE: large db may GC overhead limit exceeded.

### Available parameters:

- `<src>`: Source path for database. Default: output-directory/database
- `--db`: db name.
- `-h | --help`: provide the help info

## DB Backfill-Bloom

DB backfill bloom rebuilds missing historical SectionBloom indexes from transaction results stored in `transactionRetStore`, enabling `eth_getLogs` to filter by address and topics. This is useful for historical blocks processed by versions before v4.8.1 while JSON-RPC filtering (`isJsonRpcFilterEnabled`) was disabled. Since v4.8.1, SectionBloom indexes are generated independently of this setting.

### Prerequisites and behavior

- Stop the node and any other process using the database before running the command.
- The database directory must contain the `properties` and `transactionRetStore` databases. `transactionRetStore` must contain at least one non-zero block.
- Ensure `storage.transHistory.switch` was enabled while the historical blocks were processed. Only blocks whose transaction results are still present in `transactionRetStore` can be backfilled; this tool cannot recover missing transaction results.
- The start and end block numbers are inclusive.
- The command creates or updates the `section-bloom` database in the specified database directory.
- An existing `section-bloom` directory uses its own engine. A new one inherits the engine of `transactionRetStore`. Missing `engine.properties` is treated as LevelDB for compatibility with older databases.
- On ARM64, only RocksDB is supported. LevelDB is rejected before any database is opened or created.
- The operation is idempotent. If it is interrupted, safely rerun the same block range. Existing SectionBloom bits are preserved, and unchanged index records are not rewritten. Do not run multiple backfill processes concurrently.

### Available parameters

- `-d | --database-directory`: Parent directory containing the source databases and the destination `section-bloom` database. Default: `output-directory/database`.
- `-s | --start-block`: Inclusive start block. Omitted or `0` selects the earliest non-zero block in `transactionRetStore`. A lower value is automatically raised to the earliest available block. Negative values are rejected.
- `-e | --end-block`: Inclusive end block. Omitted or `0` selects the latest persisted block header number (`latest_block_header_number`) in `properties`. A higher value is automatically reduced to this height. Negative values are rejected.
- `-c | --max-concurrency`: Maximum number of processing threads, from 1 to 128. Default: 8. Use 4–8 for SATA SSD, 8–16 for NVMe SSD, or 1–2 for HDD. The actual concurrency does not exceed the number of sections being processed.
- `-h | --help`: Display the help message.

### Examples

```shell script
# Full command
java -jar Toolkit.jar db backfill-bloom [-d <databaseDirectory>] [-s <startBlock>] [-e <endBlock>] [-c <maxConcurrency>] [-h]

# Backfill the complete available range in the default database directory
java -jar Toolkit.jar db backfill-bloom

# Backfill blocks 1,000,000 through 2,000,000, inclusive
java -jar Toolkit.jar db backfill-bloom -s 1000000 -e 2000000

# Use a custom database directory and eight processing threads
java -jar Toolkit.jar db backfill-bloom -d /path/to/database -c 8
```

### Progress and performance

Each worker accumulates index bits for one section of up to 2,048 blocks. At the end of the section, each touched index record is read once, merged with existing bits, and written only if it changes. The Bloom write count reports actual index-record writes.

The terminal progress bar displays scanned blocks, elapsed time, and estimated remaining time. `toolkit.log` records progress every 10,000 scanned blocks and includes the percentage, elapsed time, average rate, and estimated remaining time. A section's successful-block and log-block counts are added only after its required index writes finish. If a section write fails, none of its blocks are counted as successful; rerunning the same range completes any partially written section. The final summary reports scanned and successful blocks, blocks containing logs, block/task errors, Bloom writes, duration, rates, and the concurrency used.

Performance depends on the number of logs, storage engine, disk, CPU, and database compaction. Increase `--max-concurrency` gradually while monitoring disk latency and CPU usage.

## Keystore

Keystore provides commands for managing account keystore files (Web3 Secret Storage format).

> **Migrating from `--keystore-factory`**: The legacy `FullNode.jar --keystore-factory` interactive mode is deprecated. Use the Toolkit keystore commands below instead. The mapping is:
> - `GenKeystore` → `keystore new`
> - `ImportPrivateKey` → `keystore import`
> - (new) `keystore list` — list all keystores in a directory
> - (new) `keystore update` — change the password of a keystore

### Subcommands

#### keystore new

Generate a new keystore file with a random keypair.

```shell script
# full command
  java -jar Toolkit.jar keystore new [-h] [--keystore-dir=<dir>] [--password-file=<file>] [--sm2] [--json]
# examples
  java -jar Toolkit.jar keystore new                                  # interactive prompt
  java -jar Toolkit.jar keystore new --keystore-dir /data/keystores   # custom directory
  java -jar Toolkit.jar keystore new --password-file pass.txt --json  # non-interactive with JSON output
```

#### keystore import

Import a private key into a new keystore file.

```shell script
# full command
  java -jar Toolkit.jar keystore import [-h] [--keystore-dir=<dir>] [--password-file=<file>] [--key-file=<file>] [--sm2] [--force] [--json]
# examples
  java -jar Toolkit.jar keystore import                                # interactive prompt
  java -jar Toolkit.jar keystore import --key-file key.txt --json      # from file with JSON output
```

#### keystore list

List all keystore files in a directory.

```shell script
# full command
  java -jar Toolkit.jar keystore list [-h] [--keystore-dir=<dir>] [--json]
# examples
  java -jar Toolkit.jar keystore list                                  # list default ./Wallet directory
  java -jar Toolkit.jar keystore list --keystore-dir /data/keystores   # custom directory
```

> **Note**: `list` displays the `address` field as declared in each keystore JSON without decrypting the file. A tampered keystore can claim an address that does not correspond to its encrypted private key. The address is only cryptographically verified at decryption time (e.g. by `update` or by tools that load the credentials). Only trust keystores from sources you control.

#### keystore update

Change the password of a keystore file.

```shell script
# full command
  java -jar Toolkit.jar keystore update [-h] <address> [--keystore-dir=<dir>] [--password-file=<file>] [--sm2] [--json]
# examples
  java -jar Toolkit.jar keystore update TXyz...abc                          # interactive prompt
  java -jar Toolkit.jar keystore update TXyz...abc --keystore-dir /data/ks  # custom directory
```

When using `--password-file` with `update`, the file must contain exactly two lines: the **current** password on the first line and the **new** password on the second line. Both leading/trailing whitespace within a line is preserved (passphrases with spaces are supported).

### Common Options

- `--keystore-dir`: Keystore directory, default: `./Wallet`.
- `--password-file`: Read password from a file instead of interactive prompt. For `keystore update`, the file must contain exactly two lines (current password, then new password).
- `--key-file`: Read the private key (hex, with or without `0x` prefix) from a file instead of the interactive prompt (`keystore import` only).
- `--force`: For `keystore import`, allow importing a private key whose address already has a keystore in the directory (creates an additional file).
- `--sm2`: Use SM2 algorithm instead of ECDSA (for `new` and `import`).
- `--json`: Output in JSON format for scripting.
- `-h | --help`: Provide the help info.
