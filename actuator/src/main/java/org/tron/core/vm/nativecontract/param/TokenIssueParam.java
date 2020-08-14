package org.tron.core.vm.nativecontract.param;

import lombok.Data;

@Data
public class TokenIssueParam {

    private byte[] ownerAddress;

    private byte[] name;

    private byte[] abbr;

    private long totalSupply;

    private int precision;
}
