package org.tron.core.vm.nativecontract.param;

import lombok.Data;

@Data
public class StakeParam {
    private byte[] ownerAddress;
    private byte[] srAddress;
    private long stakeAmount;
}
