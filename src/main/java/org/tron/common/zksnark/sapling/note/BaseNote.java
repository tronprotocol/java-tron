package org.tron.common.zksnark.sapling.note;

import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.diversifier_t;

public class BaseNote {

  uint64_t value_ = 0;

  public class SaplingNote extends BaseNote {

    diversifier_t d;
    uint256 pk_d;
    uint256 r;

// Call librustzcash to compute the commitment
    boost::

    optional<uint256> cm() const

    {
      uint256 result;
      if (!librustzcash_sapling_compute_cm(
          d.data(),
          pk_d.begin(),
          value(),
          r.begin(),
          result.begin()
      )) {
        return boost::none;
      }

      return result;
    }

// Call librustzcash to compute the nullifier
    boost::

    optional<uint256> nullifier(const FullViewingKey&vk, const uint64_t position) const

    {
      auto ak = vk.ak;
      auto nk = vk.nk;

      uint256 result;
      if (!librustzcash_sapling_compute_nf(
          d.data(),
          pk_d.begin(),
          value(),
          r.begin(),
          ak.begin(),
          nk.begin(),
          position,
          result.begin()
      )) {
        return boost::none;
      }

      return result;
    }
  }
}
