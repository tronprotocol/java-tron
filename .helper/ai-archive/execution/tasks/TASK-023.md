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
- 第二组候选从最新 live 停机快照启动时触发 `Asset num is wrong!`，未进入 API/性能窗口；已停止并恢复 control。该失败说明“当前 live 直接 reflink”仍需先验证 Archive/PathState 共享身份与 marker 完整性，不能继续盲目重复启动。

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
