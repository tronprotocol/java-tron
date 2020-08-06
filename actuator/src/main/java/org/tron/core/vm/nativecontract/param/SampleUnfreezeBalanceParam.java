package org.tron.core.vm.nativecontract.param;

import lombok.Data;
import org.tron.protos.contract.Common;

@Data
public class SampleUnfreezeBalanceParam {
    private byte[] ownerAddress;
    private Common.ResourceCode resource;
}