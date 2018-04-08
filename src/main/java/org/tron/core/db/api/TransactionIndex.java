package org.tron.core.db.api;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.persistence.Persistence;
import java.util.Objects;
import java.util.stream.Collectors;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.Transaction;

public class TransactionIndex extends AbstractIndex<Transaction> {

  public static final SimpleAttribute<Transaction, String> Transaction_ID =
      attribute("transaction id",
          t -> Sha256Hash.of(t.getRawData().toByteArray()).toString());
  private static final Attribute<Transaction, String> OWNERS =
      attribute(String.class, "owner address",
          t -> t.getRawData().getContractList().stream()
              .map(TransactionCapsule::getOwner)
              .filter(Objects::nonNull)
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));
  private static final Attribute<Transaction, String> TOS =
      attribute(String.class, "to address",
          t -> t.getRawData().getContractList().stream()
              .map(TransactionCapsule::getToAddress)
              .filter(Objects::nonNull)
              .map(ByteArray::toHexString)
              .collect(Collectors.toList()));

  public TransactionIndex(Persistence<Transaction, ? extends Comparable> persistence) {
    super(persistence);
  }
}
