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

import java.io.Serializable;

public class Witness implements Serializable {

  private static final long serialVersionUID = -7446501098542377380L;

  private String address;

  private String url;

  private long voteCount;

  public String getAddress() {
    return this.address;
  }

  public void setAddress(final String address) {
    this.address = address;
  }

  public String getUrl() {
    return this.url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public long getVoteCount() {
    return this.voteCount;
  }

  public void setVoteCount(final long voteCount) {
    this.voteCount = voteCount;
  }
}
