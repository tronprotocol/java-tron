/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core.vm.program.invoke;


import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.repository.Repository;

/**
 * @author Roman Mandeleil
 * @since 03.06.2014
 */
public interface ProgramInvoke {

  DataWord getContractAddress();

  DataWord getBalance();

  DataWord getOriginAddress();

  DataWord getCallerAddress();

  DataWord getCallValue();

  DataWord getTokenValue();

  DataWord getTokenId();

  DataWord getDataSize();

  DataWord getDataValue(DataWord indexData);

  byte[] getDataCopy(DataWord offsetData, DataWord lengthData);

  DataWord getPrevHash();

  DataWord getCoinbase();

  DataWord getTimestamp();

  DataWord getNumber();

  DataWord getDifficulty();

  boolean byTestingSuite();

  int getCallDeep();

  Repository getDeposit();

  boolean isStaticCall();

  long getVmShouldEndInUs();

  long getVmStartInUs();

  long getEnergyLimit();

  void setConstantCall();

  boolean isConstantCall();

}
