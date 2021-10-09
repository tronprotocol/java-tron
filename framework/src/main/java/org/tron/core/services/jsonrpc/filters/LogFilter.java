package org.tron.core.services.jsonrpc.filters;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressToByteArrayWithPrefix;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.topicToByteArray;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.Bloom;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.services.jsonrpc.JsonRpcApiUtil;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.protos.Protocol.TransactionInfo.Log;

@Slf4j(topic = "API")
public class LogFilter {

  //[addr1, addr2]
  @Getter
  @Setter
  private byte[][] contractAddresses = new byte[0][];
  // example: [[func1, func1], null, [A, B], [C]]
  // first topic must be func1 or func2，second can be any，third must be A or B，forth must be C
  @Getter
  @Setter
  private List<byte[][]> topics = new ArrayList<>();
  // [[func1, func1], null, [A, B], [C]] + [addr1, addr2] => Bloom[][]
  @Setter
  private Bloom[][] filterBlooms;


  public LogFilter() {
  }

  /**
   * construct one LogFilter from part parameters of FilterRequest
   */
  public LogFilter(FilterRequest fr) throws JsonRpcInvalidParamsException {
    if (fr.address instanceof String) {
      try {
        withContractAddress(addressToByteArrayWithPrefix((String) fr.address));
      } catch (JsonRpcInvalidParamsException e) {
        throw new JsonRpcInvalidParamsException("invalid address: " + e.getMessage());
      }
    } else if (fr.address instanceof ArrayList) {
      List<byte[]> addr = new ArrayList<>();
      int i = 0;
      for (Object s : (ArrayList) fr.address) {
        try {
          addr.add(addressToByteArrayWithPrefix((String) s));
          i++;
        } catch (JsonRpcInvalidParamsException e) {
          throw new JsonRpcInvalidParamsException(
              String.format("invalid address at index %d: [%s]", i, e.getMessage()));
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

//    if (contractAddresses.length == 0 && topics.isEmpty()) {
//      throw new JsonRpcInvalidParamsException("must specify address or topics.");
//    }
  }

  /**
   * add contractAddress
   */
  private LogFilter withContractAddress(byte[]... orAddress) {
    contractAddresses = orAddress;
    return this;
  }

  /**
   * add one or more topic
   */
  private LogFilter withTopic(byte[]... orTopic) {
    topics.add(orTopic);
    return this;
  }

  /**
   * generate filterBlooms from  contractAddresses、topics
   */
  private void initBlooms() {
    if (filterBlooms != null) {
      return;
    }

    //topics ahead，address last
    List<byte[][]> addrAndTopics = new ArrayList<>(topics);
    addrAndTopics.add(contractAddresses);

    filterBlooms = new Bloom[addrAndTopics.size()][];
    for (int i = 0; i < addrAndTopics.size(); i++) {
      byte[][] orTopics = addrAndTopics.get(i);
      if (orTopics == null || orTopics.length == 0) {
        filterBlooms[i] = new Bloom[] {new Bloom()}; // always matches
      } else {
        filterBlooms[i] = new Bloom[orTopics.length];
        for (int j = 0; j < orTopics.length; j++) {
          filterBlooms[i][j] = Bloom.create(Hash.sha3(orTopics[j]));
        }
      }
    }
  }

  /**
   * match this logFilter with a block bloom sketchy first. if matched, the match exactly
   */
  public boolean matchBloom(Bloom blockBloom) {
    initBlooms();
    for (Bloom[] andBloom : filterBlooms) {
      boolean orMatches = false;
      for (Bloom orBloom : andBloom) {
        if (blockBloom.matches(orBloom)) {
          orMatches = true;
          break;
        }
      }
      if (!orMatches) {
        return false;
      }
    }
    return true;
  }

  private boolean matchesContractAddress(byte[] toAddr) {
    byte[] toAddrWithPrefix = JsonRpcApiUtil.convertToTronAddress(toAddr);
    for (byte[] address : contractAddresses) {
      if (Arrays.equals(address, toAddrWithPrefix)) {
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
