
package org.tron.storage;


public interface MemSizeEstimator<E> {

    long estimateSize(E e);

    /**
     * byte[] type size estimator
     */
    MemSizeEstimator<byte[]> ByteArrayEstimator = new MemSizeEstimator<byte[]>() {
        @Override
        public long estimateSize(byte[] bytes) {
            return bytes == null ? 0 : bytes.length + 4; // 4 - compressed ref size
        }
    };
}
