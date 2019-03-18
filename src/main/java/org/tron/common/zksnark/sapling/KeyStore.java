package org.tron.common.zksnark.sapling;

import java.util.Map;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.sapling.zip32.HDSeed;

public class KeyStore {

  static HDSeed seed;

  static Map<FullViewingKey, ExtendedSpendingKey> mapSaplingSpendingKeys; // k:fvk,v:sk
  static Map<IncomingViewingKey, FullViewingKey> mapSaplingFullViewingKeys; // k:ivk,v:fvk
  static Map<PaymentAddress, IncomingViewingKey> mapSaplingIncomingViewingKeys; // k:addr,v:ivk

  public static boolean HaveSaplingSpendingKey(FullViewingKey fvk) {
    boolean result;
    {
      result = (mapSaplingSpendingKeys.get(fvk) != null);
    }
    return result;
  }

  public static boolean AddSaplingSpendingKey(ExtendedSpendingKey sk, PaymentAddress defaultAddr) {
    FullViewingKey fvk = sk.getExpsk().full_viewing_key();

    // if SaplingFullViewingKey is not in SaplingFullViewingKeyMap, add it
    if (!AddSaplingFullViewingKey(fvk, defaultAddr)) {
      return false;
    }

    mapSaplingSpendingKeys.put(fvk, sk);

    return true;
  }

  public static boolean AddSaplingFullViewingKey(FullViewingKey fvk, PaymentAddress defaultAddr) {
    IncomingViewingKey ivk = fvk.in_viewing_key();
    mapSaplingFullViewingKeys.put(ivk, fvk);

    return AddSaplingIncomingViewingKey(ivk, defaultAddr);
  }

  // This function updates the wallet's internal address->ivk map.
  // If we add an address that is already in the map, the map will
  // remain unchanged as each address only has one ivk.
  public static boolean AddSaplingIncomingViewingKey(IncomingViewingKey ivk, PaymentAddress addr) {

    // Add addr -> SaplingIncomingViewing to SaplingIncomingViewingKeyMap
    mapSaplingIncomingViewingKeys.put(addr, ivk);

    return true;
  }

  public static boolean HaveSaplingFullViewingKey(IncomingViewingKey ivk) {
    return mapSaplingFullViewingKeys.get(ivk) != null;
  }

  public static boolean HaveSaplingIncomingViewingKey(PaymentAddress addr) {
    return mapSaplingIncomingViewingKeys.get(addr) != null;
  }

  public static boolean GetSaplingFullViewingKey(IncomingViewingKey ivk, FullViewingKey fvkOut) {

    fvkOut = mapSaplingFullViewingKeys.get(ivk);
    if (fvkOut == null) {
      return false;
    }
    return true;
  }

  public static boolean GetSaplingIncomingViewingKey(
      PaymentAddress addr, IncomingViewingKey ivkOut) {

    ivkOut = mapSaplingIncomingViewingKeys.get(addr);

    if (ivkOut == null) {
      return false;
    }
    return true;
  }

  public static boolean GetSaplingSpendingKey(FullViewingKey fvk, ExtendedSpendingKey skOut) {
    skOut = mapSaplingSpendingKeys.get(fvk);
    if (skOut == null) {
      return false;
    }
    return false;
  }

  public static boolean GetSaplingExtendedSpendingKey(
      PaymentAddress addr, ExtendedSpendingKey extskOut) {

    IncomingViewingKey ivk = null;
    FullViewingKey fvk = null;

    return GetSaplingIncomingViewingKey(addr, ivk)
        & GetSaplingFullViewingKey(ivk, fvk)
        & GetSaplingSpendingKey(fvk, extskOut);
  }
}
