package org.tron.common.zksnark;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;

public class CmUtils {

  static private String CM_FILE_NAME = "cmInfoFile.data";
  static public HashMap<String, CmTuple> cmInfoMap = new HashMap<>();

  public static void loadCmFile() {
    cmInfoMap = loadCmFile(CM_FILE_NAME);
  }

  public static HashMap<String, CmTuple> loadCmFile(String fileName) {

    HashMap<String, CmTuple> cmInfoMap = new HashMap<>();
    BufferedReader file = null;
    try {
      FileReader fileReader = new FileReader(fileName);
      if (fileReader == null) {
        throw new IOException("Resource not found: " + fileName);
      }
      file = new BufferedReader(fileReader);
      String line;
      while ((line = file.readLine()) != null) {
        CmTuple cmTuple = new CmTuple(line);
        cmInfoMap.put(cmTuple.getKeyString(), cmTuple);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (file != null) {
        try {
          file.close();
        } catch (IOException e) {
        }
      }
    }
    return cmInfoMap;
  }

  public static void saveCmFile() {
    saveCmFile(CM_FILE_NAME);
  }

  public static void saveCmFile(String fileName) {
    BufferedWriter bufWriter = null;
    try {
      bufWriter = new BufferedWriter(new FileWriter(fileName));

      for (CmTuple cmTuple : cmInfoMap.values()) {
        try {
          bufWriter.write(cmTuple.toLine());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

    } catch (
        IOException e) {
      e.printStackTrace();
    } finally {
      if (bufWriter != null) {
        try {
          bufWriter.close();
        } catch (IOException e) {
        }
      }
    }


  }

  public static void addCmInfo(byte[] cm, byte[] addr_pk, byte[] addr_sk, byte[] v, byte[] rho,
      byte[] r) {
    CmTuple cmTuple = new CmTuple(cm, addr_pk, addr_sk, v, rho, r);
    cmInfoMap.put(cmTuple.getKeyString(), cmTuple);
  }

  public static void useCmInfo(byte[] cm) {
    CmTuple cmTuple = cmInfoMap.get(ByteArray.toHexString(cm));
    cmTuple.used = 0x01;
    cmInfoMap.put(ByteArray.toHexString(cm), cmTuple);
  }


  public static CmTuple getCm(byte[] cm) {
    return cmInfoMap.get(ByteArray.toHexString(cm));
  }


  public static class CmTuple {

    public static int numCases;
    public int caseNum;
    public byte[] cm;
    public byte[] addr_pk;
    public byte[] addr_sk;
    public byte[] v;
    public byte[] rho;
    public byte[] r;
    public byte used;

    public CmTuple(String line) {
      caseNum = ++numCases;
      String[] x = line.split(":");
      cm = ByteUtil.hexToBytes(x[0]);
      addr_pk = ByteUtil.hexToBytes(x[1]);
      addr_sk = ByteUtil.hexToBytes(x[2]);
      v = ByteUtil.hexToBytes(x[3]);
      rho = ByteUtil.hexToBytes(x[4]);
      r = ByteUtil.hexToBytes(x[5]);
      used = (byte) Character.digit(x[6].charAt(0), 16);
    }

    public CmTuple(byte[] cm, byte[] addr_pk, byte[] addr_sk, byte[] v, byte[] rho, byte[] r) {
      this.cm = cm;
      this.addr_pk = addr_pk;
      this.addr_sk = addr_sk;
      this.v = v;
      this.rho = rho;
      this.r = r;
      used = 0x00;
    }

    public String getKeyString() {
      return ByteArray.toHexString(cm);
    }

    public String toLine() {
      StringBuilder line = new StringBuilder();
      line.append(ByteArray.toHexString(cm));
      line.append(":");
      line.append(ByteArray.toHexString(addr_pk));
      line.append(":");
      line.append(ByteArray.toHexString(addr_sk));
      line.append(":");
      line.append(ByteArray.toHexString(v));
      line.append(":");
      line.append(ByteArray.toHexString(rho));
      line.append(":");
      line.append(ByteArray.toHexString(r));
      line.append(":");
      line.append(used);
      return line.toString();
    }

  }

  public static void main(String[] args) {
    //add
    byte[] cm = {0x0001};
    byte[] addr_pk = {0x02};
    byte[] addr_sk = {0x03};
    byte[] v = {0x04};
    byte[] rho = {0x05};
    byte[] r = {0x06};
    byte used = 0x00;
    CmUtils.addCmInfo(cm, addr_pk, addr_sk, v, rho, r);
    //save
    CmUtils.saveCmFile();
    //load
    CmUtils.loadCmFile();
    //get
    CmTuple cm1 = CmUtils.getCm(cm);
    //use
    CmUtils.useCmInfo(cm);

  }


}
