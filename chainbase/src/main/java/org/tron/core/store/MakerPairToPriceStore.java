package org.tron.core.store;

import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.MakerPriceListCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MakerPairToPriceStore extends TronStoreWithRevoking<MakerPriceListCapsule> {

  @Autowired
  protected MakerPairToPriceStore(@Value("maker_pair_to_price") String dbName) {
    super(dbName);
  }

  @Override
  public MakerPriceListCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MakerPriceListCapsule(value);
  }

  public static byte[] calculateDbKey(byte[] sellTokenId, byte[] buyTokenId) {
    byte[] result = new byte[sellTokenId.length + buyTokenId.length];
    System.arraycopy(sellTokenId, 0, result, 0, sellTokenId.length);
    System.arraycopy(buyTokenId, 0, result, sellTokenId.length, buyTokenId.length);
    return result;
  }

}