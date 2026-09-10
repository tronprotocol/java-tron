# State Archive Harness 当前现实使用状态

- 状态日期：2026-09-10
- 权威范围：当前验证用途与最近已记录现场
- 重要：这是文档快照，不是live monitor；任何远端操作前必须重做只读preflight。

## 总览

| 节点 | 当前角色 | 最近现实使用 | 当前证据边界 |
|---|---|---|---|
| `tron-apse1-amd-002` | 最终性能验证 | Verified `53702079ee`隔离canary正常轮转PASS但WAL恢复FAIL，已回滚到`ddb0d6ef33` | rollback run active且越过candidate高度；crash/recovery与production Gate保持OPEN |
| `tron-apse1-arm-001` | 主要开发辅助 | fd69340ca3 P66同Snapshot/并行产物，原runtime升级并两次clean reopen，联网继续 | 83,836,637旧基线与83,836,754新写入重开authority一致；100样本0.729 block/s非A/B，crash/production Gate仍OPEN |
| `tron-apse1-arm-002` | 次要辅助/备用 | PathState保持disabled；100k Archive窗口继续同步，并提供52,246块/23,229,129交易增长统计 | auxiliary capacity evidence；不提供PathState/common-checkpoint或最终性能验收 |
| `tron-apse1-amd-001` | Geth核心外部架构对照观察组 | Geth v1.17.4 path archive Phase 1执行到约25.50M；本机与集中监控可用 | external control evidence；当前历史RPC不可用，不替代AMD java-tron Gate |

## Geth核心外部架构对照组

- 2026-09-09只读现场：Geth commit `36a7dc72`，unit/PID为`geth-world-state-archive.service/182838`，参数
  `--state.scheme path --history.state 0 --history.trienode -1 --syncmode full --cache 6144`，Pebble加ancient state
  freezer；Geth/Lighthouse unit均active；
- 同墙钟近似窗口：Geth 457块/167,971交易/99秒，即4.616 block/s、1,696.68 tx/s；AMD最新Hot/Common为
  40块/10,562交易/98秒，即0.408 block/s、107.78 tx/s。链与持久语义不同，仅用于架构数量级对照；
- 三个历史balance探针均返回`historical state ... is not available`，当前数字不是完整可查询archive吞吐；
- Geth 6060 metrics返回200，Alloy运行；Thanos中`instance="tron-apse1-amd-001"`的`geth-archive`和
  `node-exporter-full`均`up=1`；详细见
  [`Geth对照观察结果`](../results/geth-path-archive-control-observation-20260909.md)。
- 03:59 UTC曾把Geth从20G短暂调为29G/30G，随后按人工修正回调为`MemoryHigh=infinity / MemoryMax=22G`；
  Lighthouse保持8 GiB，两service硬上限合计30 GiB。回调后同一PID、0新增restart/OOM/kill且短窗继续推进208块；
  Geth距自身硬上限仅约0.38 GiB，主机无swap，仍须观察host available、memory PSI及kernel/cgroup OOM。

## AMD-002：最终性能节点的当前基线

### 2026-09-06 memory-rebase修复run（历史基线，已被后续部署取代）

- 历史候选：GitHub Verified `37723917cbddd98df067a7c9576300ebd9f14725`；AMD/JDK8 JAR SHA-256
  `a50dec3f42443e8b7d29d330e9a6d84a4f39a7f3998f87bd915b66bf032a0e96`；
- unit：`amd002-archive-block2-sync-mf10-37723917cb.service`；monitor：
  `amd002-archive-block2-monitor-mf10-37723917cb.service`；当时active，后由`ddb0d6ef33`部署正常接替；
- 继续使用`/data/blade/node_mainnet/runtime-a2fc535a5a/output-directory`和既有maxFlushCount=10配置；没有
  exact-27 scan、ingest、rebuild、repair或format migration；
- 启动日志明确`Common checkpoint authority established, skip legacy checkpoint recovery`，随后三组件以
  format-v1共同head 85,136,463附着；
- 精确窗口`(85,136,463, 85,136,963]`为500块、212,456 tx、691.105秒，wall为0.7235 block/s；
  checkpoint、ProcessBlock、PathState和Archive分别为472.25、347.66、282.09和276.56 ms/block；
- JVM仍为Xms8G/Xmx18G/direct1G，cgroup仍为29G/30G。后续采样old-gen 52.41%、Full GC 0；
  `memory.high`已出现但max/OOM/kill/restart/error为0。cgroup约14.70 GiB anon和13.73 GiB file，
  不能把high直接写成PathState retained-heap复发；
- evidence：`/data/blade/node_mainnet/archive-block2-37723917cb/evidence/sync-20260906T160557Z/`。
- 30分钟夜间watch `amd002-archive-block2-nightwatch-37723917cb.service`当时active。它持续记录window throughput、
  JVM GC和OS/cgroup证据；连续90分钟不推进时先取证后正常restart且6小时冷却，disk<=80 GiB或
  memory.max/oom_kill时保护性停止。不会自动调参、删库、修复、迁移、重建或替换artifact。
- 后段同量级交易密度精确500块窗口`(85,151,099, 85,151,599]`为0.6086 block/s；PathState仍稳定在
  277.56 ms/block，checkpoint增至582.52 ms/block。30分钟watch最低窗为0.2932 block/s，RSS约21.4 GiB，
  high超过58万但Full GC、max/OOM/kill/restart仍为0；唯一peer ERROR没有引起runtime失败。
- 首个完整transaction-aware v2窗口为1,154块/439,177 tx/1800秒，即0.6411 block/s、243.99 tx/s；
  PushBlock 1519.75、checkpoint 530.82 ms/block。窗口内10 peers、0 error/restart/OOM/kill/FGC；
  old-gen端点86.86%但随后Young GC回落到78.86%。该run当时继续观察，下一candidate只增加checkpoint phase timer。
- 00:51 UTC前连续值守无自动动作/incident，RSS约22.5M KiB稳定超过3小时、Full GC持续为0；最近四窗
  0.448--0.486 block/s。v2从01:01 UTC起增加transaction、tx/s、PushBlock和checkpoint字段，首行仅建立日志游标。
- 01:31--03:31 UTC五个完整v2窗口：block/s 0.641--1.008、tx/s 205.42--243.99、tx/block由380.57降至
  212.75；checkpoint由530.82降至372.25 ms/block。RSS约22.6M KiB稳定，old-gen可回落且Full GC仍为0；
  memory.high持续增长但max/OOM/kill为0，未触发自动动作。
- 04:51--05:00 UTC rollout只读preflight再次确认FullNode、monitor、night-watch均active、0 restart；三authority
  在在线时点精确一致于durable head 85,162,803，checksum有效且连续5次无共同WAL。最后完整watch为1,246块、
  405,964 tx、0.691838 block/s、checkpoint 519.12 ms/block，0 error/OOM/kill/FGC；
- 当前是lite FullNode，四个固定高度wallet端点均关闭，JSON-RPC也disabled。因此固定高度API Gate仍OPEN；为保持配置
  不变，section 3.2仅提出“停机前H/hash + 新日志在H的blockID”continuity替代oracle，需与AMD rollout一并人工确认；
- 新candidate root/unit、pre-rollout snapshot和quarantine runtime identity均已列明并确认未占用。当前没有build、copy、
  stop/start或部署；详见[AMD只读preflight](../results/amd002-instrumentation-rollout-preflight-20260906.md)及
  [rollout plan section 3.2](../validation-operations/common-checkpoint-instrumentation-rollout-plan.md#32-待人工确认的精确amd-side-by-side-payload)。

### 2026-09-06至2026-09-07当前instrumentation run

- 人工随后明确确认section 3.2及continuity替代oracle；`ddb0d6ef33` AMD/JDK8 clean JAR SHA-256为
  `de1fc9601c684a27b3827bd2b17d3a7664a5d61dd70e9440dfc638e8d8d19879`，embedded commit正确、dirty=false；
- 旧run正常停止并在共同D=85,164,093冻结三authority；266,924 files的runtime已建立强制reflink回退基线。新run从D
  附着，并在停机前H=85,164,102输出相同blockID，continuity PASS；
- instrumentation精确500块为0.7782 block/s、280.03 tx/s；首个完整30分钟窗为1,334块、489,385 tx、0.7407
  block/s。PathState rebase prepare约216.6 ms/block，是最大稳定叶子阶段；Archive end/close约97.0 ms/block但
  P95达6.49秒/batch，是尾部第一调查对象；
- 06:22 UTC FullNode及两个observer均active、0 restart，三authority在线一致于85,166,403，0 WAL/error/OOM/kill/FGC；
  rollout与首窗Gate关闭，长期/strict A/B/production Gate仍OPEN。详见
  [AMD instrumentation结果](../results/amd002-common-checkpoint-instrumentation-rollout-20260906.md)。
- 第二个完整30分钟窗为1,236块、491,445 tx、0.6863 block/s；checkpoint为569.98 ms/block，PathState rebase
  prepare为229.14 ms/block，Archive end为106.49 ms/block且P95 6.55秒/batch。与首窗tx/block增加8.38%时，两个
  phase形态均复现；源码确认Archive end关闭target级writer并在最后引用释放时调用LevelDB `DB.close()`；
- 2026-09-07 02:59 UTC只读复核：最近实际部署仍为`ddb0d6ef33`，FullNode/monitor/night-watch均active、0 restart。
  从05:29:37到次日02:30:52 UTC约21小时推进46,752块/75,675秒，即0.6178 block/s、1.619 s/block、约主网产块速度
  1.85倍；最近12/6/3小时分别为0.6232/0.5438/0.5787 block/s，最新30分钟为0.7285 block/s。42个窗口范围
  0.4528--1.0827 block/s，个别窗口超过1 block/s但长期目标未通过；
- 同一时点RSS约20.1 GiB、cgroup current约29.0 GiB，`memory.high=1,907,783`，但max/OOM/kill、Full GC和restart均为0。
  high只证明reclaim/throttling压力，不单独证明泄漏或解释全部同步差距；
- 2026-09-07 03:07 UTC同一runtime在线存储快照：`state-archive/`相对部署前reflink基线增加5,207,171,072 allocated B，
  对应约48,229--48,599 blocks和107,145.64--107,967.64 B/block；85M block外推Archive为8.283--8.347 TiB，
  加一次当前PathState代理值为8.578--8.641 TiB。该值是online allocated方向证据，不是clean-stop整节点容量Gate；
  详见[AMD存储外推](../results/amd002-online-storage-extrapolation-20260907.md)；
- 06:30 UTC三个unit仍active、0 restart，0 error/WAL/OOM/kill/FGC，`/data`可用约270.3 GiB。已形成唯一待选候选
  [`CCI-O01`](../../design/ai/commit-recovery/common-checkpoint-serving-index-handle-reuse-candidate.md)：只改变writer
  物理handle生命周期并补齐runtime关闭所有权；尚未实现或证明收益。

以下`e92aae0c89`材料保留为修复前历史基线，不代表当前运行identity。

- 机型：AWS `c6a.4xlarge`，AMD EPYC 7R13，16 vCPU，约30 GiB RAM；
- 数据盘：1 TiB EBS gp3/XFS，历史确认7,500 IOPS / 250 MiB/s；
- 历史候选：`feature/archive_block2@e92aae0c8922726f4b255764a7dd1981fe8a9ebb`，GitHub Verified；
- AMD/JDK8 JAR SHA-256：`440ef66647dbeb74c4c33c914339f266bc5a185248af3f93f60c80acac2ae939`；
- 一致输入是已完成exact-27、super和format-v1 PathState的冻结LevelDB head `85,130,544`：
  `/data/blade/node_mainnet/archive-block2-bef5e53c-ready-85130544-20260904T2048Z/output-directory`；
- 当前独立runtime为`/data/blade/node_mainnet/runtime-a2fc535a5a/output-directory`，unit为
  `amd002-archive-block2-sync-mf10-e92aae0c89.service`；`formatVersion=1`、P2P已开启；
- 兼容接管只把已验证legacy `CURRENT`转换为共同baseline，没有运行exact-27 ingest/rebuild；固定高度正常关闭再启动
  后再次在85,130,544附着，启动约1秒打开全部PathState store，无scan/redo/error；
- 2026-09-05 09:49 UTC抽取二进制marker验证：Chainbase `CHAINBASE_CURRENT`与PathState `CURRENT`
  除各自magic外identity完全相同；Archive `READABLE`的format identity、payload digest、last epoch/block/hash和
  state root逐字段一致，三文件checksum均正确；该证据才是设计中的三authority共同checkpoint；
- 2026-09-05 09:53 UTC同步head至少85,130,824，8个active peer，unit无重启、无OOM；cgroup在线由
  `25G/26G`调为`MemoryHigh=29G/MemoryMax=30G`，JVM仍为`Xmx=18G`。调整后`memory.high`事件停止增长，
  但逐块checkpoint仍约1.5--2.6秒，不能把同步慢单独归因于cgroup；
- 人工决定后已把`storage.snapshot.maxFlushCount`由1改为10，新配置SHA-256为
  `1ee65e2fb5a084dead602734c6c335201fcc77c767b58773078ef2069ec3e6c8`。旧run正常关闭到`close end`；
  非整批停止前durable marker为85,130,951、API head为85,130,979，shutdown把8块尾批共同发布到
  85,130,959，三authority保持一致；重启准确从该head附着并恢复同步；
- 10-block batch首窗flush为4.1--6.7秒/批，约0.41--0.67秒/块；09:56:48--09:58:06推进
  74块，约0.95 block/s，高于主网约0.333 block/s并具备净追赶能力。重启后flush短窗约1.38--3.40秒/批；
- 2026-09-05 10:28 UTC只读复核monitor：10:02:02--10:28:06连续1,564秒推进1,298块，墙钟约
  0.830 block/s；8 connections、0 error、0 restart、0 OOM。RSS由8,742,536 KiB升至14,133,892 KiB，
  cgroup peak 28,923,559,936 B；同窗`/data`可用字节减少31,243,923,456 B，主要受reflink CoW、SST生命周期
  和compaction影响，不能当作逻辑Archive净增长，但必须作为现实停止条件继续观察；
- `e92aae0c89`保持相同runtime/config/JVM/cgroup完成精确500块对照：521.691秒、176,691交易、
  0.9584 block/s；50个checkpoint对应50次serving-index open，checkpoint为229.06 ms/block。相对旧窗
  blocks/s提升28.9%、checkpoint摊销下降47.2%，但新窗tx/block高21.9%，证据是online attributed而非严格A/B；
- 对照后正常关闭到`close end`，共同durable head为85,133,781；三authority文件hash在重启前后不变，重启从
  该head附着并跳过legacy recovery。没有运行ingest/rebuild，也没有修复或删除数据库；
- 持续证据位于`/data/blade/node_mainnet/archive-block2-e92aae0c89/evidence/maxflush10-20260905/`，
  monitor unit为`amd002-archive-block2-monitor-mf10-e92aae0c89.service`；
- 2026-09-05 13:01 UTC长窗复核：有效monitor样本在6,727秒推进2,636块，仅0.3919 block/s；
  `memory.high`首次非零后的样本段约0.1822 block/s，最近80块按PushBlock cost约0.0391 block/s。
  high累计206,806但max/OOM/OOM-kill/restart为0；checkpoint通常约3--4秒/10块，约25秒停顿主要落在
  prepare/trie/nodePlan。15秒device样本未饱和。该证据标记为退化相关性，尚未证明内存限流是唯一根因；
- 2026-09-05 13:10 UTC JVM只读诊断把直接机制收敛为Full GC thrashing：18 GiB old gen使用99.99%，
  `Allocation Failure`触发约25秒Full GC，累计109次/2,638.593秒；VM Thread约占一核而host约93.5% idle，
  CPU无cgroup throttle且I/O pressure低。PSS约26 GiB、几乎全为anonymous。对象保留源仍未知，未运行
  histogram/dump，也未停启或调参；
- 13:13--13:15 UTC续观测：unit仍active且0 restart，但两次API均3秒超时；连续三块各耗时约51.5--52.1秒，
  表明节点仍间歇推进而非死锁。cgroup约31.14 GB，`memory.high`在20秒增加139次至211,835，max/OOM仍为0；
  `jstat`也无法attach。唯一ERROR为peer非法同步范围，不是数据库或三authority错误；本轮未执行侵入诊断或停启；
- 人工授权后于13:20 UTC冻结pre-stop evidence并正常停止，未发送kill。systemd约44秒后返回，shutdown执行一次
  2,002 ms共同checkpoint并记录`close end`；主unit/monitor均inactive且无目标FullNode进程。Chainbase、PathState、
  Archive都在85,136,463，format/payload/block hash/state root一致、marker checksum有效、共同WAL为0；没有重启、
  删除、修复或重建。最后完成块85,136,482之上的19块属于正常丢弃的可逆内存窗口；
- 2026-09-05 13:20 UTC源码审计确认PathState最新head的28个Trie snapshot各自通过`parent`保留全部历史
  snapshot和旧node graph；外层history限长无效，normal common checkpoint没有PathState内存rebase callback。
  该机制与运行增长一致但无histogram字节归属。建议正常停止当前run并不重启，等待人工授权；
- 停止的`d0a7802dc2` format-v2 partial在Account 116,000,000行、PathState约18G且无`CURRENT`，继续保留但不再是候选；
- `bef5e53c`曾完成27/27和legacy顶层`CURRENT`，但后续同步在block `85,133,105`发生VM receipt
  `SUCCESS -> OUT_OF_TIME`不一致；正常重启又暴露Chainbase/PathState恢复head不一致。该旧unit已停止，
  运行库和故障副本原位保留，没有回退、修复或删除；
- 日志`checkpoint v2 recover success`仅表示legacy Chainbase checkpoint v2，不得记为三authority共同checkpoint成功。

详细时间线和不断变化的unit/head/磁盘数据见
[AMD-002准备与同步记录](../../../../.dev_ops/depoly_node/records/2026-09-04-amd-002-archive-sync-prep.md)。

## ARM-001：主要开发辅助

### 当前同步持久化对照校准（2026-09-10）

- ARM-001 文档曾记录 r15 的同步 durable 配置，但 2026-09-11 现场 live PID `1665805` 实际使用
  `config-p66-live.conf`：JDK17、Chainbase/PathState/Hot/serving 均 RocksDB，`pathStateRoot.mode=shadow`；因此
  r15 记录不是当前 live 配置，不能用于当前性能对照。
- AMD-002 当前 live PID `2725661` 实际使用显式 JDK8、Chainbase LevelDB、同步 PathState/Archive 语义，MemoryHigh/Max
  为 29/30 GiB；JDK17+RocksDB9.7.4 仅在隔离候选窗口验证，未切换 live。
- 因此此前“ARM 异步/AMD 同步”的速度归因已撤销。后续比较必须绑定 DB engine/JNI、JDK、JVM/cgroup、config hash、
  起始 head/hash、tx/block 与窗口；不能把 ARM 的历史 async 窗口与 AMD 的 synchronous 窗口直接比较。

- `ddb0d6ef33` ARM/JDK17 clean JAR SHA-256为
  `7921af4c80d7e46b303c33c4c8b25f407057ad90607ffc9c3cfa82c6a3cd4203`，embedded commit正确、
  `git.dirty=false`；
- head-38 MR-06 base的两个独立reflink副本完成13次apply关联和12,594-byte共同WAL SIGKILL；首次恢复唯一
  `mode=recover`到head 39，第二次为zero-action，恢复后四authority hash逐项不变；
- apply阶段中Archive materialize均值23.28 ms且12/13次为列出的最大子阶段；Chainbase materialize均值21.50 ms，
  redo/runtime total均值74.24/78.77 ms。由于fresh私链无真实交易且r5并发运行，只用于决定AMD观测字段；
- 最终candidate进程、open fd和WAL均为0；r5仍为PID 747996、active、0 restart。证据根为
  `/data/blade/common-checkpoint-instrumentation-ddb0d6ef33`；
- `37723917cb` MR-06根：`/data/blade/common-checkpoint-memory-rebase-37723917cb`；ARM JDK17 JAR SHA-256
  `89420091236810c13ec030b06b78fdd9b41a148079de78ea181845f006078788`；
- 为避免隐式格式迁移，旧format-v2隔离副本只记录fail-closed，不被打开或重建；验证改用fresh format-v1私链；
- 命中11,141-byte共同WAL后精确SIGKILL；首次恢复退役WAL，第二次恢复仍在head 38附着，四个authority marker
  checksum逐项稳定；r5的PID、restart count和数据路径未变化；
- 四个新增memory-rebase测试4/4以及framework main/test Checkstyle通过。完整ARM集合的两个失败分别来自
  aarch64 LevelDB JNI不可用和RocksDB 9.7 OPTIONS文本差异，不能将其写成完整ARM suite全绿；
- fresh私链接近实时出块导致effective flush近似逐块，不能当作maxFlushCount=10性能样本。header diagnostic
  MISMATCH仍是独立OPEN Gate。

以下为更早的ARM开发证据。

- 本轮候选：base `bef5e53c`之上的36个未提交文件；规范化内容manifest
  `211c6c89fa0d0b35e0e66eba41b8a80d43b0fa139efdfb9a568565219a435b22`；
- ARM JAR SHA-256 `ee676ad34406ce4f630f53d76ff073f19fa952303f6481e4f0eaf2d9b17c3acd`，配置SHA-256
  `9a99a4d5750bdc36ca85add5388b929cf838b20940194f4bbd890657450b754a`；
- 私链block-final实测PathState `durableWrites=0, journal=0`；SIGKILL保留11,227-byte共同WAL，恢复后WAL退役并在
  head 24附着，第二次启动head与三authority marker hash不变；
- 证据根：`/data/blade/common-checkpoint-dev-20260905-ec90022d`；原始失败副本和两个后续故障副本全部保留；
- 2026-09-05 03:32 UTC可用661,668,003,840 bytes，所有本轮FullNode unit已停止；
- 保留唯一full-valid base和r5历史证据，不用next-format代码原地打开r5目录；
- 每次运行前重查process、mount/device/filesystem、runtime/CURRENT、base identity、evidence和cgroup；
- 私链持续出现`Path-state header diagnostic: result=MISMATCH`，target-specific marker随checkpoint增长；二者和
  fresh mainnet容量均未关闭。本机结论仅为`development evidence`，不能替代AMD-002最终性能验收。

详细现场见[ARM-001当前部署](../../../../.dev_ops/current_depoly/tron-apse1-arm-001.md)。

## ARM-002：次要辅助/备用

- 当前保持`storage.pathStateRoot.enabled=false`，不复制、创建或启动PathState runtime；
- 当前允许作为Archive增长预估样本，与ARM-001 current stateRoot固定footprint分列后组合；不得称为同机A/B；
- 可用于Historical query固定语料、replay oracle、查询脚本和不需要PathState的辅助回归；
- 只在ARM-001不适合承载某个辅助任务时作为备用，不自动接管最终性能验证；
- 证据一律标记`auxiliary`。

详细现场见[ARM-002当前部署](../../../../.dev_ops/current_depoly/tron-apse1-arm-002.md)。

当前容量结果见[ARM-002 Archive增长与ARM-001 stateRoot容量预估](../results/storage-growth-arm002-state-root-arm001-20260905.md)。
当前最高优先级问题的背景、指标和关闭条件见
[`HT-001同步速度与可持续性`](../../problem/focus/state-root-sync-speed.md)。

## 当前下一验证链

1. 已完成：AMD-002以冻结`a2fc535a5a`、JAR、config和format-v1 ready输入完成无重建兼容接管；
2. 已完成：固定高度正常关闭/reopen并验证三authority共同checkpoint与zero-scan；
3. 已完成：部署`e92aae0c89`消除同一redo内重复open，取得固定500块对照并通过正常停机/恢复Gate；
4. 已完成：ARM-001对`37723917cb`执行真实WAL crash、恢复、第二次恢复和短窗资源MR-06；
5. 已完成：AMD-002以`37723917cb`冻结首个500块statistics；PathState没有明显回退，checkpoint为第一大项；
6. 进行中：保持run不变取得30分钟以上长窗，持续观察FGC、old-gen、cgroup anon/file、吞吐和磁盘；
7. 已完成：`ddb0d6ef33`在ARM-001通过13次phase关联、真实WAL crash和second recovery；Archive materialize为当前
   development样本均值最大子阶段；
8. 已完成：AMD `ddb0d6ef33` clean build、normal stop、reflink基线、continuity、3-checkpoint smoke及observer接管；
9. 已完成：首个精确500块和完整30分钟phase窗口；PathState rebase prepare为均值主项，Archive close为尾部主项；
10. 已完成：第二个完整窗口复现两项phase形态，源码关闭路径已定位，并形成单变量`CCI-O01`及A/B、风险和磁盘控制表；
11. 下一步等待人工选择是否准备`CCI-O01`本地实现与测试；未选择前保持live run及全部远端现场不变；
12. 同步完成后按[`AI-MANAGEMENT.md`](../../execution/AI-MANAGEMENT.md)暂停修改并执行项目控制恢复；
13. header diagnostic、target-marker retention、reorg/fault/power-loss与production acceptance仍是独立开放Gate。
