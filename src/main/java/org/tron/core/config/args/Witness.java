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
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;

public class Witness implements Serializable {

  private static final long serialVersionUID = -7446501098542377380L;

  @Getter
  private byte[] address;

  @Getter
  private String url;

  @Getter
  @Setter
  private long voteCount;

  /**
   * set address.
   */
  public void setAddress(final byte[] address) {
    if ( !Wallet.addressValid(address)){
      throw new IllegalArgumentException(
          "The address(" + StringUtil.createReadableString(address) + ") must be a 21 bytes.");
    }
    this.address = address;
  }

  /**
   * set url.
   */
  public void setUrl(final String url) {
    if (StringUtils.isBlank(url)) {
      throw new IllegalArgumentException(
          "The url(" + url + ") format error.");
    }

    this.url = url;
  }
}
