
package org.tron.core.capsule.utils;

import org.tron.common.utils.ByteUtil;

import java.util.ArrayList;

/**
 */
public class RLPList extends ArrayList<RLPElement> implements RLPElement {

  byte[] rlpData;

  public void setRLPData(byte[] rlpData) {
    this.rlpData = rlpData;
  }

  public byte[] getRLPData() {
    return rlpData;
  }

  public static void recursivePrint(RLPElement element) {

    if (element == null) {
      throw new RuntimeException("RLPElement object can't be null");
    }
    if (element instanceof RLPList) {

      RLPList rlpList = (RLPList) element;
      System.out.print("[");
      for (RLPElement singleElement : rlpList) {
        recursivePrint(singleElement);
      }
      System.out.print("]");
    } else {
      String hex = ByteUtil.toHexString(element.getRLPData());
      System.out.print(hex + ", ");
    }
  }
}
