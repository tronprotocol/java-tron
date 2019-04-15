package org.tron.common.zksnark.zen.address;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.zen.utils.PRF;

@AllArgsConstructor
public class SpendingKey {


  public byte[] value;
  // class SpendingKey : public uint256 {

  public static SpendingKey random() {
    while (true) {
      SpendingKey sk = new SpendingKey(randomUint256());
      if (sk.fullViewingKey().is_valid()) {
        return sk;
      }
    }
  }

  public ExpandedSpendingKey expandedSpendingKey() {
    return new ExpandedSpendingKey(
        PRF.prfAsk(this.value), PRF.prfNsk(this.value), PRF.prfOvk(this.value));
  }

  public FullViewingKey fullViewingKey() {

    return expandedSpendingKey().fullViewingKey();
  }

  // Can derive  addr from default diversifier
  PaymentAddress default_address() {
    // Iterates within defaultDiversifier to ensure a valid address is returned
    Optional<PaymentAddress> addrOpt =
        fullViewingKey().inViewingKey().address(defaultDiversifier(this));
    //    assert (addrOpt != boost::none);
    return addrOpt.get();
  }

  // todo
  private DiversifierT defaultDiversifier(SpendingKey spendingKey) {
    return null;
  }

  // todo
  private static byte[] randomUint256() {
    return null;
  }
}
