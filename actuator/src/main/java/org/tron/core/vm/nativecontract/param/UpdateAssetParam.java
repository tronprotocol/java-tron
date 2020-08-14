package org.tron.core.vm.nativecontract.param;

import lombok.Data;

@Data
public class UpdateAssetParam {

    private byte[] ownerAddress;

    private byte[] newUrl;

    private byte[] newDesc;
}
