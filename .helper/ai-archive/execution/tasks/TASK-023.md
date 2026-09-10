---
id: TASK-023
type: task
title: HLT-002 AMD JDK17 RocksDB long-sync performance
status: in_progress
priority: high
parent: HLT-002
owner: AI-archive
created: 2026-09-10
updated: 2026-09-10
active_worktree: .
active_branch: feature/archive_block2
---

# HLT-002 AMD/JDK17/RocksDB长期同步性能

## 目标

在 AMD-002 上建立与 ARM-001 可比的 JDK17 + RocksDB 高版本长期同步基线，逐项验证 JVM、JNI、native options、cgroup/cache 与磁盘基础设施对 PushBlock、PathState、Common checkpoint、State Archive 和 compaction 的影响。

## 当前基线

- AMD-002 live control：JDK8、Chainbase LevelDB、PathState/Hot/serving RocksDB 5.15.10，Xmx18G/direct1G，MemoryHigh=29G、MemoryMax=30G、swap=0。
- `abd9b1bbfb`：RocksDB JNI 9.7.4 在当前完整 runtime reflink 上启动兼容通过；P2P 窗口 60s=8 blocks、180s=20 blocks（约0.11–0.13 block/s），随后回滚；候选 runtime/evidence 保留。
- AMD-002 已安装 OpenJDK 17.0.20，路径 `/usr/lib/jvm/java-17-openjdk-amd64/bin/java`；系统默认 `java` 已切换为 JDK17，因此 control 必须使用显式 JDK8 路径 `/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java`。
- `9ea27f2797` 增加 `-Dtron.java17.x86.candidate=true` 的显式 x86 JDK17 候选门禁；JDK17+RocksDB9.7.4 候选在现有快照上 P2P-disabled 启动成功，27 个 PathState store、Common checkpoint 与 State Archive 数据库均打开。
- 同一候选 P2P 窗口约 120 秒从区块 85150075 到 85150103（+28，约 0.23 block/s），未见 ERROR/Exception；窗口结束后已恢复 JDK8 control。该样本起点为候选旧快照，不能与实时 control 直接作严格 A/B 晋级结论。
- 同快照顺序 A/B：JDK8 control 120 秒 `85150420→85150443`（+23，约 0.192 block/s）；JDK17+RocksDB9.7.4 120 秒 `85150409→85150435`（+26，约 0.217 block/s）。候选约高 13%，但受顺序网络窗口与起点微差影响，仅作方向性证据。
- 第二组候选从最新 live 停机快照启动时触发 `Asset num is wrong!`，未进入 API/性能窗口；失败 runtime 的 `database/block` 仅约 16 KB、`CURRENT=MANIFEST-000002`，而 live 为约 4.7 GB、`MANIFEST-001533`，确认是 reflink 复制未完成/残缺导致的空 block DB。已停止并恢复 control；后续必须先做 regular-file/manifest/marker 完整性校验。
- 进一步日志确认失败 clone 的 `latestBlockHeaderNumber=0`（`Total block:1`），与残缺 block DB 一致；现有失败目录可作为复制完整性负例。
- 使用独立长时 copy unit 重新复制后，block DB 校验为 769 文件/5,050,533,467 字节，PathState 3272 文件，Archive hot 149 文件，关键 CURRENT/MANIFEST hash 与源一致。后续 smoke 因 control 已恢复、Prometheus 端口冲突而不计入性能/启动结论。
- 第二份完整快照在 control 保持停止期间启动 JDK17 smoke 成功，HTTP/PBFT API 全部启动并返回快照 head；候选随后停止并恢复 JDK8 control。
- 在完整性校验通过且 control 全程停止的前提下，JDK17+RocksDB9.7.4 P2P 窗口从 `85151013` 到 `85151045`（120 秒 +32，约 `0.267 block/s`），采样 CPU 约 471%、RSS 约 9.8 GB；窗口后已恢复 control。该值仍需与同快照 JDK8 交错复测。
- 对比校准：真正 ARM-001 为 `10.255.10.101`，当前 live 实际是 JDK8、Chainbase RocksDB、`pathStateRoot.mode=shadow`，且 unit 无 MemoryHigh/Max；AMD-002 是 JDK8、Chainbase LevelDB、同步 PathState、30 GiB cgroup。不能把 ARM 历史 shadow/无硬内存上限窗口与 AMD 同步/30 GiB窗口直接比较。
- 2026-09-11 对齐动作：ARM-001 现场确认已是 JDK17、RocksDB、shadow 模式且 `volatileSnapshotBenchmark=false`/`asyncPrepareBenchmark=false`，unit 已是 MemoryHigh/Max=30 GiB；代码不接受字面 `mode=sync`。AMD-002 已切换 JDK17+RocksDB9.7.4 live，启动后 60 秒 `85151840→85151866`、0 restart。
- 对齐后在线观察（metrics）：ARM block-process 计数 `530→565`（约 73 秒，`0.48 block/s`），AMD `777→804`（约 76 秒，`0.36 block/s`）。两端仅 Chainbase engine 仍不同，但该自然流量窗口未锁定 tx/block、peer/network 和时间交错，暂作基线不作因果结论。

## 实验顺序

1. 固定 JDK8/5.15.10 control 的 config、runtime、head/hash、JVM/cgroup/cache 与 30m 观测合同。
2. 只切换 JDK17，保持 5.15.10、配置和运行目录语义不变，完成 P2P-disabled 启动与短窗。
3. 在 JDK17 control 上只切换 RocksDB JNI 9.7.4，完成兼容/恢复门。
4. 最后单变量调 native options（block cache、write buffer、max open files、compaction），每次保留失败副本。
5. 通过固定输入后再进行 30m/6h 长期窗口；性能、恢复、容量和生产切换分别判门。

## 禁止

不得原地转换 LevelDB；不得把 JDK 安装、JNI 升级和 options 调优合并成不可归因的一轮；不得删除 control 或失败 runtime；不得以短窗提升关闭长期性能 Gate。

## 下一动作

在显式 JDK8 control 下固定一次同步窗口；随后只启用 `-Dtron.java17.x86.candidate=true` 做 JDK17+RocksDB9.7.4 P2P 短窗，再决定是否进入 native options 与长期窗口。候选启动失败或性能不足时保留 runtime/evidence 并恢复 control。
