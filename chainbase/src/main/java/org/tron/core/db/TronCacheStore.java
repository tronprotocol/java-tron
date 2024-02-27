package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db2.common.TronCacheDB;
import org.tron.protos.Protocol;

@Slf4j(topic = "DB")
@Component
public class TronCacheStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final byte[] WITNESS_STANDBY_127 =
      "WITNESS_STANDBY_127".getBytes(StandardCharsets.UTF_8);

  @Autowired
  public TronCacheStore(@Value("tron-cache") String dbName) {
    super(new TronCacheDB(dbName));
  }

  @Override
  public BytesCapsule get(byte[] key) {
    BytesCapsule value  = getUnchecked(key);
    if (value == null || value.getData() == null) {
      return null;
    }
    return value;
  }

  public void putWitnessStandby(List<WitnessCapsule> witnessStandby) {
    GrpcAPI.WitnessList witnessList = GrpcAPI.WitnessList.newBuilder().addAllWitnesses(
        witnessStandby.stream().map(w -> Protocol.Witness.newBuilder()
                .setAddress(w.getAddress())
                .setVoteCount(w.getVoteCount()).build())
            .collect(Collectors.toList())).build();
    this.put(WITNESS_STANDBY_127, new BytesCapsule(witnessList.toByteArray()));
  }

  public List<WitnessCapsule> getWitnessStandby() {
    BytesCapsule value = this.get(WITNESS_STANDBY_127);
    if (value == null) {
      return null;
    }
    try {
      return GrpcAPI.WitnessList.parseFrom(value.getData())
          .getWitnessesList().stream().map(WitnessCapsule::new).collect(Collectors.toList());
    } catch (InvalidProtocolBufferException e) {
      logger.warn(e.getMessage(), e);
    }
    return null;
  }

}
