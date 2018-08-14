package org.tron.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class CollectionUtils {

    public static <K, V> List<V> collectList(Collection<K> items, Function<K, V> collector) {
        List<V> collected = new ArrayList<>(items.size());
        for(K item : items) {
            collected.add(collector.apply(item));
        }
        return collected;
    }

    public static <K, V> Set<V> collectSet(Collection<K> items, Function<K, V> collector) {
        Set<V> collected = new HashSet<>();
        for(K item : items) {
            collected.add(collector.apply(item));
        }
        return collected;
    }

    public static <T> List<T> truncate(List<T> items, int limit) {
        if(limit > items.size()) {
            return new ArrayList<>(items);
        }
        List<T> truncated = new ArrayList<>(limit);
        for(T item : items) {
            truncated.add(item);
            if(truncated.size() == limit) {
                break;
            }
        }
        return truncated;
    }

    public static <T> List<T> truncateRandom(List<T> items, int limit, int confirm) {
        if (limit > items.size()) {
            return new ArrayList<>(items);
        }
        List<T> truncated = new ArrayList<>(limit);
        if (confirm >= limit) {
            for (T item : items) {
                truncated.add(item);
                if (truncated.size() == limit) {
                    break;
                }
            }
        } else {
            if (confirm > 0) {
                truncated.addAll(items.subList(0, confirm));
            }
            List<T> endList = items.subList(confirm, items.size());
            Collections.shuffle(endList);
            for (int i = 0; i < limit - confirm; i++) {
                truncated.add(endList.get(i));
            }
        }
        return truncated;
    }

    public static <T> List<T> selectList(Collection<T> items, Predicate<T> predicate) {
        List<T> selected = new ArrayList<>();
        for(T item : items) {
            if(predicate.test(item)) {
                selected.add(item);
            }
        }
        return selected;
    }

    public static <T> Set<T> selectSet(Collection<T> items, Predicate<T> predicate) {
        Set<T> selected = new HashSet<>();
        for(T item : items) {
            if(predicate.test(item)) {
                selected.add(item);
            }
        }
        return selected;
    }
}
