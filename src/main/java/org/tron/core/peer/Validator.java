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

package org.tron.core.peer;


import org.tron.protos.Protocal.Block;

public class Validator {

  public static ValidationRule validationRule = ValidationRuleFactory
      .create("Validation");

  public static byte[] start(Block block) {
    return validationRule.start(block);
  }

  public static void stop() {
    validationRule.stop();
  }

  public static boolean validate(Block block) {
    return validationRule.validate(block);
  }
}