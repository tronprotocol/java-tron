package org.tron.common.zksnark.sapling.address;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.utils.PRF;


@AllArgsConstructor
public class SpendingKey {
  public byte[] value;
  // class SpendingKey : public uint256 {

  static SpendingKey random() {
    while (true) {
      SpendingKey sk = new SpendingKey(randomUint256());
      if (sk.full_viewing_key().is_valid()) {
        return sk;
      }
    }
  }

  ExpandedSpendingKey expanded_spending_key() {
    return new ExpandedSpendingKey(PRF.prfAsk( this),PRF.prfNsk(  this),PRF.prfOvk(this));
  }

  FullViewingKey full_viewing_key() {

    return expanded_spending_key().full_viewing_key();
  }

  // Can derive  addr from default diversifier
  PaymentAddress default_address() {
    // Iterates within defaultDiversifier to ensure a valid address is returned
    Optional<PaymentAddress> addrOpt = full_viewing_key().in_viewing_key().address(
        defaultDiversifier(  this));
//    assert (addrOpt != boost::none);
    return addrOpt.get();
  }

  //todo
  private DiversifierT defaultDiversifier(SpendingKey spendingKey){
    return null;
  }
  //todo
  static private byte[] randomUint256(){
    return null;
  }


}
