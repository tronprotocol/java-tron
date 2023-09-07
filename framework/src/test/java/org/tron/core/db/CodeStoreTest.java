package org.tron.core.db;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import javax.annotation.Resource;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.CodeStore;

public class CodeStoreTest extends BaseTest {

  private static String dbDirectory = "db_CodeStore_test";
  private static final byte[] contractAddr1 = Hex.decode(
      "41000000000000000000000000000000000000dEaD");
  private static final byte[] contractAddr2 = Hex.decode(
      "41000000000000000000000000000000000000dEbD");
  private static final byte[] contractAddr3 = Hex.decode(
      "41000000000000000000000000000000000000dEcD");

  private static String codeString =
      "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d28015"
      + "61002a57600080fd5b50600436106100495760003560e01c806385bb7d69146100555761004a565b5b61"
      + "0052610073565b50005b61005d610073565b60405161006a91906100b9565b60405180910390f35b6000"
      + "80600090505b60028110156100a657808261009091906100d4565b915060018161009f91906100d4565b"
      + "905061007b565b5090565b6100b38161012a565b82525050565b60006020820190506100ce6000830184"
      + "6100aa565b92915050565b60006100df8261012a565b91506100ea8361012a565b9250827fffffffffff"
      + "ffffffffffffffffffffffffffffffffffffffffffffffffffffff0382111561011f5761011e61013456"
      + "5b5b828201905092915050565b6000819050919050565b7f4e487b710000000000000000000000000000"
      + "0000000000000000000000000000600052601160045260246000fdfea26474726f6e58221220f3d01983"
      + "23c67293b97323c101e294e6d2cac7fb29555292675277e11c275a4b64736f6c63430008060033";
  private static final CodeCapsule codeCapsule = new CodeCapsule(ByteArray
      .fromHexString(codeString));

  @Resource
  private CodeStore codeStore;

  static {
    dbPath = "output_CodeStore_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath
        },
        Constant.TEST_CONF
    );
  }

  @Test
  public void testGet() {
    codeStore.put(contractAddr1, codeCapsule);
    final CodeCapsule result = codeStore.get(contractAddr1);
    assertEquals(result.toString(), Arrays.toString(ByteArray.fromHexString(codeString)));
  }

  @Test
  public void testGetTotalCodes() throws Exception {
    codeStore.put(contractAddr1, codeCapsule);
    codeStore.put(codeCapsule.getCodeHash().getBytes(), codeCapsule);
    final long result = codeStore.getTotalCodes();
    assertEquals(2L, result);
  }

  @Test
  public void testFindCodeByHash() {
    codeStore.put(codeCapsule.getCodeHash().getBytes(), codeCapsule);
    final byte[] result = codeStore.findCodeByHash(codeCapsule.getCodeHash().getBytes());
    assertEquals(Arrays.toString(result), Arrays.toString(ByteArray.fromHexString(codeString)));
  }
}