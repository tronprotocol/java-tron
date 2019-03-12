package org.tron.common.zksnark.sapling;

import org.tron.common.zksnark.sapling.zip32.SaplingExtendedSpendingKey;

public class KeyStore {

  static mapSaplingIncomingViewingKeys;//k:addr,v:ivk
  static mapSaplingFullViewingKeys;//k:ivk,v:fvk
  static mapSaplingSpendingKeys;//k:fvk,v:sk


  //! Sapling
  bool CBasicKeyStore::

  AddSaplingSpendingKey(
    const libzcash::SaplingExtendedSpendingKey&sk,
    const libzcash::SaplingPaymentAddress&defaultAddr) {
    LOCK(cs_SpendingKeyStore);
    auto fvk = sk.expsk.full_viewing_key();

    // if SaplingFullViewingKey is not in SaplingFullViewingKeyMap, add it
    if (!AddSaplingFullViewingKey(fvk, defaultAddr)) {
      return false;
    }

    mapSaplingSpendingKeys[fvk] = sk;

    return true;
  }

  bool CBasicKeyStore::

  AddSaplingFullViewingKey(
    const libzcash::SaplingFullViewingKey&fvk,
    const libzcash::SaplingPaymentAddress&defaultAddr) {
    LOCK(cs_SpendingKeyStore);
    auto ivk = fvk.in_viewing_key();
    mapSaplingFullViewingKeys[ivk] = fvk;

    return CBasicKeyStore::AddSaplingIncomingViewingKey (ivk, defaultAddr);
  }

  // This function updates the wallet's internal address->ivk map.
// If we add an address that is already in the map, the map will
// remain unchanged as each address only has one ivk.
  bool CBasicKeyStore::

  AddSaplingIncomingViewingKey(
    const libzcash::SaplingIncomingViewingKey&ivk,
    const libzcash::SaplingPaymentAddress&addr) {
    LOCK(cs_SpendingKeyStore);

    // Add addr -> SaplingIncomingViewing to SaplingIncomingViewingKeyMap
    mapSaplingIncomingViewingKeys[addr] = ivk;

    return true;
  }


  bool CBasicKeyStore::

  HaveSaplingFullViewingKey(const libzcash::SaplingIncomingViewingKey&ivk) const

  {
    LOCK(cs_SpendingKeyStore);
    return mapSaplingFullViewingKeys.count(ivk) > 0;
  }

  bool CBasicKeyStore::

  HaveSaplingIncomingViewingKey(const libzcash::SaplingPaymentAddress&addr) const

  {
    LOCK(cs_SpendingKeyStore);
    return mapSaplingIncomingViewingKeys.count(addr) > 0;
  }


  bool CBasicKeyStore::

  GetSaplingFullViewingKey(const libzcash::SaplingIncomingViewingKey&ivk,
      libzcash::SaplingFullViewingKey&fvkOut) const

  {
    LOCK(cs_SpendingKeyStore);
    SaplingFullViewingKeyMap::const_iterator mi = mapSaplingFullViewingKeys.find(ivk);
    if (mi != mapSaplingFullViewingKeys.end()) {
      fvkOut = mi -> second;
      return true;
    }
    return false;
  }

  bool CBasicKeyStore::

  GetSaplingIncomingViewingKey(const libzcash::SaplingPaymentAddress&addr,
      libzcash::SaplingIncomingViewingKey&ivkOut) const

  {
    LOCK(cs_SpendingKeyStore);
    SaplingIncomingViewingKeyMap::const_iterator mi = mapSaplingIncomingViewingKeys.find(addr);
    if (mi != mapSaplingIncomingViewingKeys.end()) {
      ivkOut = mi -> second;
      return true;
    }
    return false;
  }

  bool CBasicKeyStore::

  GetSaplingExtendedSpendingKey(const libzcash::SaplingPaymentAddress&addr,
      libzcash::SaplingExtendedSpendingKey&extskOut) const

  {
    libzcash::SaplingIncomingViewingKey ivk;
    libzcash::SaplingFullViewingKey fvk;

    return GetSaplingIncomingViewingKey(addr, ivk) &&
        GetSaplingFullViewingKey(ivk, fvk) &&
        GetSaplingSpendingKey(fvk, extskOut);
  }

}
