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
package org.tron.common.runtime.vm.program.listener;

import org.tron.common.runtime.vm.DataWord;

import java.util.ArrayList;
import java.util.List;


public class CompositeProgramListener implements ProgramListener {

    private List<ProgramListener> listeners = new ArrayList<>();

    @Override
    public void onMemoryExtend(int delta) {
        for (ProgramListener listener : listeners) {
            listener.onMemoryExtend(delta);
        }
    }

    @Override
    public void onMemoryWrite(int address, byte[] data, int size) {
        for (ProgramListener listener : listeners) {
            listener.onMemoryWrite(address, data, size);
        }
    }

    @Override
    public void onStackPop() {
        for (ProgramListener listener : listeners) {
            listener.onStackPop();
        }
    }

    @Override
    public void onStackPush(DataWord value) {
        for (ProgramListener listener : listeners) {
            listener.onStackPush(value);
        }
    }

    @Override
    public void onStackSwap(int from, int to) {
        for (ProgramListener listener : listeners) {
            listener.onStackSwap(from, to);
        }
    }

    @Override
    public void onStoragePut(DataWord key, DataWord value) {
        for (ProgramListener listener : listeners) {
            listener.onStoragePut(key, value);
        }
    }

    @Override
    public void onStorageClear() {
        for (ProgramListener listener : listeners) {
            listener.onStorageClear();
        }
    }

    public void addListener(ProgramListener listener) {
        listeners.add(listener);
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }
}
