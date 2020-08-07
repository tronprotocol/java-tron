package org.tron.core.vm.nativecontract.param;

import lombok.Data;

@Data
public class TokenIssueParam {

    byte[] ownerAddress;

    byte[] name;

    byte[] abbr;

    long totalSupply;

    int precision;
}
