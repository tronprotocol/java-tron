package org.tron.common.logsfilter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.util.encoders.Hex;

@Slf4j(topic = "Parser")
public class ContractEventParserJson extends ContractEventParser {

  private static final String INPUTS = "inputs";
  private static final String INDEXED = "indexed";

  /**
   * parse Event Topic into map NOTICE: In solidity, Indexed Dynamic types's topic is just
   * EVENT_INDEXED_ARGS
   */
  public static Map<String, String> parseTopics(List<byte[]> topicList, JSONObject entry) {
    Map<String, String> map = new HashMap<>();
    if (topicList == null || topicList.isEmpty()) {
      return map;
    }

    // the first is the signature.
    int index = 1;
    JSONArray inputs = entry.getJSONArray(INPUTS);

    // in case indexed topics doesn't match
    if (topicsMatched(topicList, entry)) {
      if (inputs != null) {
        for (int i = 0; i < inputs.size(); ++i) {
          JSONObject param = inputs.getJSONObject(i);
          if (param != null) {
            Boolean indexed = param.getBoolean(INDEXED);
            if (indexed == null || !indexed) {
              continue;
            }
            if (index >= topicList.size()) {
              break;
            }
            String str = parseTopic(topicList.get(index++), param.getString("type"));
            if (StringUtils.isNotNullOrEmpty(param.getString("name"))) {
              map.put(param.getString("name"), str);
            }
            map.put("" + i, str);
          }
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
      List<byte[]> topicList, JSONObject entry) {
    Map<String, String> map = new HashMap<>();
    if (ArrayUtils.isEmpty(data)) {
      return map;
    }
    // in case indexed topics doesn't match
    if (!topicsMatched(topicList, entry)) {
      map.put("" + (topicList.size() - 1), Hex.toHexString(data));
      return map;
    }

    // the first is the signature.
    JSONArray inputs = entry.getJSONArray(INPUTS);
    Integer startIndex = 0;

    try {
      // this one starts from the first position.
      int index = 0;
      if (inputs != null) {
        for (Integer i = 0; i < inputs.size(); ++i) {
          JSONObject param = inputs.getJSONObject(i);
          if (param != null) {
            Boolean indexed = param.getBoolean(INDEXED);
            if (indexed != null && indexed) {
              continue;
            }

            if (startIndex == 0) {
              startIndex = i;
            }

            String str = parseDataBytes(data, param.getString("type"), index++);
            if (StringUtils.isNotNullOrEmpty(param.getString("name"))) {
              map.put(param.getString("name"), str);
            }
            map.put("" + i, str);
          }
        }
      } else {
        map.put("0", Hex.toHexString(data));
      }
    } catch (UnsupportedOperationException e) {
      logger.debug("UnsupportedOperationException", e);
      map.clear();
      map.put(startIndex.toString(), Hex.toHexString(data));
    }
    return map;
  }

  private static boolean topicsMatched(List<byte[]> topicList, JSONObject entry) {
    if (topicList == null || topicList.isEmpty()) {
      return true;
    }
    int inputSize = 1;
    JSONArray inputs = entry.getJSONArray(INPUTS);
    if (inputs != null) {
      for (int i = 0; i < inputs.size(); i++) {
        JSONObject param = inputs.getJSONObject(i);
        if (param != null) {
          Boolean indexed = param.getBoolean(INDEXED);
          if (indexed != null && indexed) {
            inputSize++;
          }
        }
      }
    }
    return inputSize == topicList.size();
  }
}
