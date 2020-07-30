# Advanced Configurations

we provide some configuration items for LevelDB and gRPC in `config.conf` file, for fine-grained performance tuning.   
You may custom these items only if you have deep understanding on them, otherwise keep them as default.

## LevelDB

You can custom LevelDB options in the `storage` part of `config.conf`, which looks like:

```
storage {
  # Directory for storing persistent data

  db.directory = "database",
  index.directory = "index",

  # You can custom these 14 databases' configs:

  # account, account-index, asset-issue, block, block-index,
  # block_KDB, peers, properties, recent-block, trans,
  # utxo, votes, witness, witness_schedule.

  # Otherwise, db configs will remain defualt and data will be stored in
  # the path of "output-directory" or which is set by "-d" ("--output-directory").

  # Attention: name is a required field that must be set !!!
  properties = [
    {
      name = "account",
      path = "/path/to/accout",   // relative or absolute path
      createIfMissing = true,
      paranoidChecks = true,
      verifyChecksums = true,
      compressionType = 1,        // 0 - no compression,  1 - compressed with snappy
      blockSize = 4096,           // 4  KB =         4 * 1024 B
      writeBufferSize = 10485760, // 10 MB = 10 * 1024 * 1024 B
      cacheSize = 10485760,       // 10 MB = 10 * 1024 * 1024 B
      maxOpenFiles = 100
    }
  ]

}

```

As shown in the example above, the data of database `accout` will be stored in the path of `/path/to/accout/database` while the index be stored in `/path/to/accout/index`. And, the example also shows our default value of LevelDB options from `createIfMissing` to `maxOpenFiles`. You can just refer to the docs of [LevelDB](https://github.com/google/leveldb/blob/master/doc/index.md#performance) to figure out details of these options.

## gRPC

You can custom gPRC options in the `node.rpc` part of `config.conf`, which looks like:

```
node {
  rpc {
    port = 50051

    # Number of gRPC thread, default availableProcessors / 2
    # thread = 16

    # The maximum number of concurrent calls permitted for each incoming connection
    # maxConcurrentCallsPerConnection =

    # The HTTP/2 flow control window, default 1MB
    # flowControlWindow =

    # Connection being idle for longer than which will be gracefully terminated
    maxConnectionIdleInMillis = 60000

    # Connection lasting longer than which will be gracefully terminated
    # maxConnectionAgeInMillis =

    # The maximum message size allowed to be received on the server, default 4MB
    # maxMessageSize =

    # The maximum size of header list allowed to be received, default 8192
    # maxHeaderListSize =
  }
}
```

## backup
You can custom backup options in the `node.backup` part of `config.conf`, which looks like:
```
node.backup {
    # my priority, each member should use different priority
    priority = 
    # members should use same port
    port = 
    # peer's ip list, can't contain mine
    members = []
}
```
policy: 
1. the one which synchronized first will become master.
2. if synchronization is completed at the same time, the one which with big priority will become master.

E.g. create backups for node A(192.168.0.100) and node B(192.168.0.100 ):
node A's configuration:
```
node.backup {
    priority = 8 
    port = 10001
    members = [
        "192.168.0.101"
    ]
}
```
node B's configuration:
```
node.backup {
    priority = 5
    port = 10001
    members = [
        "192.168.0.100"
    ]
}
```

You may refer to the source code of `io.grpc.netty.NettyServerBuilder` class to see details or just make a decision according to the brief comments above.  
