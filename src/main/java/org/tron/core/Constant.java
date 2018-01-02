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
package org.tron.core;

import org.tron.utils.ByteArray;

public class Constant {

    // whole
    public final static byte[] LAST_HASH = ByteArray.fromString("lastHash");
    public final static String DIFFICULTY = "2001";

    // DB
    public final static String BLOCK_DB_NAME = "block_data";
    public final static String TRANSACTION_DB_NAME = "transaction_data";

    // kafka
    public final static String TOPIC_BLOCK = "block";
    public final static String TOPIC_TRANSACTION = "transaction";
    public final static Integer PARTITION = 0;




}
