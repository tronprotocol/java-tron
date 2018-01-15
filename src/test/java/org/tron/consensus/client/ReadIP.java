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

package org.tron.consensus.client;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ReadIP {
  public String ReadFile(String Path) {
    BufferedReader reader = null;
    String laststr = "";
    try {
      FileInputStream fileInputStream = new FileInputStream(Path);
      InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
      reader = new BufferedReader(inputStreamReader);
      String tempString = null;
      while ((tempString = reader.readLine()) != null) {
        laststr += tempString;
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return laststr;
  }

}
