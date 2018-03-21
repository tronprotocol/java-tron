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

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.utils.StringUtil;
import java.io.Serializable;

public class Witness implements Serializable {

  private static final long serialVersionUID = -7446501098542377380L;

  @Getter
  private String address;

  @Getter
  private String url;

  @Getter
  @Setter
  private long voteCount;

  /**
   * set address.
   */
  public void setAddress(final String address) {
    if (null == address) {
      throw new IllegalArgumentException(
          "The address(" + address + ") must be a 40-bit hexadecimal string.");
    }

    if (StringUtil.isHexString(address, 40)) {
      this.address = address;
    } else {
      throw new IllegalArgumentException(
          "The address(" + address + ") must be a 40-bit hexadecimal string.");
    }
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
