package org.tron.core.store;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.entity.Dec;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.OraclePrevoteCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.Protocol.OracleVote;

@Slf4j(topic = "DB")
@Component
public class OracleStore extends TronStoreWithRevoking<BytesCapsule> {
  @Autowired
  public OracleStore(@Value("oracle") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
  }

  public void setFeeder(byte[] srAddress, byte[] feederAddress) {
    byte[] key = buildFeederKey(srAddress);

    // if feeder == sr or fedder is empty, delete sr feeder from db
    if (Arrays.equals(srAddress, feederAddress) || ArrayUtils.isEmpty(feederAddress)) {
      delete(key);
      return;
    }

    put(key, new BytesCapsule(feederAddress));
  }

  public byte[] getFeeder(byte[] srAddress) {
    BytesCapsule feeder = get(buildFeederKey(srAddress));
    if (feeder != null) {
      return feeder.getData();
    }
    return null;
  }

  private byte[] buildFeederKey(byte[] address) {
    return ("feeder-" + Hex.toHexString(address)).getBytes();
  }

  public void setPrevote(byte[] srAddress, OraclePrevoteCapsule oraclePrevoteCapsule) {
    byte[] key = buildPrevoteKey(srAddress);
    put(key, new BytesCapsule(oraclePrevoteCapsule.getData()));
  }

  public OraclePrevoteCapsule getPrevote(byte[] srAddress) {
    BytesCapsule prevote = get(buildPrevoteKey(srAddress));
    if (prevote != null) {
      return new OraclePrevoteCapsule(prevote.getData());
    }
    return null;
  }

  private byte[] buildPrevoteKey(byte[] address) {
    return ("prevote-" + Hex.toHexString(address)).getBytes();
  }

  public void setVote(byte[] srAddress, OracleVote vote) {
    byte[] key = buildVoteKey(srAddress);
    put(key, new BytesCapsule(vote.toByteArray()));
  }

  public OracleVote getVote(byte[] srAddress) {
    BytesCapsule vote = get(buildVoteKey(srAddress));
    if (vote == null) {
      return null;
    }

    OracleVote oracleVote;
    try {
      oracleVote = OracleVote.parseFrom(vote.getData());
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      return null;
    }
    return oracleVote;
  }

  public void clearPrevoteAndVotes(long blockNum, long votePeriod) {
    // TODO
  }

  private byte[] buildVoteKey(byte[] address) {
    return ("vote-" + Hex.toHexString(address)).getBytes();
  }

  public void clearAllExchangeRates() {
    // TODO
  }

  public void setTrxExchangeRate(String asset, Dec exchangeRate) {
    byte[] key = buildExchangeRateKey(asset);
    put(key, new BytesCapsule(exchangeRate.toByteArray()));
  }

  public Dec getTrxExchangeRate(String asset) {
    BytesCapsule exchangeRate = get(buildExchangeRateKey(asset));
    if (exchangeRate == null) {
      return null;
    }
    return Dec.newDec(exchangeRate.getData());
  }

  private byte[] buildExchangeRateKey(String asset) {
    return ("ex-rate-" + asset).getBytes();
  }

  public void updateTobinTax(Map<String, Dec> proposalTobinList, Map<String, Dec> oracleTobinList) {
    // check is there any update in proposal
    boolean needUpdate = false;
    if (proposalTobinList.size() != oracleTobinList.size()) {
      needUpdate = true;
    } else {
      for (Map.Entry<String, Dec> proposalTobinTax : proposalTobinList.entrySet()) {
        Dec oracleTobin = oracleTobinList.get(proposalTobinTax.getKey());
        if (oracleTobin == null || !oracleTobin.eq(proposalTobinTax.getValue())) {
          needUpdate = true;
          break;
        }
      }
    }

    if (needUpdate) {
      clearAllTobinTax();

      for (Map.Entry<String, Dec> proposalTobinTax : proposalTobinList.entrySet()) {
        byte[] key = buildTobinTaxKey(proposalTobinTax.getKey());
        put(key, new BytesCapsule(proposalTobinTax.getValue().toByteArray()));
      }
    }
  }

  public Dec getTobinTax(String asset) {
    BytesCapsule tobinTax = get(buildTobinTaxKey(asset));
    if (tobinTax == null) {
      return null;
    }
    return Dec.newDec(tobinTax.getData());
  }

  public Map<String, Dec> getAllTobinTax() {
    // TODO
    return null;
  }

  public void clearAllTobinTax() {
    // TODO
  }

  private byte[] buildTobinTaxKey(String asset) {
    return ("tobin-" + asset).getBytes();
  }

  public static Map<String, Dec> parseExchangeRateTuples(String exchangeRatesStr) {
    exchangeRatesStr = exchangeRatesStr.trim();
    if (exchangeRatesStr.isEmpty()) {
      throw new RuntimeException("exchange rate string cannot be empty");
    }

    Map<String, Dec> ex = new HashMap<>();
    String[] exchangeRateList = exchangeRatesStr.split(",");
    for (String e : exchangeRateList) {
      String[] exchangeRatePair = e.split(":");
      if (exchangeRatePair.length != 2) {
        throw new RuntimeException("exchange rate pair length error");
      }
      String asset = exchangeRatePair[0];
      String exchangeRate = exchangeRatePair[1];
      ex.put(asset, Dec.newDec(exchangeRate));
    }
    return ex;
  }
}
