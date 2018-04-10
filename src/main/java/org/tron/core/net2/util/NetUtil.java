package org.tron.core.net2.util;

import org.apache.commons.collections4.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NetUtil {

    public static List<InetSocketAddress> deleteRepeatedAddress(List<InetSocketAddress> seeds){
        if (CollectionUtils.isEmpty(seeds) || seeds.size() < 2) {
            return seeds;
        }
        List<InetSocketAddress> list = new ArrayList<>();
        seeds.forEach(seed -> {
            if (!list.contains(seed)){
                list.add(seed);
            }
        });
        return list;
    }
}
