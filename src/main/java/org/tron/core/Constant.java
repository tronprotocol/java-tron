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

import org.tron.common.utils.ByteArray;

public class Constant {

  // whole
  public static final byte[] LAST_HASH = ByteArray.fromString("lastHash");
  public static final String DIFFICULTY = "2001";

  // DB
  public static final String BLOCK_DB_NAME = "block_data";
  public static final String TRANSACTION_DB_NAME = "transaction_data";

  //config for testnet, mainnet, beta
  public static final String TESTNET_CONF = "config.conf";

  //config for junit test
  public static final String TEST_CONF = "config-test.conf";

  public static final String DATABASE_DIR = "storage.directory";

  public static final byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x41;   //41 + address
  public static final String ADD_PRE_FIX_STRING_MAINNET = "41";
  public static final byte ADD_PRE_FIX_BYTE_TESTNET = (byte) 0xa0;   //a0 + address
  public static final String ADD_PRE_FIX_STRING_TESTNET = "a0";
  public static final int ADDRESS_SIZE = 42;

  // config for transaction
  public static final long TRANSACTION_MAX_BYTE_SIZE = 500 * 1_024L;
  public static final long MAXIMUM_TIME_UNTIL_EXPIRATION = 24 * 60 * 60 * 1_000L; //one day
  public static final long TRANSACTION_DEFAULT_EXPIRATION_TIME = 60 * 1_000L; //60 seconds

  // config for smart contract
  public static final long MEM_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT = 32 * 1024 * 1024L; // 32MB
  public static final long MAX_CPU_TIME_OF_ONE_TX = 50000; // 50 ms = 50000 us
  public static final long STORAGE_LIMIT_IN_ONE_TX_OF_SMART_CONTRACT = 32 * 1024 * 1024L; // 32MB
  public static final long SUN_PER_ENERGY = 100; // 1 us = 100 DROP = 100 * 10^-6 TRX
  public static final long MAX_ENERGY_IN_TX = 3000000; // ref: 1 us = 1 energy
  public static final long MAX_RESULT_SIZE_IN_TX = 64; // max 8 * 8 items in result
  public static final long MAX_CONSUME_USER_RESOURCE_PERCENT = 100L;
  public static final long MIN_CONSUME_USER_RESOURCE_PERCENT = 0L;
  public static final long ACCORD_RANGE_PERCENT = 0L;

}
