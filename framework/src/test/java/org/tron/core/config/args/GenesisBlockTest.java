/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.config.args;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.args.Account;
import org.tron.common.args.GenesisBlock;
import org.tron.common.args.Witness;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

public class GenesisBlockTest {

  private GenesisBlock genesisBlock = new GenesisBlock();

  /**
   * init genesis block.
   */
  @Before
  public void setGenesisBlock() {
    Account account = new Account();

    account.setAccountName("tron");
    account.setAccountType("Normal");
    account
        .setAddress(ByteArray.fromHexString(
            Wallet.getAddressPreFixString() + "4948c2e8a756d9437037dcd8c7e0c73d560ca38d"));
    account.setBalance("10000");

    List<Account> assets = new ArrayList<>();
    assets.add(account);

    genesisBlock.setAssets(assets);

    Witness witness = new Witness();

    witness
        .setAddress(ByteArray.fromHexString(
            Wallet.getAddressPreFixString() + "448d53b2df0cd78158f6f0aecdf60c1c10b15413"));
    witness.setUrl("http://Uranus.org");
    witness.setVoteCount(1000L);

    List<Witness> witnesses = new ArrayList<>();

    witnesses.add(witness);

    genesisBlock.setWitnesses(witnesses);

    genesisBlock.setTimestamp("1");
    genesisBlock
        .setParentHash("0x0000000000000000000000000000000000000000000000000000000000000000");
    genesisBlock.setNumber("0");
  }

  @Test
  public void getDefaultGenesisBlock() {
    GenesisBlock defaultGenesisBlock = GenesisBlock.getDefault();
    Assert.assertEquals(0, defaultGenesisBlock.getAssets().size());
    Assert.assertEquals(0, defaultGenesisBlock.getWitnesses().size());
    Assert.assertEquals(GenesisBlock.DEFAULT_NUMBER, defaultGenesisBlock.getNumber());
    Assert.assertEquals(GenesisBlock.DEFAULT_TIMESTAMP, defaultGenesisBlock.getTimestamp());
    Assert.assertEquals(GenesisBlock.DEFAULT_PARENT_HASH, defaultGenesisBlock.getParentHash());
  }

  @Test
  public void setNullAssets() {
    genesisBlock.setAssets(null);
    Assert.assertEquals(0, genesisBlock.getAssets().size());
  }

  @Test
  public void setAssets() {
    List<Account> assets = new ArrayList<>();
    Account account = new Account();
    assets.add(account);
    genesisBlock.setAssets(assets);
    Assert.assertEquals(1, genesisBlock.getAssets().size());
  }

  @Test
  public void setNullWitnesses() {
    genesisBlock.setWitnesses(null);
    Assert.assertEquals(0, genesisBlock.getWitnesses().size());
  }

  @Test
  public void setWitnesses() {
    List<Witness> witnesses = new ArrayList<>();
    Witness witness = new Witness();
    witnesses.add(witness);
    genesisBlock.setWitnesses(witnesses);
    Assert.assertEquals(1, genesisBlock.getWitnesses().size());
  }

  @Test
  public void whenSetNullTimestampEqualsDefaultTimestamp() {
    genesisBlock.setTimestamp(null);
    Assert.assertEquals(GenesisBlock.DEFAULT_TIMESTAMP, genesisBlock.getTimestamp());
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetBadFormatTimestampShouldThrowIllegalArgumentException() {
    genesisBlock.setTimestamp("123a");
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetNegativeNumberTimestampShouldThrowIllegalArgumentException() {
    genesisBlock.setTimestamp("-1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetExceedTheMaxTimestampShouldThrowIllegalArgumentException() {
    genesisBlock.setTimestamp("9223372036854775808");
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenSetExceedTheMinTimestampShouldThrowIllegalArgumentException() {
    genesisBlock.setTimestamp("-9223372036854775809");
  }

  @Test
  public void setTimestamp() {
    genesisBlock.setTimestamp("1234");
    Assert.assertEquals("1234", genesisBlock.getTimestamp());
  }

  @Test
  public void getTimestamp() {
    Assert.assertEquals("1", genesisBlock.getTimestamp());
  }

  @Test
  public void whenSetNullParentHashEqualsDefaultParentHash() {
    genesisBlock.setParentHash(null);
    Assert.assertEquals(GenesisBlock.DEFAULT_PARENT_HASH, genesisBlock.getParentHash());
  }

  @Test
  public void setParentHash() {
    genesisBlock.setParentHash("0x1234");
    Assert.assertEquals("0x1234", genesisBlock.getParentHash());
  }

  @Test
  public void getParentHash() {
    Assert.assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000",
        genesisBlock.getParentHash());
  }

  @Test
  public void getNumber() {
    Assert.assertEquals("0", genesisBlock.getNumber());
  }
}
