package org.tron.core.ibc.common;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;

public class Utils {

  public static BlockCapsule buildBlockCapsule(ChainBaseManager chainBaseManager) {
    BlockCapsule blockCapsule =
        new BlockCapsule(
            100,
            Sha256Hash.wrap(chainBaseManager.getGenesisBlockId().getByteString()),
            1,
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(
                        Args.getLocalWitnesses().getPrivateKey()))
                    .getAddress()), Sha256Hash.wrap(ByteArray
            .fromHexString("00000000000000007adbf8dc20423f587a5f3f8ea83e2877e2129c5128c12d11"))
            .getByteString());
    return blockCapsule;
  }
}
