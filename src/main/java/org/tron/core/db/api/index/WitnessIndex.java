package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.protos.Protocol.Witness;

@Component
@Slf4j
public class WitnessIndex extends AbstractIndex<WitnessCapsule, Witness> {

  public final Attribute<WrappedByteArray, String> Witness_ADDRESS =
      attribute("witness address",
          bytes -> ByteArray.toHexString(getObject(bytes).getAddress().toByteArray()));
  public final Attribute<WrappedByteArray, String> PUBLIC_KEY =
      attribute("public key",
          bytes -> ByteArray.toHexString(getObject(bytes).getPubKey().toByteArray()));
  public final Attribute<WrappedByteArray, String> Witness_URL =
      attribute("witness url", bytes -> getObject(bytes).getUrl());

  @Autowired
  public WitnessIndex(
      @Qualifier("witnessStore") final TronDatabase<WitnessCapsule> database) {
    super();
    this.database = database;
  }

  public WitnessIndex(
      final TronDatabase<WitnessCapsule> database,
      Persistence<WrappedByteArray, ? extends Comparable> persistence) {
    super(persistence);
    this.database = database;
  }

  @PostConstruct
  public void init() {
    index.addIndex(SuffixTreeIndex.onAttribute(Witness_ADDRESS));
    index.addIndex(SuffixTreeIndex.onAttribute(PUBLIC_KEY));
    index.addIndex(SuffixTreeIndex.onAttribute(Witness_URL));
    fill();
  }

}
