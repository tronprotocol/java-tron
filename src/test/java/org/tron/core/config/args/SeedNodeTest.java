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
import org.junit.Test;

public class SeedNodeTest {

  private SeedNode seedNode = new SeedNode();

  @Test
  public void setSeedNode() {
    List<String> ipList = new ArrayList<>();
    ipList.add("127.0.0.1");
    seedNode.setIpList(ipList);
  }

  @Test
  public void setNullIpList() {
    seedNode.setIpList(null);
    Assert.assertEquals(null, seedNode.getIpList());
  }

  @Test
  public void setIpList() {
    List<String> ipList = new ArrayList<>();
    ipList.add("127.0.0.2");
    seedNode.setIpList(ipList);
    Assert.assertEquals("127.0.0.2", seedNode.getIpList().get(0));
  }
}
