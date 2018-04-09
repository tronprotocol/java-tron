package org.tron.core.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.Persistence;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.protos.Protocol.Witness;

@Component
@Slf4j
public class WitnessIndex extends AbstractIndex<Witness> {

  private static final Attribute<Witness, String> Witness_ADDRESS =
      attribute("witness address", witness -> witness.getAddress().toStringUtf8());
  private static final Attribute<Witness, String> PUBLIC_KEY =
      attribute("public key", witness -> witness.getPubKey().toStringUtf8());
  private static final Attribute<Witness, Boolean> IS_JOBS =
      attribute("is jobs", Witness::getIsJobs);
  private static final Attribute<Witness, String> Witness_URL =
      attribute("witness url", Witness::getUrl);

  public WitnessIndex() {
    super();
  }

  public WitnessIndex(Persistence<Witness, ? extends Comparable> persistence) {
    super(persistence);
  }

  @PostConstruct
  public void init() {
    addIndex(SuffixTreeIndex.onAttribute(Witness_ADDRESS));
    addIndex(SuffixTreeIndex.onAttribute(PUBLIC_KEY));
    addIndex(NavigableIndex.onAttribute(IS_JOBS));
    addIndex(SuffixTreeIndex.onAttribute(Witness_URL));
  }

}
