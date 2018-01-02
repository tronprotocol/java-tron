package org.tron.storage;


public class ObjectDataSourceInter<V> extends SourceInterChainBox<byte[], V, byte[], byte[]> {

    ReadCache<byte[], V> cache;
    SourceInterCodec<byte[], V, byte[], byte[]> codec;
    SourceInter<byte[], byte[]> byteSourceInter;

    public ObjectDataSourceInter(SourceInter<byte[], byte[]> byteSourceInter, Serializer<V, byte[]> serializer, int readCacheEntries) {
        super(byteSourceInter);
        this.byteSourceInter = byteSourceInter;
        add(codec = new SourceInterCodec<>(byteSourceInter, new Serializers.Identity<byte[]>(), serializer));
        if (readCacheEntries > 0) {
            add(cache = new ReadCache.BytesKey<>(codec).withMaxCapacity(readCacheEntries));
        }
    }
}
