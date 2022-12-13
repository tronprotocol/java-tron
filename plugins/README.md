# Toolkit Manual

This package contains a set of tools for Tron, the following is the documentation for each tool.

## DB Archive

DB archive provides the ability to reformat the manifest according to the current `database`,
parameters are compatible with previous `ArchiveManifest`.

Parameter explanation:

- `-b | --batch-size`: [int] specify the batch manifest size,default：80000.
- `-d | --database-directory`: [string] specify the database directory to be processed,default：output-directory/database.
- `-m | --manifest-size`: [int] specify the minimum required manifest file size ，unit:M，default：0.
- `-h | --help`: provide the help info

Demo:

```shell script
# full command
  java -jar Toolkit.jar db archive [-h] [-b=<maxBatchSize>] [-d=<databaseDirectory>] [-m=<maxManifestSize>]
# examples
   java -jar Toolkit.jar db archive #1. use default settings
   java -jar Toolkit.jar db archive -d /tmp/db/database #2. Specify the database directory as /tmp/db/database
   java -jar Toolkit.jar db archive -b 64000 #3. Specify the batch size to 64000 when optimizing Manifest
   java -jar Toolkit.jar db archive -m 128 #4. Specify optimization only when Manifest exceeds 128M
```


## DB Convert

DB convert provides a helper which can convert LevelDB data to RocksDB data, parameters are compatible with previous `DBConvert`.

Parameter explanation:

- `<src>`: Input path for leveldb. Default: output-directory/database
- `<dest>`: Output path for rocksdb. Default: output-directory-dst/database
- `--safe`: In safe mode, read data from leveldb then put rocksdb, it's a very time-consuming procedure. If not, just change engine.properties from leveldb to rocksdb,rocksdb
  is compatible with leveldb for current version.This may not be the case in the future.Default: false
- `-h | --help`: provide the help info

Demo:

```shell script
# full command
  java -jar Toolkit.jar db convert [-h] [--safe] <src> <dest>
# examples
  java -jar Toolkit.jar db convert  output-directory/database /tmp/databse
```


## DB Lite

DB Lite provides lite database, parameters are compatible with previous `LiteFullNodeTool`.

Parameter explanation:

- `-o | --operate`: [split,merge]. Default: split.
- `-t | --type`: only used with operate=split: [snapshot,history]. Default: snapshot.
- `-fn | --fn-data-path`: the database path to be split or merged.
- `-ds | --dataset-path`: when operation is `split`,`dataset-path` is the path that store the `snapshot` or `history`,when
  operation is `split`,`dataset-path` is the `history` data path.
- `-h | --help`: provide the help info

Demo:

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

DB Move provides a helper to move some dbs to pre-set new path. For example move `block`, `transactionRetStore` or `transactionHistoryStore` to HDD,reduce storage expenses

Parameter explanation:

- `-c | --config`: config file. Default: config.conf.
- `-d | --database-directory`: database directory path. Default: output-directory.
- `-h | --help`: provide the help info

Demo:

Take the example of moving `block` and `trans`

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

```shell script
# full command
  java -jar Toolkit.jar db mv [-h] [-c=<config>] [-d=<database>]
# examples
  java -jar Toolkit.jar db mv -c main_net_config.conf -d /data/tron/output-directory
```