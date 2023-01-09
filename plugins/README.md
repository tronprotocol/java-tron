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
- `--safe`: In safe mode, read data from leveldb then put into rocksdb, it's a very time-consuming procedure. If not, just change engine.properties from leveldb to rocksdb, rocksdb
  is compatible with leveldb for the current version. This may not be the case in the future, default: false.
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

