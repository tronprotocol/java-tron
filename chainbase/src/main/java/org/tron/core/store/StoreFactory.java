package org.tron.core.store;

import org.tron.core.db2.core.ITronChainBase;
import org.tron.core.exception.TypeMismatchNamingException;

import java.util.HashMap;
import java.util.Map;

public class StoreFactory {
    private Map<String, Object> stores;

    public StoreFactory() {
        stores = new HashMap<>();
    }

    private StoreFactory(Map<String, Object> stores) {
        this.stores = stores;
    }

    public void add(Object store) {
        String name = store.getClass().getSimpleName();
        stores.put(name, store);
    }

    public <T> T getStore(Class<T> clz) throws TypeMismatchNamingException {
        String name = clz.getSimpleName();
        return getStore(name, clz);
    }

    public <T> T getStore(String name, Class<T> clz) throws TypeMismatchNamingException {
        @SuppressWarnings("unchecked")
        T t = (T) stores.get(name);
        if (clz != null && !clz.isInstance(t)) {
            throw new TypeMismatchNamingException(
                    name, clz, (t != null ? t.getClass() : null));
        }
        return t;
    }

    public StoreFactory newInstance() {
        return new StoreFactory(new HashMap<>(stores));
    }
 }
