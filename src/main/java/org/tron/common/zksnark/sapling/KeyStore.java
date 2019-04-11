package org.tron.common.zksnark.sapling;

import java.util.HashMap;
import java.util.Map;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.sapling.zip32.HDSeed;

public class KeyStore {

  static HDSeed seed = new HDSeed();

  static Map<FullViewingKey, ExtendedSpendingKey> mapSpendingKeys = new HashMap<>();
  static Map<IncomingViewingKey, FullViewingKey> mapFullViewingKeys;
  static Map<PaymentAddress, IncomingViewingKey> mapIncomingViewingKeys;

  public static boolean haveSpendingKey(FullViewingKey fvk) {
    boolean result;
    {
      result = (mapSpendingKeys.get(fvk) != null);
    }
    return result;
  }

  public static boolean AddSpendingKey(ExtendedSpendingKey sk, PaymentAddress defaultAddr) {
    FullViewingKey fvk = sk.getExpsk().fullViewingKey();

    // if SaplingFullViewingKey is not in SaplingFullViewingKeyMap, add it
    if (!addFullViewingKey(fvk, defaultAddr)) {
      return false;
    }

    mapSpendingKeys.put(fvk, sk);

    return true;
  }

  public static boolean addFullViewingKey(FullViewingKey fvk, PaymentAddress defaultAddr) {
    IncomingViewingKey ivk = fvk.inViewingKey();
    mapFullViewingKeys.put(ivk, fvk);

    return addIncomingViewingKey(ivk, defaultAddr);
  }

  // This function updates the wallet's internal address->ivk map.
  // If we add an address that is already in the map, the map will
  // remain unchanged as each address only has one ivk.
  public static boolean addIncomingViewingKey(IncomingViewingKey ivk, PaymentAddress addr) {

    mapIncomingViewingKeys.put(addr, ivk);

    return true;
  }

  public static boolean haveFullViewingKey(IncomingViewingKey ivk) {
    return mapFullViewingKeys.get(ivk) != null;
  }

  public static boolean haveIncomingViewingKey(PaymentAddress addr) {
    return mapIncomingViewingKeys.get(addr) != null;
  }

  public static boolean getFullViewingKey(IncomingViewingKey ivk, FullViewingKey fvkOut) {

    fvkOut = mapFullViewingKeys.get(ivk);
    if (fvkOut == null) {
      return false;
    }
    return true;
  }

  public static boolean getIncomingViewingKey(
      PaymentAddress addr, IncomingViewingKey ivkOut) {

    ivkOut = mapIncomingViewingKeys.get(addr);

    if (ivkOut == null) {
      return false;
    }
    return true;
  }

  public static boolean getSpendingKey(FullViewingKey fvk, ExtendedSpendingKey skOut) {
    skOut = mapSpendingKeys.get(fvk);
    if (skOut == null) {
      return false;
    }
    return false;
  }

  public static boolean getExtendedSpendingKey(
      PaymentAddress addr, ExtendedSpendingKey extskOut) {

    IncomingViewingKey ivk = null;
    FullViewingKey fvk = null;

    return getIncomingViewingKey(addr, ivk)
        & getFullViewingKey(ivk, fvk)
        & getSpendingKey(fvk, extskOut);
  }
}
