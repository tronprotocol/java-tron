1. snapshot history文件的切分（done）

4. checkpoint逻辑处理（done）
1. leveldb的读写（done）
2. transactionCache的逻辑（done）
- 首先新增一个持久化存储localStore
- 在fullnode启动时，从localStore中初始化cache，原来的初始化逻辑保留，用于localStore为空时依然能够初始化。
首先读取localstore，然后再读取block，如果存在重复数据则覆盖，这样也不会造成数据错乱。
- 一个未升级的节点，升级后第一次启动时，localStore为空，此时cache中的数据从block中获取，同时需要将初始化的数据写入localStore。
- cache写入时，会做remove旧数据的判断。如果需要移除时，同时需要将localStore中的旧数据移除。
- cache目前是一个内存级的revokingdb，在flush时，会将impl中的内存数据合并至自己的hashmap中。需要处理的逻辑：在合并时，同时将合并的数据写入localStore中。
而且应该先写localStore，然后在写hashmap。
note：目前肉眼观测日志，新加了localstore后的区块同步速度几乎没有受到影响。目前来看方案可行。


3. 合并
命令行提示：请确保磁盘剩余容量大于两倍的history数据集，合并过程中会复制一份history至fullnode数据目录，防止因磁盘空间不够导致合并失败。
maybe you can delete the history.tar.gz first.

1. 首先判断history的最高块和database目前缺失的最高块比较，如果history不覆盖所有的缺失，提示不可合并
2. 并判断genesisblock是否一致
2. 将database中五个历史库拷贝至bak目录
3. 然后将history的五个历史库拷贝至database中
4. 如果history的高度比snapshot要高，进行裁剪

裁剪
- block-index： key是block num，value是blockid的hash
- block: key是blockid的hash，value是block整个区块
- trans：key是transaction的hash，value是对应的block的num
（早期的trans库存放的是TRXhash+TRXbody，后来变成hash+blocknum，然后通过blocknum去blockstore中查询对应的TRX）
- transactionRetStore：key是block num，value是block中ret的结果集
- transactionHistoryStore：这个库直接拷贝即可，不考虑合并或切分，已废弃

假如目前的history的区块高度为100010，snapshot记录的区块高度是100000，history多了10个区块，则需要删除100000~100010之间的区块。
for循环从100010-100000开始遍历，取i=100010，
{
    从block-index中获取i的block hash，然后从blockstore中获取block i，
    然后从block中获取该block的所有transaction hash，然后依次for循环遍历transtore{
       删除transStore中对应的交易hash
    }
    然后删除blockstore中的block i
    然后删除transactionRetStore中对应的key为i的value
    然后删除block-index中的key为i的value
    historyStore暂时忽略
}

5. 合并
将bak目录中五个数据库依次遍历，依次合并至对应的数据库中，完成history和当前节点数据的合并。合并后删除snapshot标志，表明该节点为全数据节点
注：这5个库因为都是追加写入，接收一个区块就写一份数据，所以可以直接追加。

