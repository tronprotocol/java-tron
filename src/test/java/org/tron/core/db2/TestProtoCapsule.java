package org.tron.core.db2;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.core.capsule.ProtoCapsule;

import java.util.Arrays;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TestProtoCapsule implements ProtoCapsule<Object> {

    private byte[] value;

    @Override
    public byte[] getData() {
        return value;
    }

    @Override
    public Object getInstance() {
        return value;
    }

    @Override
    public String toString() {
        return "TestProtoCapsule{"
                + "value=" + Arrays.toString(value)
                + ", string=" + (value == null ? "" : new String(value))
                + '}';
    }
}