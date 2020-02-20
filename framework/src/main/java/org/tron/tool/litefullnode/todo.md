1. snapshot history文件的切分（done）

4. checkpoint逻辑处理（done）
1. leveldb的读写（done）
2. transactionCache的逻辑
- 首先新增一个持久化存储localStore
- 在fullnode启动时，从localStore中初始化cache，原来的初始化逻辑保留，用于localStore为空时依然能够初始化。
首先读取localstore，然后再读取block，如果存在重复数据则覆盖，这样也不会造成数据错乱。
- 一个未升级的节点，升级后第一次启动时，localStore为空，此时cache中的数据从block中获取，同时需要将初始化的数据写入localStore。
- cache写入时，会做remove旧数据的判断。如果需要移除时，同时需要将localStore中的旧数据移除。
- cache目前是一个内存级的revokingdb，在flush时，会将impl中的内存数据合并至自己的hashmap中。需要处理的逻辑：在合并时，同时将合并的数据写入localStore中。
而且应该先写localStore，然后在写hashmap。


3. 数据库的移动
5. snapshot启动后会自动创建缺失的数据库吗
6. leveldb和rocksdb的合并操作