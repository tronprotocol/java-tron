package org.tron.consensus.common;

import io.atomix.copycat.Query;

public class GetQuery implements Query<Object> {

    private final Object key;

    public GetQuery(Object key) {
        this.key = key;
    }

    public Object key() {
        return key;
    }
}
