package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BytesCapsule;

@Slf4j
@Component
public class WitnessScheduleStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final byte[] ACTIVE_WITNESSES = "active_witnesses".getBytes();
  private static final byte[] CURRENT_SHUFFLED_WITNESSES = "current_shuffled_witnesses".getBytes();

  private static final int ADDRESS_BYTE_ARRAY_LENGTH = 21;

  @Autowired
  private WitnessScheduleStore(@Value("witness_schedule") String dbName) {
    super(dbName);
  }

  private void saveData(byte[] species, List<ByteString> witnessesAddressList) {
    byte[] ba = new byte[witnessesAddressList.size() * ADDRESS_BYTE_ARRAY_LENGTH];
    int i = 0;
    for (ByteString address : witnessesAddressList) {
      System.arraycopy(address.toByteArray(), 0,
          ba, i * ADDRESS_BYTE_ARRAY_LENGTH, ADDRESS_BYTE_ARRAY_LENGTH);
      i++;
    }

    this.put(species, new BytesCapsule(ba));
  }

  private List<ByteString> getData(byte[] species) {
    List<ByteString> witnessesAddressList = new ArrayList<>();
    return Optional.ofNullable(getUnchecked(species))
        .map(BytesCapsule::getData)
        .map(ba -> {
          int len = ba.length / ADDRESS_BYTE_ARRAY_LENGTH;
          for (int i = 0; i < len; ++i) {
            byte[] b = new byte[ADDRESS_BYTE_ARRAY_LENGTH];
            System.arraycopy(ba, i * ADDRESS_BYTE_ARRAY_LENGTH, b, 0, ADDRESS_BYTE_ARRAY_LENGTH);
            witnessesAddressList.add(ByteString.copyFrom(b));
          }
          logger.debug("getWitnesses:" + ByteArray.toStr(species) + witnessesAddressList);
          return witnessesAddressList;
        }).orElseThrow(
            () -> new IllegalArgumentException(
                "not found " + ByteArray.toStr(species) + "Witnesses"));
  }

  public void saveActiveWitnesses(List<ByteString> witnessesAddressList) {
    saveData(ACTIVE_WITNESSES, witnessesAddressList);
  }

  public List<ByteString> getActiveWitnesses() {
    return getData(ACTIVE_WITNESSES);
  }

  public void saveCurrentShuffledWitnesses(List<ByteString> witnessesAddressList) {
    saveData(CURRENT_SHUFFLED_WITNESSES, witnessesAddressList);
  }

  public List<ByteString> getCurrentShuffledWitnesses() {
    return getData(CURRENT_SHUFFLED_WITNESSES);
  }
}
