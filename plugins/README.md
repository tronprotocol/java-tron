# Toolkit Manual

This package contains a set of tools for TRON, the followings are the documentation for each tool.

## DB Archive

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


## DB Convert

DB convert provides a helper which can convert LevelDB data to RocksDB data, parameters are compatible with previous `DBConvert`.

### Available parameters:

- `<src>`: Input path for leveldb, default: output-directory/database.
- `<dest>`: Output path for rocksdb, default: output-directory-dst/database.
- `--safe`: It is deprecated, now must is in the safe mode.
- `-h | --help`: Provide the help info.

### Examples:

```shell script
# full command
  java -jar Toolkit.jar db convert [-h] [--safe] <src> <dest>
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


## DB Lite

DB lite provides lite database, parameters are compatible with previous `LiteFullNodeTool`.

### Available parameters:

- `-o | --operate`: [split,merge], default: split.
- `-t | --type`: Only used with operate=split: [snapshot,history], default: snapshot.
- `-fn | --fn-data-path`: The database path to be split or merged.
- `-ds | --dataset-path`: When operation is `split`,`dataset-path` is the path that store the `snapshot` or `history`, when
  operation is `split`, `dataset-path` is the `history` data path.
- `-h | --help`: Provide the help info.

### Examples:

```shell script
# full command
  java -jar Toolkit.jar db lite [-h] -ds=<datasetPath> -fn=<fnDataPath> [-o=<operate>] [-t=<type>]
# examples
  #split and get a snapshot dataset
  java -jar Toolkit.jar db lite -o split -t snapshot --fn-data-path output-directory/database --dataset-path /tmp
  #split and get a history dataset
  java -jar Toolkit.jar db lite -o split -t history --fn-data-path output-directory/database --dataset-path /tmp
  #merge history dataset and snapshot dataset
  java -jar Toolkit.jar db lite -o split -t history --fn-data-path /tmp/snapshot --dataset-path /tmp/history
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


## DB Prune

Prune tool is only used for pruning the MPT data for archive node. When a Fullnode sets `stateRoot.switch = true`, 
it is a archive node and will store the history data in `stateGenesis.directory(default: output-directory/state-genesis/world-state-trie)`, 
the data volume of this database grows fast and may reach terabyte levels in a few months.
But not all the archive node want to reserve the whole history data, some may only want to reserve recently history data like three month, 
this tool can split the trie data and generate a new database which only contain the latest MPT as you specified.
When prune finished, you shoud replace the `state-genesis` by `state-directory-pruned`, 
prune may take a long time depend on the number of MPT that you want reserved.



### Available parameters:

- `-c | --config`: config file, Default: config.conf.
- `-d | --output-directory`: src output directory, Default: output-directory.
- `-p | --state-directory-pruned`: pruned state directory, Default: state-genesis-pruned.
- `-n | --number-reserved`: the number of recent trie data should be reserved.
- `-k | --check`: the switch whether check the data correction after prune
- `-h | --help`: provide the help info

### Examples:

Execute move command.
```shell script
# full command
  java -jar Toolkit.jar db prune [-hk] [-c=<config>] -n=<reserveNumber>
                                 [-p=<prunedDir>] [-d=<srcDirectory>]
# 1. split and get pruned data
  java -jar Toolkit.jar db prune -d ./output-directory -p ./state-genesis-pruned -c ./config.conf -n 1 -k
# 2. mv the prev state db away
  mv ./output-directory/state-genesis /backup
# 3. replace and rename the pruned dir 
  mv ./state-genesis-pruned ./output-directory/state-genesis
```
