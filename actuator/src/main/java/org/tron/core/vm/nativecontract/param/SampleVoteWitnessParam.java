package org.tron.core.vm.nativecontract.param;

import lombok.Data;
import org.tron.protos.Protocol;

import java.util.ArrayList;

@Data
public class SampleVoteWitnessParam {

    private byte[] ownerAddress;

    private Protocol.Vote vote;

}
