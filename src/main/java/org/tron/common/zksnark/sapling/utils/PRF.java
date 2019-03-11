package org.tron.common.zksnark.sapling.utils;

public class PRF {


  public static uint256 PRF_ask(const uint256&sk) {
    uint256 ask;
    auto tmp = PRF_expand(sk, 0);
    librustzcash_to_scalar(tmp.data(), ask.begin());
    return ask;
  }

  public static uint256 PRF_nsk(const uint256&sk) {
    uint256 nsk;
    auto tmp = PRF_expand(sk, 1);
    librustzcash_to_scalar(tmp.data(), nsk.begin());
    return nsk;
  }

  public static uint256 PRF_ovk(const uint256&sk) {
    uint256 ovk;
    auto tmp = PRF_expand(sk, 2);
    memcpy(ovk.begin(), tmp.data(), 32);
    return ovk;
  }

  Array<char,64>

  PRF_expand(const uint256&sk, char t) {
    Array<char, 64 > res;
    char blob[ 33];

    memcpy( & blob[0], sk.begin(), 32);
    blob[32] = t;

    crypto_generichash_blake2b_state state;
    crypto_generichash_blake2b_init_salt_personal(
        & state, nullptr, 0, 64, nullptr, ZCASH_EXPANDSEED_PERSONALIZATION);
    crypto_generichash_blake2b_update( & state, blob, 33);
    crypto_generichash_blake2b_final( & state, res.data(), 64);

    return res;
  }
}
