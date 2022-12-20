package org.tron.plugins;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DBComparator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.DbConstants;
import org.iq80.leveldb.impl.FileMetaData;
import org.iq80.leveldb.impl.Filename;
import org.iq80.leveldb.impl.InternalKeyComparator;
import org.iq80.leveldb.impl.InternalUserComparator;
import org.iq80.leveldb.impl.TableCache;
import org.iq80.leveldb.impl.VersionSet;
import org.iq80.leveldb.table.BytewiseComparator;
import org.iq80.leveldb.table.CustomUserComparator;
import org.iq80.leveldb.table.UserComparator;
import org.tron.plugins.comparator.MarketOrderPriceComparatorForLevelDB;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;
import picocli.CommandLine;


@Slf4j(topic = "debug")
@CommandLine.Command(name = "dbg", aliases = "debug",
    description = "Show leveldb manifest and sst content.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class DbDebug implements Callable<Integer> {


  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @CommandLine.Parameters(index = "0",
      description = "full path for leveldb data.")
  private File databaseDir;

  @CommandLine.Option(names = {"--level"}, converter = LevelConverter.class,
      description = "LSM-Tree level, [1,7]")
  private int level;

  @CommandLine.Option(names = { "--sst"},
      description = "sst file number")
  private long sst = -1;

  @CommandLine.Option(names = {"-h", "--help"})
  private boolean help;


  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }

    if (!databaseDir.exists()) {
      logger.info(" {} does not exist.", databaseDir);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s does not exist.", databaseDir)));
      return 404;
    }

    if (!databaseDir.isDirectory()) {
      logger.info(" {} is not a directory.", databaseDir);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s is not a directory.", databaseDir)));
      return 403;
    }

    if (!Paths.get(databaseDir.toString(), Filename.currentFileName()).toFile().exists()
        || FileUtils.isRocksDBEngine(databaseDir.toPath())) {
      logger.info(" {} is not a leveldb path.", databaseDir);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s is not a leveldb path.", databaseDir)));
      return 402;
    }

    if (sst != -1 && !new File(databaseDir.toString(), sst + ".sst").exists()) {
      logger.info(" {}.sst does not exist.", sst);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%d.sst does not exist.", sst)));
      return 404;
    }


    Options options = new Options();
    if (DBUtils.MARKET_PAIR_PRICE_TO_ORDER.equals(databaseDir.getName())) {
      options.comparator(new MarketOrderPriceComparatorForLevelDB());
    }
    int tableCacheSize = options.maxOpenFiles() - 10;
    InternalKeyComparator internalKeyComparator;
    //use custom comparator if set
    DBComparator comparator = options.comparator();
    UserComparator userComparator;
    if (comparator != null) {
      userComparator = new CustomUserComparator(comparator);
    } else {
      userComparator = new BytewiseComparator();
    }
    internalKeyComparator = new InternalKeyComparator(userComparator);
    TableCache tableCache = new TableCache(databaseDir, tableCacheSize,
        new InternalUserComparator(internalKeyComparator), options.verifyChecksums());
    VersionSet versions = new VersionSet(databaseDir, tableCache, options, internalKeyComparator);

    // load  (and recover) current version
    versions.recover();
    if (sst != -1) {
      spec.commandLine().getOut().format("print %d.sst content in hex string", sst).println();
      logger.info("print {}.sst content in hex string", sst);
      tableCache.newIterator(sst).forEachRemaining(e -> {
        String k = ByteArray.toHexString(e.getKey().getUserKey().getBytes());
        String v = ByteArray.toHexString(e.getValue().getBytes());
        spec.commandLine().getOut().format("k: %s, v: %s", k, v).println();
        logger.info("k: {}, v: {}", k, v);
      });
      spec.commandLine().getOut().format("print %d.sst content done", sst).println();
      logger.info("print {}.sst content done", sst);
    } else {
      if (level > 0) {
        versions.getCurrent().getFiles().get(level).forEach(meta -> this.print(level, meta));
      } else {
        versions.getCurrent().getFiles().forEach(this::print);
      }
    }
    return 0;
  }

  private void print(long level, FileMetaData meta) {
    String small = ByteArray.toHexString(meta.getSmallest().getUserKey().getBytes());
    String max = ByteArray.toHexString(meta.getLargest().getUserKey().getBytes());
    long size = meta.getFileSize();
    long num = meta.getNumber();
    long seek = meta.getAllowedSeeks();
    spec.commandLine().getOut().format(
        "level: %d, num: %d, seek: %d, size: %d, small: %s, max: %s",
        level, num, seek, size, small, max).println();
    logger.info("level: {}, num: {}, seek: {}, size: {}, small: {}, max: {}",
        level, num, seek, size, small, max);
  }

  static class LevelConverter implements CommandLine.ITypeConverter<Integer> {
    public Integer convert(String value) {
      try {
        int level = Integer.parseInt(value);
        if (level > 0 && level <= DbConstants.NUM_LEVELS) {
          return level;
        }
        throw new CommandLine.TypeConversionException("level '" + value
            + "' range out of [1," + DbConstants.NUM_LEVELS + "] ");
      } catch (CommandLine.TypeConversionException e) {
        throw e;
      } catch (Exception ex) {
        throw new CommandLine.TypeConversionException("'" + value + "' is not a int");
      }
    }
  }

}
