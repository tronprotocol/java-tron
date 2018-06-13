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
package org.tron.common.runtime.vm.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.tron.common.runtime.vm.DataWord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.tron.common.utils.ByteUtil.toHexString;

public class OpActions {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Action {

        public enum Name {
            pop,
            push,
            swap,
            extend,
            write,
            put,
            remove,
            clear;
        }

        private Name name;
        private Map<String, Object> params;

        public Name getName() {
            return name;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        Action addParam(String name, Object value) {
            if (value != null) {
                if (params == null) {
                    params = new HashMap<>();
                }
                params.put(name, value.toString());
            }
            return this;
        }
    }

    private List<Action> stack = new ArrayList<>();
    private List<Action> memory = new ArrayList<>();
    private List<Action> storage = new ArrayList<>();

    public List<Action> getStack() {
        return stack;
    }

    public void setStack(List<Action> stack) {
        this.stack = stack;
    }

    public List<Action> getMemory() {
        return memory;
    }

    public void setMemory(List<Action> memory) {
        this.memory = memory;
    }

    public List<Action> getStorage() {
        return storage;
    }

    public void setStorage(List<Action> storage) {
        this.storage = storage;
    }

    private static Action addAction(List<Action> container, Action.Name name) {
        Action action = new Action();
        action.setName(name);

        container.add(action);

        return action;
    }

    public Action addStackPop() {
        return addAction(stack, Action.Name.pop);
    }

    public Action addStackPush(DataWord value) {
        return addAction(stack, Action.Name.push)
                .addParam("value", value);
    }

    public Action addStackSwap(int from, int to) {
        return addAction(stack, Action.Name.swap)
                .addParam("from", from)
                .addParam("to", to);
    }

    public Action addMemoryExtend(long delta) {
        return addAction(memory, Action.Name.extend)
                .addParam("delta", delta);
    }

    public Action addMemoryWrite(int address, byte[] data, int size) {
        return addAction(memory, Action.Name.write)
                .addParam("address", address)
                .addParam("data", toHexString(data).substring(0, size));
    }

    public Action addStoragePut(DataWord key, DataWord value) {
        return addAction(storage, Action.Name.put)
                .addParam("key", key)
                .addParam("value", value);
    }

    public Action addStorageRemove(DataWord key) {
        return addAction(storage, Action.Name.remove)
                .addParam("key", key);
    }

    public Action addStorageClear() {
        return addAction(storage, Action.Name.clear);
    }
}
