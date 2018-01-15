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

package org.tron.trie;


import org.tron.utils.Value;

public class Node {


  private final Value value;
  private boolean dirty;

  public Node(Value val) {
    this(val, false);
  }

  public Node(Value val, boolean dirty) {
    this.value = val;
    this.dirty = dirty;
  }

  public Node copy() {
    return new Node(this.value, this.dirty);
  }

  public boolean isDirty() {
    return dirty;
  }

  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  public Value getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "[" + dirty + ", " + value + "]";
  }
}
