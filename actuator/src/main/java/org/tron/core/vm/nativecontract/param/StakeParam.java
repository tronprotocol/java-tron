package org.tron.core.vm.nativecontract.param;

public class StakeParam {
    private byte[] ownerAddress;
    private byte[] srAddress;
    private long stakeAmount;
    private long now;

    public byte[] getOwnerAddress() {
        return ownerAddress;
    }

    public void setOwnerAddress(byte[] ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    public byte[] getSrAddress() {
        return srAddress;
    }

    public void setSrAddress(byte[] srAddress) {
        this.srAddress = srAddress;
    }

    public long getStakeAmount() {
        return stakeAmount;
    }

    public void setStakeAmount(long stakeAmount) {
        this.stakeAmount = stakeAmount;
    }

    public long getNow() {
        return now;
    }

    public void setNow(long now) {
        this.now = now;
    }
}
