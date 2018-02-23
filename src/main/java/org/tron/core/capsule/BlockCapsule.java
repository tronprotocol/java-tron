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

package org.tron.core.capsule;

import org.tron.core.Sha256Hash;
import org.tron.core.peer.Validator;
import org.tron.protos.Protocal.Block;


public class BlockCapsule {

  private byte[] serializEncode;

  private Block block;

  private Sha256Hash hash;

  public Block getBlock() {
    return block;
  }

  public boolean validate() {
    return Validator.validate(block);
  }

  public BlockCapsule(Block blk) {
    this.block = blk;
    this.hash = Sha256Hash.of(this.block.toByteArray());
  }

  public Sha256Hash getParentHash() {
    return Sha256Hash.wrap(this.block.getBlockHeader().getParentHash());
  }

  public Sha256Hash getHash() {
    return hash;
  }

  public long getNum() {
    return this.block.getBlockHeader().getNumber();
  }
}
