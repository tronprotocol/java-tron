package org.tron.storage;


public class ObjectDataSource<V> extends SourceChainBox<byte[], V, byte[], byte[]> {

  ReadCache<byte[], V> cache;
  SourceCodec<byte[], V, byte[], byte[]> codec;
  SourceInter<byte[], byte[]> byteSourceInter;

  public ObjectDataSource(SourceInter<byte[], byte[]> byteSourceInter,
      Serializer<V, byte[]> serializer, int readCacheEntries) {
    super(byteSourceInter);
    this.byteSourceInter = byteSourceInter;
    add(codec = new SourceCodec<>(byteSourceInter, new Serializers.Identity<byte[]>(), serializer));
    if (readCacheEntries > 0) {
      add(cache = new ReadCache.BytesKey<>(codec).withMaxCapacity(readCacheEntries));
    }
  }
}
