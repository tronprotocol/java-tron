package org.tron.common.zksnark.sapling;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.tron.common.zksnark.sapling.address.FullViewingKey;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.sapling.zip32.HDSeed;

public class KeyStore {

  static HDSeed seed = new HDSeed();

  private static Map<FullViewingKey, ExtendedSpendingKey> mapSpendingKeys = new HashMap<>();
  private static Map<IncomingViewingKey, FullViewingKey> mapFullViewingKeys = new HashMap<>();
  private static Map<PaymentAddress, IncomingViewingKey> mapIncomingViewingKeys = new HashMap<>();

  public static boolean haveSpendingKey(FullViewingKey fvk) {
    boolean result;
    {
      result = (mapSpendingKeys.get(fvk) != null);
    }
    return result;
  }

  public static boolean addSpendingKey(ExtendedSpendingKey sk, PaymentAddress address) {
    FullViewingKey fvk = sk.getExpsk().fullViewingKey();

    // if SaplingFullViewingKey is not in SaplingFullViewingKeyMap, add it
    if (!addFullViewingKey(fvk, address)) {
      return false;
    }

    addSpendingKey(fvk, sk);

    return true;
  }

  public static void addSpendingKey(FullViewingKey fvk, ExtendedSpendingKey sk) {
    mapSpendingKeys.put(fvk, sk);
  }

  public static boolean addFullViewingKey(FullViewingKey fvk, PaymentAddress address) {
    IncomingViewingKey ivk = fvk.inViewingKey();
    addFullViewingKey(ivk, fvk);

    return addIncomingViewingKey(address, ivk);
  }

  public static void addFullViewingKey(IncomingViewingKey ivk, FullViewingKey fvk) {
    mapFullViewingKeys.put(ivk, fvk);
  }

  // This function updates the wallet's internal address->ivk map.
  // If we add an address that is already in the map, the map will
  // remain unchanged as each address only has one ivk.
  public static boolean addIncomingViewingKey(PaymentAddress addr, IncomingViewingKey ivk) {
    mapIncomingViewingKeys.put(addr, ivk);
    return true;
  }

  public static boolean haveFullViewingKey(IncomingViewingKey ivk) {
    return mapFullViewingKeys.get(ivk) != null;
  }

  public static boolean haveIncomingViewingKey(PaymentAddress addr) {
    return mapIncomingViewingKeys.get(addr) != null;
  }

  public static FullViewingKey getFullViewingKey(IncomingViewingKey ivk) {

    FullViewingKey fvkOut = mapFullViewingKeys.get(ivk);
    return fvkOut;
  }

  public static IncomingViewingKey getIncomingViewingKey(
      PaymentAddress addr) {

    IncomingViewingKey ivkOut = mapIncomingViewingKeys.get(addr);
    return ivkOut;
  }

  public static ExtendedSpendingKey getSpendingKey(FullViewingKey fvk) {
    ExtendedSpendingKey skOut = mapSpendingKeys.get(fvk);
    return skOut;
  }

  public static Optional<ExtendedSpendingKey> getExtendedSpendingKey(PaymentAddress addr) {

    IncomingViewingKey ivk = getIncomingViewingKey(addr);
    if (ivk == null) {
      return Optional.empty();
    }

    FullViewingKey fvk = getFullViewingKey(ivk);
    if (fvk == null) {
      return Optional.empty();
    }

    ExtendedSpendingKey extskOut = getSpendingKey(fvk);
    return Optional.of(extskOut);
  }
}
