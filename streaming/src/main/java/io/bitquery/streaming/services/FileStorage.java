package io.bitquery.streaming.services;

import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.messages.ProtobufMessage;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
//import org.tron.core.config.args.StreamingConfig;
//import io.bitquery.streaming.messages.ProtobufMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "streaming")
public class FileStorage {

    private ProtobufMessage protobufMessage;

    private TracerConfig config;

    private final String directoriesMonitor = "directories-monitor";
    private final ScheduledExecutorService directoriesMonitorExecutor = ExecutorServiceManager
            .newSingleThreadScheduledExecutor(directoriesMonitor);

    public FileStorage(ProtobufMessage protobufMessage, TracerConfig config) {
        this.protobufMessage = protobufMessage;
        this.config = config;

        startDirectoriesMonitoring();
    }

    public void store() {
        String fullPath = getBlockPath();
        writeOnceMessageToFile(fullPath);

        String uriPath = setUri(fullPath);
        logger.info("Stored message, Path: {}, Length: {}", uriPath, protobufMessage.getMeta().getSize());
    }

    public void close() {
        ExecutorServiceManager.shutdownAndAwaitTermination(directoriesMonitorExecutor, directoriesMonitor);
    }

    private String getBlockPath() {
        String directoryPath = getDirectoryName();
        String fileName = String.format("%s_%s_%s%s",
                getPaddedBlockNumber(protobufMessage.getMeta().getDescriptor().getBlockNumber()),
                protobufMessage.getMeta().getDescriptor().getBlockHash(),
                ByteArray.toHexString(protobufMessage.getBodyHash()),
                config.getPathGeneratorSuffix()
        );

        String fullPath = Paths.get(directoryPath, fileName).toString();

        return fullPath;
    }

    private String getDirectoryName() {
        long currentBlockNum = protobufMessage.getMeta().getDescriptor().getBlockNumber();
        int bucketSize = config.getPathGeneratorBucketSize();
        String folderPrefix = config.getFileStorageRoot();

        String paddedBlockNum = getPaddedBlockNumber(bucketSize * (currentBlockNum / bucketSize));
        String dirName = Paths.get(folderPrefix, protobufMessage.getTopic(), paddedBlockNum).toString();

        return dirName;
    }

    private String getPaddedBlockNumber(long number) {
        String template = "%%%s%dd";
        String formattedBlockNumber = String.format(
                template,
                config.getPathGeneratorSpacer(),
                config.getPathGeneratorBlockNumberPadding()
        );

        String blockNumber = String.format(formattedBlockNumber, number);

        return blockNumber;
    }

    private void writeOnceMessageToFile(String fullPath) {
        if (FileUtil.isExists(fullPath)) {
            return;
        }

        new File(fullPath).getParentFile().mkdirs();

        try {
            LZ4FrameOutputStream outStream = new LZ4FrameOutputStream(new FileOutputStream(new File(fullPath)));

            outStream.write(protobufMessage.getBody());
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String setUri(String fullPath) {
        // deletes folder prefix;
        String uriPath = fullPath.replaceFirst(config.getFileStorageRoot() + "/", "");
        protobufMessage.getMeta().setUri(uriPath);

        return uriPath;
    }

    private void startDirectoriesMonitoring() {
        int ttlSecs = config.getFileStorageTtlSecs();
        int poolPeriodSec = config.getFileStoragePoolPeriodSec();

        if (ttlSecs < 0) {
            return;
        }

        if (poolPeriodSec == 0) {
            poolPeriodSec = ttlSecs;
        }

        logger.info("Configuring directories monitoring, Period: {}, TTL: {}", poolPeriodSec, ttlSecs);

        directoriesMonitorExecutor.scheduleWithFixedDelay(() -> {
            try {
                long maxDirectoriesTtl = System.currentTimeMillis() - ttlSecs * 1000;
                File[] streamingDirs = new File(config.getFileStorageRoot()).listFiles(File::isDirectory);

                removeDirs(maxDirectoriesTtl, streamingDirs);
            } catch (Exception e) {
                logger.error("Directories monitoring error, error: {}", e.getMessage());
            }
        }, 1, poolPeriodSec, TimeUnit.SECONDS);
    }

    private void removeDirs(long ttl, File[] streamingDirs) {
        for (File dir : streamingDirs) {
            if (dir.lastModified() < ttl) {
                logger.info("Removing directory, Path: {}", dir.getAbsolutePath());
                dir.delete();
            }

            if (dir.isDirectory()) {
                removeDirs(ttl, dir.listFiles(File::isDirectory));
            }
        }
    }
}
