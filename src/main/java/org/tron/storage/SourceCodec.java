package org.tron.storage;


public class SourceCodec<Key, Value, SourceKey, SourceValue> extends AbstractChainedSource<Key, Value, SourceKey, SourceValue> {

    protected Serializer<Key, SourceKey> keySerializer;
    protected Serializer<Value, SourceValue> valSerializer;


    /**
     * Instantiates class
     * @param src  Backing SourceInter
     * @param keySerializer  Key codec Key <=> SourceKey
     * @param valSerializer  Value codec Value <=> SourceValue
     */
    public SourceCodec(SourceInter<SourceKey, SourceValue> src, Serializer<Key, SourceKey> keySerializer, Serializer<Value, SourceValue> valSerializer) {
        super(src);
        this.keySerializer = keySerializer;
        this.valSerializer = valSerializer;
        setFlushSource(true);
    }



    @Override
    public void putData(Key key, Value val) {
        getSourceInter().putData(keySerializer.serialize(key), valSerializer.serialize(val));
    }

    @Override
    public Value getData(Key key) {
        return valSerializer.deserialize(getSourceInter().getData(keySerializer.serialize(key)));
    }

    @Override
    public void deleteData(Key key) {
        getSourceInter().deleteData(keySerializer.serialize(key));
    }

    @Override
    protected boolean flushImpl() {
        return false;
    }

    /**
     * Shortcut class when only value conversion is required
     */
    public static class ValueOnly<Key, Value, SourceValue> extends SourceCodec<Key, Value, Key, SourceValue> {
        public ValueOnly(SourceInter<Key, SourceValue> src, Serializer<Value, SourceValue> valSerializer) {
            super(src, new Serializers.Identity<Key>(), valSerializer);
        }
    }

    /**
     * Shortcut class when only value conversion is required and allKeys are of byte[] type
     */
    public static class BytesKey<Value, SourceValue> extends ValueOnly<byte[], Value, SourceValue> {
        public BytesKey(SourceInter<byte[], SourceValue> src, Serializer<Value, SourceValue> valSerializer) {
            super(src, valSerializer);
        }
    }
}
