package org.tron.common.logsfilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;
import org.pf4j.util.StringUtils;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;

@Slf4j(topic = "Parser")
public class ContractEventParserAbi extends ContractEventParser {

  public static final int MAX_ABI_ENTRIES = 1024;
  public static final int MAX_ABI_EVENTS = 512;
  public static final int MAX_ABI_ENTRY_PARAMS = 64;
  public static final int MAX_ABI_IDENTIFIER_LENGTH = 256;

  /**
   * parse Event Topic into map NOTICE: In solidity, Indexed Dynamic types's topic is just
   * EVENT_INDEXED_ARGS
   */
  public static Map<String, String> parseTopics(List<byte[]> topicList, ABI.Entry entry) {
    Map<String, String> map = new HashMap<>();
    if (topicList == null || topicList.isEmpty()) {
      return map;
    }

    // the first is the signature.
    int index = 1;
    List<ABI.Entry.Param> list = entry.getInputsList();

    // in case indexed topics doesn't match
    if (topicsMatched(topicList, entry)) {
      for (int i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (param.getIndexed()) {
          if (index >= topicList.size()) {
            break;
          }
          String str = parseTopic(topicList.get(index++), param.getType());
          if (StringUtils.isNotNullOrEmpty(param.getName())) {
            map.put(param.getName(), str);
          }
          map.put("" + i, str);
        }
      }
    } else {
      for (int i = 1; i < topicList.size(); ++i) {
        map.put("" + (i - 1), Hex.toHexString(topicList.get(i)));
      }
    }
    return map;
  }

  /**
   * parse Event Data into map, If parser failed, then return {"0", Hex.toHexString(data)} Only
   * support basic solidity type, String, Bytes. Fixed Array or dynamic Array are not support yet
   * (then return {"0": Hex.toHexString(data)}).
   */
  public static Map<String, String> parseEventData(byte[] data,
      List<byte[]> topicList, ABI.Entry entry) {
    Map<String, String> map = new HashMap<>();
    if (ArrayUtils.isEmpty(data)) {
      return map;
    }
    // in case indexed topics do not match
    if (!topicsMatched(topicList, entry)) {
      map.put("" + (topicList.size() - 1), Hex.toHexString(data));
      return map;
    }

    // the first is the signature.
    List<ABI.Entry.Param> list = entry.getInputsList();
    Integer startIndex = 0;
    try {
      // this one starts from the first position.
      int index = 0;
      for (Integer i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (param.getIndexed()) {
          continue;
        }
        if (startIndex == 0) {
          startIndex = i;
        }

        String str = parseDataBytes(data, param.getType(), index++);
        if (StringUtils.isNotNullOrEmpty(param.getName())) {
          map.put(param.getName(), str);
        }
        map.put("" + i, str);

      }
      if (list.isEmpty()) {
        map.put("0", Hex.toHexString(data));
      }
    } catch (UnsupportedOperationException e) {
      logger.debug("UnsupportedOperationException", e);
      map.clear();
      map.put(startIndex.toString(), Hex.toHexString(data));
    }
    return map;
  }

  private static boolean topicsMatched(List<byte[]> topicList, ABI.Entry entry) {
    if (topicList == null || topicList.isEmpty()) {
      return true;
    }
    int inputSize = 1;
    for (ABI.Entry.Param param : entry.getInputsList()) {
      if (param.getIndexed()) {
        inputSize++;
      }
    }
    return inputSize == topicList.size();
  }

  /** True if the ABI declares more entries than {@link #MAX_ABI_ENTRIES}. */
  public static boolean hasTooManyEntries(ABI abi) {
    return abi.getEntrysCount() > MAX_ABI_ENTRIES;
  }

  /** True if the ABI declares more non-anonymous events than {@link #MAX_ABI_EVENTS}. */
  public static boolean hasTooManyEvents(ABI abi) {
    int events = 0;
    for (ABI.Entry entry : abi.getEntrysList()) {
      if (entry.getType() == ABI.Entry.EntryType.Event && !entry.getAnonymous()
          && ++events > MAX_ABI_EVENTS) {
        return true;
      }
    }
    return false;
  }

  /**
   * True if an entry exceeds the per-entry limits on parameter count
   * ({@link #MAX_ABI_ENTRY_PARAMS}) or identifier length ({@link #MAX_ABI_IDENTIFIER_LENGTH}).
   */
  public static boolean isEntryTooLarge(ABI.Entry entry) {
    if (entry.getInputsCount() > MAX_ABI_ENTRY_PARAMS) {
      return true;
    }
    if (isIdentifierTooLong(entry.getName())) {
      return true;
    }
    for (ABI.Entry.Param param : entry.getInputsList()) {
      if (isIdentifierTooLong(param.getType()) || isIdentifierTooLong(param.getName())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isIdentifierTooLong(String identifier) {
    return identifier != null && identifier.length() > MAX_ABI_IDENTIFIER_LENGTH;
  }
}
