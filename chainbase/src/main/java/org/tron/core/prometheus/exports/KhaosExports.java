package org.tron.core.prometheus.exports;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.KhaosDatabase;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.prometheus.client.SampleNameFilter.ALLOW_ALL;

@Slf4j(topic = "metrics")
@Component
public class KhaosExports extends Collector {

    private static final String KHAOSDB_MEMORY_USAGE = "khaosdb_memory_usage";
    private static final String KHAOSDB_BLOCK_COUNT = "khaosdb_block_count";
    private static final String KHAOSDB_BRANCH_COUNT = "khaosdb_branch_count";

    @Autowired
    private KhaosDatabase khaosDb;

    @Override
    public List<MetricFamilySamples> collect() {
        return collect(null);
    }

    @Override
    public List<MetricFamilySamples> collect(Predicate<String> nameFilter) {
        List<MetricFamilySamples> mfs = new ArrayList<>();
        addKhaosdbMetrics(mfs, nameFilter == null ? ALLOW_ALL : nameFilter);
        return mfs;
    }

    private void addKhaosdbMetrics(List<MetricFamilySamples> sampleFamilies, Predicate<String> nameFilter) {
        if (nameFilter.test(KHAOSDB_MEMORY_USAGE)) {
            sampleFamilies.add(
                    new GaugeMetricFamily(
                            KHAOSDB_MEMORY_USAGE,
                            "khaosdb memory usage",
                            calculateMemoryUse(khaosDb)));
        }

        if (nameFilter.test(KHAOSDB_BLOCK_COUNT)) {
            sampleFamilies.add(
                    new GaugeMetricFamily(
                            KHAOSDB_BLOCK_COUNT,
                            "khaosdb block count",
                            calculateBlockCount(khaosDb)));
        }

        if (nameFilter.test(KHAOSDB_BRANCH_COUNT)) {
            sampleFamilies.add(
                    new GaugeMetricFamily(
                            KHAOSDB_BRANCH_COUNT,
                            "khaosdb branch count",
                            calculateBranchCount(khaosDb)));
        }
    }

    private double calculateBranchCount(KhaosDatabase khaosDb) {
        if (Objects.isNull(khaosDb)) {
            return 0;
        }
        double branchCount = 0;
        try {
            branchCount = calculateStoreBranchCount(Optional.ofNullable(khaosDb)
                    .map(KhaosDatabase::getMiniStore)
                    .map(KhaosDatabase.KhaosStore::getNumKblkMap).orElse(null));
            return branchCount;
        } catch (Exception e) {
            logger.warn("calculate branch count error:", e.getMessage());
            return branchCount;
        }
    }

    private double calculateStoreBranchCount(LinkedHashMap<Long, ArrayList<KhaosDatabase.KhaosBlock>> map) {
        if (Objects.isNull(map)) {
            return 0;
        }
        double branchCount = 0;
        for (ArrayList<KhaosDatabase.KhaosBlock> list : map.values()) {
            branchCount = (Objects.nonNull(list) && list.size() > branchCount) ? list.size() : branchCount;
        }
        return branchCount;
    }

    private double calculateBlockCount(KhaosDatabase khaosDb) {
        if (Objects.isNull(khaosDb)) {
            return 0;
        }
        Double blockCount = (double) 0;
        try {
            blockCount += calculateStoreBlockCount(Optional.ofNullable(khaosDb)
                    .map(KhaosDatabase::getMiniUnlinkedStore)
                    .map(KhaosDatabase.KhaosStore::getNumKblkMap).orElse(null));
            blockCount += calculateStoreBlockCount(Optional.ofNullable(khaosDb)
                    .map(KhaosDatabase::getMiniStore)
                    .map(KhaosDatabase.KhaosStore::getNumKblkMap).orElse(null));
            return blockCount;
        } catch (Exception e) {
            logger.warn("calculate khaosdb block count failed:", e.getMessage());
            return blockCount;
        }
    }

    private double calculateStoreBlockCount(LinkedHashMap<Long, ArrayList<KhaosDatabase.KhaosBlock>> map) {
        if (Objects.isNull(map)) {
            return 0.0;
        }
        double blockCount = 0.0;
        for (ArrayList<KhaosDatabase.KhaosBlock> list : map.values()) {
            new CopyOnWriteArrayList<>(list);
            if (Objects.isNull(list) || list.isEmpty()) {
                continue;
            }
            blockCount += list.size();
        }
        return blockCount;
    }

    private double calculateMemoryUse(KhaosDatabase khaosDb) {
        if (Objects.isNull(khaosDb)) {
            return 0;
        }
        Double memoryUse = (double) 0;
        try {
            memoryUse += calculateBlockMemory(Optional.ofNullable(khaosDb)
                    .map(KhaosDatabase::getMiniUnlinkedStore)
                    .map(KhaosDatabase.KhaosStore::getNumKblkMap).orElse(null));
            memoryUse += calculateBlockMemory(Optional.ofNullable(khaosDb)
                    .map(KhaosDatabase::getMiniStore)
                    .map(KhaosDatabase.KhaosStore::getNumKblkMap).orElse(null));
            return memoryUse;
        } catch (Exception e) {
            logger.warn("calculate khaosdb memory use failed:", e.getMessage());
            return memoryUse;
        }
    }

    private double calculateBlockMemory(LinkedHashMap<Long, ArrayList<KhaosDatabase.KhaosBlock>> map) {
        if (Objects.isNull(map)) {
            return 0.0;
        }
        double memoryUse = 0.0;
        for (ArrayList<KhaosDatabase.KhaosBlock> list : map.values()) {
            if (Objects.isNull(list) || list.isEmpty()) {
                continue;
            }
            for (KhaosDatabase.KhaosBlock block : list) {
                byte[] data = Optional.ofNullable(block).map(KhaosDatabase.KhaosBlock::getBlk)
                        .map(BlockCapsule::getData).orElse(new byte[0]);
                memoryUse += data.length;
            }
        }
        return memoryUse;
    }
}

