package org.tron.common.zksnark;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JLibarkworks {

    public static boolean libarkworksG1IsValid(byte[] x, byte[] y) {
        return LibarkworksWrapper.getInstance().libarkworksG1IsValid(x, y);
    }
        
    public static boolean libarkworksG2IsValid(byte[] a, byte[] b, byte[] c, byte[] d) {
        return LibarkworksWrapper.getInstance().libarkworksG2IsValid(a, b, c, d);
    }
    
    public static byte[] libarkworksAddG1(byte[] a, byte[] b) {
        byte[] result = new byte[64];
        boolean success = LibarkworksWrapper.getInstance().libarkworksAddG1(a, b, result);
        if (!success) {
            return null;
        }
        return result;
    }
    
    public static byte[] libarkworksMulG1(byte[] p, byte[] s) {
        byte[] result = new byte[64];
        boolean success = LibarkworksWrapper.getInstance().libarkworksMulG1(p, s, result);
        if (!success) {
            return null;
        }
        return result;
    }
    
    public static boolean libarkworksPairingCheck(byte[] g1s, byte[] g2s, int pairs) {
        return LibarkworksWrapper.getInstance().libarkworksPairingCheck(g1s, g2s, pairs);
    }
}
