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
package org.tron.common.runtime;

/**
 * @author Roman Mandeleil
 * @since 03.07.2014
 */
public class CallCreate {

  private final byte[] data;
  private final byte[] destination;
  private final byte[] energyLimit;
  private final byte[] value;


  public CallCreate(byte[] data, byte[] destination, byte[] energyLimit, byte[] value) {
    this.data = data;
    this.destination = destination;
    this.energyLimit = energyLimit;
    this.value = value;
  }

  public byte[] getData() {
    return data;
  }

  public byte[] getDestination() {
    return destination;
  }

  public byte[] getEnergyLimit() {
    return energyLimit;
  }

  public byte[] getValue() {
    return value;
  }
}
