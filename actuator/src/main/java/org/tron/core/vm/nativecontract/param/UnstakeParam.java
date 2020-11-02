package org.tron.core.vm.nativecontract.param;

public class UnstakeParam {
    private byte[] ownerAddress;
    private long now;

    public byte[] getOwnerAddress() {
        return ownerAddress;
    }

    public void setOwnerAddress(byte[] ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    public long getNow() {
        return now;
    }

    public void setNow(long now) {
        this.now = now;
    }
}
