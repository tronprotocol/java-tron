package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.topicToByteArray;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.protos.Protocol.TransactionInfo.Log;

public class LogFilter {

  @Getter
  @Setter
  private byte[][] contractAddresses = new byte[0][]; //[addr1, addr2]
  //first topic must be func1 or func2，ignore second，third must be A or B，forth must be C
  @Getter
  @Setter
  private List<byte[][]> topics = new ArrayList<>();  //  [[func1, func1], null, [A, B], [C]]

  public LogFilter() {
  }

  /**
   * construct one LogFilter from part parameters of FilterRequest
   */
  public LogFilter(FilterRequest fr) throws JsonRpcInvalidParamsException {
    if (fr.address instanceof String) {
      try {
        withContractAddress(addressToByteArray((String) fr.address));
      } catch (JsonRpcInvalidParamsException e) {
        throw new JsonRpcInvalidParamsException("invalid address: " + e.getMessage());
      }
    } else if (fr.address instanceof ArrayList) {
      List<byte[]> addr = new ArrayList<>();
      int i = 0;
      for (Object s : (ArrayList) fr.address) {
        try {
          addr.add(addressToByteArray((String) s));
          i++;
        } catch (JsonRpcInvalidParamsException e) {
          throw new JsonRpcInvalidParamsException(
              String.format("invalid address at index %d: %s", i, e.getMessage()));
        }
      }
      withContractAddress(addr.toArray(new byte[addr.size()][]));

    } else if (fr.address != null) {
      throw new JsonRpcInvalidParamsException("invalid addresses in query");
    }

    if (fr.topics != null) {
      //restrict depth of topics, because event has a signature and most 3 indexed parameters
      if (fr.topics.length > 4) {
        throw new JsonRpcInvalidParamsException("topics size should be <= 4");
      }
      for (Object topic : fr.topics) {
        if (topic == null) {
          withTopic((byte[][]) null);
        } else if (topic instanceof String) {
          try {
            withTopic(new DataWord(topicToByteArray((String) topic)).getData());
          } catch (JsonRpcInvalidParamsException e) {
            throw new JsonRpcInvalidParamsException("invalid topic(s): " + e.getMessage());
          }
        } else if (topic instanceof ArrayList) {

          List<byte[]> t = new ArrayList<>();
          for (Object s : ((ArrayList) topic)) {
            try {
              t.add(new DataWord(topicToByteArray((String) s)).getData());
            } catch (JsonRpcInvalidParamsException e) {
              throw new JsonRpcInvalidParamsException("invalid topic(s): " + e.getMessage());
            }
          }
          withTopic(t.toArray(new byte[t.size()][]));
        } else {
          throw new JsonRpcInvalidParamsException("invalid topic(s)");
        }
      }
    }

    if (contractAddresses.length == 0 && topics.size() == 0) {
      throw new JsonRpcInvalidParamsException("must specify address or topics.");
    }
  }

  /**
   * add contractAddress
   */
  public LogFilter withContractAddress(byte[]... orAddress) {
    contractAddresses = orAddress;
    return this;
  }

  /**
   * add one or more topic
   */
  public LogFilter withTopic(byte[]... orTopic) {
    topics.add(orTopic);
    return this;
  }

  public boolean matchesContractAddress(byte[] toAddr) {
    for (byte[] address : contractAddresses) {
      if (Arrays.equals(address, toAddr)) {
        return true;
      }
    }
    return contractAddresses.length == 0;
  }

  /**
   * match any event
   */
  public boolean matchesExactly(Log logInfo) {

    if (!matchesContractAddress(logInfo.getAddress().toByteArray())) {
      return false;
    }
    List<ByteString> logTopics = logInfo.getTopicsList();
    for (int i = 0; i < this.topics.size(); i++) {
      if (i >= logTopics.size()) {
        return false;
      }
      byte[][] orTopics = topics.get(i);
      if (orTopics != null && orTopics.length > 0) {
        boolean orMatches = false;
        byte[] logTopic = logTopics.get(i).toByteArray();
        for (byte[] orTopic : orTopics) {
          if (new DataWord(orTopic).equals(new DataWord(logTopic))) {
            orMatches = true;
            break;
          }
        }
        if (!orMatches) {
          return false;
        }
      }
    }
    return true;
  }
}
