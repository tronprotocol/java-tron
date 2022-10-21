# Toolkit Manual

This package contains a set of tools for Tron, the following is the documentation for each tool.

## DB Query

DB Query provides kv queries for a single database.

Parameter explanation:
 
- `-d | --db-path`: specify the database directory
- `-k | --key`: default using the String-UTF8 parsing this key, you can change the default format with `--key-format`
- `-f | --key-format`: the way to encode the key to byte array, only [utf8|hex] is supported
- `-h | --help`: provide the help info

Demo:

```shell script
java -jar Toolkit.jar db query -d /data/database/properties -k latest_block_header_timestamp -f utf8

java -jar Toolkit.jar db query -d /data/database/account -k 416758A9B1ED67EEAF14534CE6B770B83E4D7E7528 -f hex
```


## DB Stat

DB Query provides the state of the database.

Parameter explanation:
 
- `-d | --db-path`: specify the database directory
- `-h | --help`: provide the help info

Demo:

```shell script
java -jar Toolkit.jar db stat -d /data/database/account
```


## DB Compare

DB Compare check the consistency of two db or two db set. The summary result is stored in `/compare_timestamp/summary`.

- `-s | --src-path`: specify the src database dir, this path must be a [leveldb|rocksdb] dir or the parent dir of a set of dbs
- `-d | --dest-path`: specify the dest database dir, this path must be a [leveldb|rocksdb] dir or the parent dir of a set of dbs
- `-r | --recurse`: set `recurse=true` if you want to compare a set of dbs, and `src-path` & `dest-path` also must be the parent path. default: false
- `-e | --exclude`: using this option to ignore db when comparing, 
- `-l | --detail`: print the difference kv of db, the result is store in `./compare_timestamp/diff_detail` dir
- `-h | --help`: provide the help info

Demo:

```shell script
// compare the account db and print the difference
java -jar Toolkit.jar db compare -s /data/database1/account -d /data/database2/account -l 

// compare the account db but not print the difference
java -jar Toolkit.jar db compare -s /data/database1/account -d /data/database2/account

// compare two db set and print the difference
java -jar Toolkit.jar db compare -s /data/database1/ -d /data/database2/ -rl

// compare two db set and print the difference, skip the `block` and `account` db
java -jar Toolkit.jar db compare -s /data/database1/ -d /data/database2/ -rl -e block -e account
```

The content of detail file is like below:
```
--SRC-- key: 4108b55b2611ec829d308a62b3339fba9dd5c27151, src value: 10011a154108b55b2611ec829d308a62b3339fba9dd5c271515880a09ce3097001, dest value: 10011a154108b55b2611ec829d308a62b3339fba9dd5c271515880b0bdf2097001
--SRC-- key: 4127a6419bbe59f4e64a064d710787e578a150d6a7, src value: 10011a154127a6419bbe59f4e64a064d710787e578a150d6a75880a09ce3097001, dest value: 10011a154127a6419bbe59f4e64a064d710787e578a150d6a75880b0bdf2097001
--SRC-- key: 414676d81e16604bd8e1dcaad854842699f5ba027e, src value: 0a0a4a757374696e5f53756e1a15414676d81e16604bd8e1dcaad854842699f5ba027e2080d46148f0cefba7c32c50c881a7a8c32c9801c301b0018bec95f301, dest value: 0a0a4a757374696e5f53756e1a15414676d81e16604bd8e1dcaad854842699f5ba027e20c0b95548f0cefba7c32c50a8dfa7a8c32c9801c301b0018bec95f301
--SRC-- key: 4177944d19c052b73ee2286823aa83f8138cb7032f, src value: 0a09426c61636b686f6c651a154177944d19c052b73ee2286823aa83f8138cb7032f20c0f8f4d0a58080808001fa01241a056f776e657220013a190a154100000000000000000000000000000000000000001001, dest value: 0a09426c61636b686f6c651a154177944d19c052b73ee2286823aa83f8138cb7032f20e085fbd0a58080808001fa01241a056f776e657220013a190a154100000000000000000000000000000000000000001001
--DEST-- key: 4108b55b2611ec829d308a62b3339fba9dd5c27151, dest value: 10011a154108b55b2611ec829d308a62b3339fba9dd5c271515880b0bdf2097001, src value: 10011a154108b55b2611ec829d308a62b3339fba9dd5c271515880a09ce3097001
--DEST-- key: 4127a6419bbe59f4e64a064d710787e578a150d6a7, dest value: 10011a154127a6419bbe59f4e64a064d710787e578a150d6a75880b0bdf2097001, src value: 10011a154127a6419bbe59f4e64a064d710787e578a150d6a75880a09ce3097001
--DEST-- key: 41440d87fb70196b980cf77171c5977e54cab04f01, dest value: 1a1541440d87fb70196b980cf77171c5977e54cab04f0120a08d0648a8dfa7a8c32c, src value:
--DEST-- key: 414676d81e16604bd8e1dcaad854842699f5ba027e, dest value: 0a0a4a757374696e5f53756e1a15414676d81e16604bd8e1dcaad854842699f5ba027e20c0b95548f0cefba7c32c50a8dfa7a8c32c9801c301b0018bec95f301, src value: 0a0a4a757374696e5f53756e1a15414676d81e16604bd8e1dcaad854842699f5ba027e2080d46148f0cefba7c32c50c881a7a8c32c9801c301b0018bec95f301
--DEST-- key: 4177944d19c052b73ee2286823aa83f8138cb7032f, dest value: 0a09426c61636b686f6c651a154177944d19c052b73ee2286823aa83f8138cb7032f20e085fbd0a58080808001fa01241a056f776e657220013a190a154100000000000000000000000000000000000000001001, src value: 0a09426c61636b686f6c651a154177944d19c052b73ee2286823aa83f8138cb7032f20c0f8f4d0a58080808001fa01241a056f776e657220013a190a154100000000000000000000000000000000000000001001
```

The line started with `--SRC--` represents the difference between source database with dest database.

Meanwhile, the line started with `--DEST--` represents the difference between dest database with source database.