package org.tron.core.vm.nativecontract.param;

import lombok.Data;

@Data
public class UpdateAssetParam {

    byte[] ownerAddress;

    byte[] newUrl;

    byte[] newDesc;
}
