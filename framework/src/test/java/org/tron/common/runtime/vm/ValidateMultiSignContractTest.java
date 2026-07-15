package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.client.utils.AbiUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.PrecompiledContracts.ValidateMultiSign;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;


@Slf4j
public class ValidateMultiSignContractTest extends BaseTest {

  private static final String METHOD_SIGN = "validatemultisign(address,uint256,bytes32,bytes[])";
  private static final byte[] longData;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath(), "--debug"}, TestConstants.TEST_CONF);
    longData = new byte[1000000];
    Arrays.fill(longData, (byte) 2);
  }

  ValidateMultiSign contract = new ValidateMultiSign();

  @Before
  public void before() {
    dbManager.getDynamicPropertiesStore().saveAllowMultiSign(1);
    dbManager.getDynamicPropertiesStore().saveTotalSignNum(5);
  }

  @Test
  public void testAddressNonExist() {
    byte[] hash = Hash.sha3(longData);
    ECKey key = new ECKey();
    byte[] sign = key.sign(hash).toByteArray();
    List<Object> signs = new ArrayList<>();
    signs.add(Hex.toHexString(sign));

    //Address non exist
    Assert.assertArrayEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), 1, hash, signs)
            .getValue(), DataWord.ZERO().getData());
  }

  @Test
  public void testDifferentCase() {
    //Create an account with permission

    ECKey key = new ECKey();
    AccountCapsule toAccount = new AccountCapsule(ByteString.copyFrom(key.getAddress()),
        Protocol.AccountType.Normal,
        System.currentTimeMillis(), true, dbManager.getDynamicPropertiesStore());

    ECKey key1 = new ECKey();
    ECKey key2 = new ECKey();

    Protocol.Permission activePermission =
        Protocol.Permission.newBuilder()
            .setType(Protocol.Permission.PermissionType.Active)
            .setId(2)
            .setPermissionName("active")
            .setThreshold(2)
            .setOperations(ByteString.copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
            .addKeys(Protocol.Key.newBuilder().setAddress(ByteString.copyFrom(key1.getAddress()))
                .setWeight(1).build())
            .addKeys(
                Protocol.Key.newBuilder()
                    .setAddress(ByteString.copyFrom(key2.getAddress()))
                    .setWeight(1)
                    .build())
            .build();

    toAccount
        .updatePermissions(toAccount.getPermissionById(0), null,
            Collections.singletonList(activePermission));
    dbManager.getAccountStore().put(key.getAddress(), toAccount);

    //generate data

    byte[] address = key.getAddress();
    int permissionId = 2;
    byte[] data = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), longData);

    //combine data
    byte[] merged = ByteUtil.merge(address, ByteArray.fromInt(permissionId), data);
    //sha256 of it
    byte[] toSign = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), merged);

    //sign data

    List<Object> signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    //add Repetitive
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    signs.add(Hex.toHexString(key2.sign(toSign).toByteArray()));

    Assert.assertArrayEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ONE().getData());

    //after optimized
    VMConfig.initAllowTvmSelfdestructRestriction(1);
    Assert.assertArrayEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ONE().getData());
    VMConfig.initAllowTvmSelfdestructRestriction(0);

    //weight not enough
    signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    Assert.assertArrayEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ZERO().getData());

    //put wrong sign
    signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    Assert.assertArrayEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ZERO().getData());

    signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    signs.add(Hex.toHexString(new ECKey().sign(toSign).toByteArray()));

    Assert.assertArrayEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ZERO().getData());
  }


  // TIP-854: after activation, validateMultiSign (H=5, I=5) must reject calldata
  // whose byte length is incompatible with the (words - 5) / 5 shape the per-call
  // energy formula already assumes, returning (false, empty).
  @Test
  public void testTip854RejectsMalformedCalldata() {
    VMConfig.initAllowTvmOsaka(1);
    try {
      // Bucket 1: 32-aligned head + sub-word trailing bytes (r=1, r=31).
      for (int r : new int[]{1, 31}) {
        byte[] data = new byte[(5 + 5) * 32 + r];
        Pair<Boolean, byte[]> ret = contract.execute(data);
        Assert.assertFalse("non-32-aligned len=" + data.length, ret.getLeft());
        Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
      }
      // Bucket 2: fewer than the static head's 5 words.
      for (int bytes : new int[]{0, 32, 64, 96, 128}) {
        Pair<Boolean, byte[]> ret = contract.execute(new byte[bytes]);
        Assert.assertFalse("len=" + bytes + " < 5 words", ret.getLeft());
        Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
      }
      // Bucket 3: 32-aligned but tail not a multiple of I=5 words (k = 1..4).
      for (int k = 1; k <= 4; k++) {
        byte[] data = new byte[(5 + k) * 32];
        Pair<Boolean, byte[]> ret = contract.execute(data);
        Assert.assertFalse("aligned bad-tail k=" + k, ret.getLeft());
        Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
      }
      // Null calldata: explicit spec clause.
      Pair<Boolean, byte[]> ret = contract.execute(null);
      Assert.assertFalse("null calldata", ret.getLeft());
      Assert.assertSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
    } finally {
      VMConfig.initAllowTvmOsaka(0);
    }
  }

  // TIP-854 Compatibility: for canonically-shaped calldata (real 65-byte sigs,
  // total length == 5*32 + 5*32*N), behaviour must be identical pre- vs
  // post-activation — the guard is a no-op for well-formed inputs.
  @Test
  public void testTip854CanonicalInputUnchanged() {
    ECKey key = new ECKey();
    AccountCapsule toAccount = new AccountCapsule(ByteString.copyFrom(key.getAddress()),
        Protocol.AccountType.Normal,
        System.currentTimeMillis(), true, dbManager.getDynamicPropertiesStore());
    ECKey key1 = new ECKey();
    ECKey key2 = new ECKey();
    Protocol.Permission activePermission =
        Protocol.Permission.newBuilder()
            .setType(Protocol.Permission.PermissionType.Active)
            .setId(2)
            .setPermissionName("active")
            .setThreshold(2)
            .setOperations(ByteString.copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
            .addKeys(Protocol.Key.newBuilder().setAddress(ByteString.copyFrom(key1.getAddress()))
                .setWeight(1).build())
            .addKeys(Protocol.Key.newBuilder().setAddress(ByteString.copyFrom(key2.getAddress()))
                .setWeight(1).build())
            .build();
    toAccount.updatePermissions(toAccount.getPermissionById(0), null,
        Collections.singletonList(activePermission));
    dbManager.getAccountStore().put(key.getAddress(), toAccount);

    byte[] data = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), longData);
    byte[] merged = ByteUtil.merge(key.getAddress(), ByteArray.fromInt(2), data);
    byte[] toSign = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), merged);
    List<Object> signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    signs.add(Hex.toHexString(key2.sign(toSign).toByteArray()));

    VMConfig.initAllowTvmOsaka(0);
    Pair<Boolean, byte[]> pre =
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), 2, data, signs);
    VMConfig.initAllowTvmOsaka(1);
    try {
      Pair<Boolean, byte[]> post =
          validateMultiSign(StringUtil.encode58Check(key.getAddress()), 2, data, signs);
      Assert.assertEquals(pre.getLeft(), post.getLeft());
      Assert.assertArrayEquals(pre.getValue(), post.getValue());
      Assert.assertArrayEquals(DataWord.ONE().getData(), post.getValue());
    } finally {
      VMConfig.initAllowTvmOsaka(0);
    }
  }

  // TIP-854: before activation, malformed calldata reaches the legacy decoder.
  // Assert the guard is not taken — this precompile has no outer catch, so a
  // too-short input raises inside the decoder; that is the documented
  // pre-activation failure mode the TIP explicitly preserves.
  @Test
  public void testTip854PreActivationNoOp() {
    VMConfig.initAllowTvmOsaka(0);
    contract.setRepository(RepositoryImpl.createRoot(StoreFactory.getInstance()));
    try {
      Pair<Boolean, byte[]> ret = contract.execute(new byte[(5 + 1) * 32]);
      // If the decoder happened to handle it without raising, we must not have
      // taken the post-activation reject path (false, empty).
      Assert.assertNotSame(ByteUtil.EMPTY_BYTE_ARRAY, ret.getRight());
    } catch (RuntimeException expectedLegacyBehaviour) {
      // Pre-activation: decoder may throw — this is the existing behaviour.
    }
  }

  Pair<Boolean, byte[]> validateMultiSign(String address, int permissionId, byte[] hash,
      List<Object> signatures) {
    List<Object> parameters = Arrays
        .asList(address, permissionId, "0x" + Hex.toHexString(hash), signatures);
    byte[] input = Hex.decode(AbiUtil.parseParameters(METHOD_SIGN, parameters));
    Repository deposit = RepositoryImpl.createRoot(StoreFactory.getInstance());
    logger.info("energy for data:{}", contract.getEnergyForData(input));
    contract.setRepository(deposit);

    Pair<Boolean, byte[]> ret = contract.execute(input);

    logger.info("BytesArray:{}，HexString:{}", Arrays.toString(ret.getValue()),
        Hex.toHexString(ret.getValue()));
    return ret;
  }


}
