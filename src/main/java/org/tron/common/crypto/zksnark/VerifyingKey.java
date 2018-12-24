package org.tron.common.crypto.zksnark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import org.tron.common.utils.ByteArray;

public class VerifyingKey {

  private G2Point A;
  private G1Point B;
  private G2Point C;
  private G2Point gamma;
  private G1Point gammaBeta1;
  private G2Point gammaBeta2;
  private G2Point Z;
  private G1Point[] IC;

  public G2Point getA() {
    return A;
  }

  public G1Point getB() {
    return B;
  }

  public G2Point getC() {
    return C;
  }

  public G2Point getGamma() {
    return gamma;
  }

  public G1Point getGammaBeta1() {
    return gammaBeta1;
  }

  public G2Point getGammaBeta2() {
    return gammaBeta2;
  }

  public G2Point getZ() {
    return Z;
  }

  public G1Point[] getIC() {
    return IC;
  }

  private VerifyingKey() {

  }

  private static VerifyingKey vk;

  public static VerifyingKey getInstance() {
    if (vk == null) {
      vk = new VerifyingKey();
      vk.A = Pairing.G2Point(
          "[0x209dd15ebff5d46c4bd888e51a93cf99a7329636c63514396b4a452003a35bf7, 0x04bf11ca01483bfa8b34b43561848d28905960114c8ac04049af4b6315a41678], [0x2bb8324af6cfc93537a2ad1a445cfd0ca2a71acd7ac41fadbf933c2a51be344d, 0x120a2a4cf30c1bf9845f20c6fe39e07ea2cce61f0c9bb048165fe5e4de877550]");
      vk.B = Pairing.G1Point(
          "0x2eca0c7238bf16e83e7a1e6c5d49540685ff51380f309842a98561558019fc02, 0x03d3260361bb8451de5ff5ecd17f010ff22f5c31cdf184e9020b06fa5997db84");
      vk.C = Pairing.G2Point(
          "[0x2e89718ad33c8bed92e210e81d1853435399a271913a6520736a4729cf0d51eb, 0x01a9e2ffa2e92599b68e44de5bcf354fa2642bd4f26b259daa6f7ce3ed57aeb3], [0x14a9a87b789a58af499b314e13c3d65bede56c07ea2d418d6874857b70763713, 0x178fb49a2d6cd347dc58973ff49613a20757d0fcc22079f9abd10c3baee24590]");
      vk.gamma = Pairing.G2Point(
          "[0x25f83c8b6ab9de74e7da488ef02645c5a16a6652c3c71a15dc37fe3a5dcb7cb1, 0x22acdedd6308e3bb230d226d16a105295f523a8a02bfc5e8bd2da135ac4c245d], [0x065bbad92e7c4e31bf3757f1fe7362a63fbfee50e7dc68da116e67d600d9bf68, 0x06d302580dc0661002994e7cd3a7f224e7ddc27802777486bf80f40e4ca3cfdb]");
      vk.gammaBeta1 = Pairing.G1Point(
          "0x15794ab061441e51d01e94640b7e3084a07e02c78cf3103c542bc5b298669f21, 0x14db745c6780e9df549864cec19c2daf4531f6ec0c89cc1c7436cc4d8d300c6d");
      vk.gammaBeta2 = Pairing.G2Point(
          "[0x1f39e4e4afc4bc74790a4a028aff2c3d2538731fb755edefd8cb48d6ea589b5e, 0x283f150794b6736f670d6a1033f9b46c6f5204f50813eb85c8dc4b59db1c5d39], [0x140d97ee4d2b36d99bc49974d18ecca3e7ad51011956051b464d9e27d46cc25e, 0x0764bb98575bd466d32db7b15f582b2d5c452b36aa394b789366e5e3ca5aabd4]");
      vk.Z = Pairing.G2Point(
          "[0x217cee0a9ad79a4493b5253e2e4e3a39fc2df38419f230d341f60cb064a0ac29, 0x0a3d76f140db8418ba512272381446eb73958670f00cf46f1d9e64cba057b53c], [0x26f64a8ec70387a13e41430ed3ee4a7db2059cc5fc13c067194bcc0cb49a9855, 0x2fd72bd9edb657346127da132e5b82ab908f5816c826acb499e22f2412d1a2d7]");
      vk.IC = new G1Point[10];
      vk.IC[0] = Pairing.G1Point(
          "0x0aee46a7ea6e80a3675026dfa84019deee2a2dedb1bbe11d7fe124cb3efb4b5a, 0x044747b6e9176e13ede3a4dfd0d33ccca6321b9acd23bf3683a60adc0366ebaf");
      vk.IC[1] = Pairing.G1Point(
          "0x1e39e9f0f91fa7ff8047ffd90de08785777fe61c0e3434e728fce4cf35047ddc, 0x2e0b64d75ebfa86d7f8f8e08abbe2e7ae6e0a1c0b34d028f19fa56e9450527cb");
      vk.IC[2] = Pairing.G1Point(
          "0x1c36e713d4d54e3a9644dffca1fc524be4868f66572516025a61ca542539d43f, 0x042dcc4525b82dfb242b09cb21909d5c22643dcdbe98c4d082cc2877e96b24db");
      vk.IC[3] = Pairing.G1Point(
          "0x17d5d09b4146424bff7e6fb01487c477bbfcd0cdbbc92d5d6457aae0b6717cc5, 0x02b5636903efbf46db9235bbe74045d21c138897fda32e079040db1a16c1a7a1");
      vk.IC[4] = Pairing.G1Point(
          "0x0f103f14a584d4203c27c26155b2c955f8dfa816980b24ba824e1972d6486a5d, 0x0c4165133b9f5be17c804203af781bcf168da7386620479f9b885ecbcd27b17b");
      vk.IC[5] = Pairing.G1Point(
          "0x232063b584fb76c8d07995bee3a38fa7565405f3549c6a918ddaa90ab971e7f8, 0x2ac9b135a81d96425c92d02296322ad56ffb16299633233e4880f95aafa7fda7");
      vk.IC[6] = Pairing.G1Point(
          "0x09b54f111d3b2d1b2fe1ae9669b3db3d7bf93b70f00647e65c849275de6dc7fe, 0x18b2e77c63a3e400d6d1f1fbc6e1a1167bbca603d34d03edea231eb0ab7b14b4");
      vk.IC[7] = Pairing.G1Point(
          "0x0c54b42137b67cc268cbb53ac62b00ecead23984092b494a88befe58445a244a, 0x18e3723d37fae9262d58b548a0575f59d9c3266db7afb4d5739555837f6b8b3e");
      vk.IC[8] = Pairing.G1Point(
          "0x0a6de0e2240aa253f46ce0da883b61976e3588146e01c9d8976548c145fe6e4a, 0x04fbaa3a4aed4bb77f30ebb07a3ec1c7d77a7f2edd75636babfeff97b1ea686e");
      vk.IC[9] = Pairing.G1Point(
          "0x111e2e2a5f8828f80ddad08f9f74db56dac1cc16c1cb278036f79a84cf7a116f, 0x1d7d62e192b219b9808faa906c5ced871788f6339e8d91b83ac1343e20a16b30");
    }
    return vk;
  }

  public static void assertAequalsB(byte a, byte b) throws IOException {
    if (a != b) {
      byte[] A = {a};
      byte[] B = {b};
      throw new IOException(
          "Need " + ByteArray.toHexString(A) + " but found " + ByteArray.toHexString(B));
    }
  }

  public static VerifyingKey loadVk() {
    if (vk == null) {
      File file = new File("sprout-verifying.key");
      Long filelength = file.length();
      byte[] filecontent = new byte[filelength.intValue()];
      try {
        FileInputStream in = new FileInputStream(file);
        in.read(filecontent);
        in.close();
        vk = new VerifyingKey();
        int offset = 0;
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.A = Pairing.G2Point(Arrays.copyOfRange(filecontent, offset, offset + 128));
        offset += 128;
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.B = Pairing.G1Point(Arrays.copyOfRange(filecontent, offset, offset + 64));
        offset += 64;
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.C = Pairing.G2Point(Arrays.copyOfRange(filecontent, offset, offset + 128));
        offset += 128;
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.gamma = Pairing.G2Point(Arrays.copyOfRange(filecontent, offset, offset + 128));
        offset += 128;
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.gammaBeta1 = Pairing.G1Point(Arrays.copyOfRange(filecontent, offset, offset + 64));
        offset += 64;
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.gammaBeta2 = Pairing.G2Point(Arrays.copyOfRange(filecontent, offset, offset + 128));
        offset += 128;
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.Z = Pairing.G2Point(Arrays.copyOfRange(filecontent, offset, offset + 128));
        offset += 128;
        vk.IC = new G1Point[10];
        assertAequalsB((byte) (0x30), filecontent[offset++]);
        vk.IC[0] = Pairing.G1Point(Arrays.copyOfRange(filecontent, offset, offset + 64));
        offset += 88;
        for (int i = 1; i < 10; i++) {
          assertAequalsB((byte) (0x30), filecontent[offset++]);
          vk.IC[i] = Pairing.G1Point(Arrays.copyOfRange(filecontent, offset, offset + 64));
          offset += 64;
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return vk;
  }

  // zcash
  public static VerifyingKey initVk() {
    if (vk == null) {
      vk = new VerifyingKey();
      vk.A = new G2Point(
          "14752851163271972921165116810778899752274893127848647655434033030151679466487",
          "2146841959437886920191033516947821737903543682424168472444605468016078231160",
          "19774899457345372253936887903062884289284519982717033379297427576421785416781",
          "8159591693044959083845993640644415462154314071906244874217244895511876957520");
      vk.B = new G1Point(
          "21163380042281667028194921861846440787793088615342153907557220755287297358850",
          "1730005633951488561162401768080055521907218407650744548610087024095695199108");
      vk.C = new G2Point(
          "21049232722760520884910305096518213957309396732908002623546433288382066807275",
          "752476689148090443252690606274719847522796924289184281944322016120845872819",
          "9346016947773545029940290874113526292203330783138316933543286726319309993747",
          "10657101118636466197534311304303971390099046792106599174009327086566056805776");
      vk.gamma = new G2Point(
          "17174171333098854828033634539500164902488935492941049779522958919502622588081",
          "15684072703239714088748884492940919778409948011906556607893998678768263898205",
          "2875949754418862832249955782188169764124161746616276218844232725526931423080",
          "3086697999584045732786424745914779370086036468911841736272013630524944011227");
      vk.gammaBeta1 = new G1Point(
          "9712873799510369170966410452086991795283841876597505062330138725142728449825",
          "9433999572643313025031367487888933220352848413609488855427374525420646960237");
      vk.gammaBeta2 = new G2Point(
          "14123988352319117906018970862315159679452117471849989000282265698764599106398",
          "18203970449465878141055527247672796515569702004956673464850250575302350363961",
          "9070274571799942693810232181825350369966811716654884009331987967306715939422",
          "3344169380239392314048474373026629561296701202792428464496597755254021991380");
      vk.Z = new G2Point(
              "15147055940817099984713168864119185960995485721468434382981947300642935581737",
              "4631727067030503710010688256995134761045201948838871620017875546783390086460",
              "17623047202600292659611642134440671974256480551657416500487803939184025704533",
              "21638878652776235365545898652250152098523031965244840843903617079107891864279");
      vk.IC = new G1Point[10];
      vk.IC[0] = new G1Point(
          "4944125736493822447335225095051526251764804673819722614680138374080051759962",
          "1935192491180648890600311215252271941452272522684171010354270378941282184111");
      vk.IC[1] = new G1Point(
          "13671710343712145123751755431743289257188978742535474396465034058168696864220",
          "20826522333544227498944395534998927652160019773231902383997070883222962120651");
      vk.IC[2] = new G1Point(
          "12761764339888541584683044940570653033593327533706847447891003583652324561983",
          "1890169332711480046756085850376547686758361763522376714890812739379155117275");
      vk.IC[3] = new G1Point(
          "10780973691118990463572234139590032304523642666772690042271497225434343505093",
          "1225111119988715799286416715484295049110336342693607646598445309214414972833");
      vk.IC[4] = new G1Point(
          "6813397648435401772315777392068447297731770034820995962730599333379629935197",
          "5543296837108785826068557070639753614637117027747616223928176042346981863803");
      vk.IC[5] = new G1Point(
          "15888176973130579702136584647732320447911977285971866534948545524629530339320",
          "19353498956202835216323577418447420319043865439124272799134659660891866791335");
      vk.IC[6] = new G1Point(
          "4391160655333174988591015543994926300076235924540378279303074871686753798142",
          "11171604800461778651579303937810014677300582907756707298692424617842305602740");
      vk.IC[7] = new G1Point(
          "5577412546328490241391307238739013120425748898832356403880504969857771119690",
          "11257371099238762117045275690719175766562617360639429481885451902339768879934");
      vk.IC[8] = new G1Point(
          "4717266903818752750408066803467256739157750721583295354208673132324161744458",
          "2253904876039028511475843193830314875944384935466740271927641917713621346414");
      vk.IC[9] = new G1Point(
          "7742642460569273216539674856471756904887522145302233146876244281004809392495",
          "13338610944590869762446817049541912676528855874207736821753831893421715974960");
    }
    return vk;
  }

  // load verify from file
  public static VerifyingKey initVkFromFile() {
    if (vk == null) {
      vk = new VerifyingKey();
      File file = new File("sprout-verifying.key");
      Long filelength = file.length();
      System.out.println("file size: " + filelength);
      byte[] filecontent = new byte[filelength.intValue()];
      try {
        FileInputStream in = new FileInputStream(file);
        in.read(filecontent);
        int startPos = 0;

        //A (x>y 0>1)
        vk.A = readG2(startPos, filecontent);
        startPos += 129;

        //B
        vk.B  = readG1(startPos, filecontent);
        startPos += 65;

        //C
        vk.C = readG2(startPos, filecontent);
        startPos += 129;

        //gamma_g2
        vk.gamma = readG2(startPos, filecontent);
        startPos += 129;

        //gamma_beta_g1
        vk.gammaBeta1 = readG1(startPos, filecontent);
        startPos += 65;

        //gamma_beta_g2
        vk.gammaBeta2 = readG2(startPos, filecontent);
        startPos += 129;

        //rC_Z_g2
        vk.Z = readG2(startPos, filecontent);
        startPos += 129;

        vk.IC = new G1Point[10];
        vk.IC[0] = readG1(startPos, filecontent);
        startPos += 65;

        // 1-10
        startPos += 24;
        for(int i=1; i<10; i++) {
          vk.IC[i] = readG1(startPos, filecontent);
          startPos += 65;
        }

        if ( startPos != filelength.intValue() ) {
            System.out.println("init vk failure!");
            return null;
        }
        in.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return vk;
  }


    // load verify from file
    public static VerifyingKey initVkFromFileRaw() throws Exception {
        if (vk == null) {
            vk = new VerifyingKey();
            File file = new File("sprout-verifying.key");
            Long filelength = file.length();
            System.out.println("file size: " + filelength);
            byte[] filecontent = new byte[filelength.intValue()];
            try {
                FileInputStream in = new FileInputStream(file);
                in.read(filecontent);
                int startPos = 0;

                //A (x>y 0>1)
                vk.A = readG2Raw(startPos, filecontent);
                startPos += 129;

                //B
                vk.B  = readG1Raw(startPos, filecontent);
                startPos += 65;

                //C
                vk.C = readG2Raw(startPos, filecontent);
                startPos += 129;

                //gamma_g2
                vk.gamma = readG2Raw(startPos, filecontent);
                startPos += 129;

                //gamma_beta_g1
                vk.gammaBeta1 = readG1Raw(startPos, filecontent);
                startPos += 65;

                //gamma_beta_g2
                vk.gammaBeta2 = readG2Raw(startPos, filecontent);
                startPos += 129;

                //rC_Z_g2
                vk.Z = readG2Raw(startPos, filecontent);
                startPos += 129;

                vk.IC = new G1Point[10];
                vk.IC[0] = readG1Raw(startPos, filecontent);
                startPos += 65;

                // 1-10
                startPos += 24;
                for(int i=1; i<10; i++) {
                    vk.IC[i] = readG1Raw(startPos, filecontent);
                    startPos += 65;
                }

                if ( startPos != filelength.intValue() ) {
                    System.out.println("init vk failure!");
                    return null;
                }
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return vk;
    }

  public static boolean checkG1Poin(G1Point g1) {
    BN128G1 g11 = g1.toBN128G1();
    if (g11 == null) {
      return false;
    }
    return true;
  }

  public static boolean checkG2Poin(G2Point g2) {
    BN128G2 g21 = g2.toBN128G2();
    if (g21 == null) {
      return false;
    }
    return true;
  }

  public static G1Point G1Point(byte[] x, byte[] y) {
    ZksnarkUtils.sort(x);
    ZksnarkUtils.sort(y);
    return new G1Point(x, y);
  }

  public static void test() {
    byte[] x = ByteArray
        .fromHexString("e16358efd807dd3a3f0c68618cc81605dcdbf35a6354b5fa81b2390eae345927");
    byte[] y = ByteArray
        .fromHexString("6ec608532907e9fd152e785eb80ae8a2b7a06cc40ebaaad6f3a3d2dd479b5625");
    G1Point p = G1Point(x, y);
    if (checkG1Poin(p)) {
      System.out.println("1");
    } else {
      System.out.println("2");
    }

    byte[] x1 = ByteArray.fromHexString("3af932f1b5784e512b18a3bef26485958789f56bb75de275952391f8c90b0c2a");
    byte[] x2 = ByteArray.fromHexString("1cb607c805373d08f94567b71e8f3736464af688d722cd2b560c4630cf701c26");
    byte[] y1 = ByteArray.fromHexString("00f66b4bc25f8adf9b841161e7d25b7c4a76661f0fdf09fb86510585a562ca12");
    byte[] y2 = ByteArray.fromHexString("9c5b1866b8bb84981970d3273fea70f6643a1faa8e73fe376a8e818bc287431e");
    ZksnarkUtils.sort(x1);
    ZksnarkUtils.sort(x2);
    ZksnarkUtils.sort(y1);
    ZksnarkUtils.sort(y2);
    G2Point g2 = new G2Point(x2, x1, y2, y1);
    if (checkG2Poin(g2)) {
      System.out.println("1");
    } else {
      System.out.println("2");
    }
  }

  public static String bytesToHex(byte[] bytes) {
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < bytes.length; i++) {
      int c =  bytes[i] & 0xFF;
      String hex = Integer.toHexString(c);
      sb.append(" ");
      if(hex.length() < 2){
        sb.append(0);
      }
      sb.append(hex);
    }
    return sb.toString();
  }

  public static G1Point readG1( int startPos, final byte[] filecontent ) {
    byte[] cx = new byte[32];
    byte[] cy = new byte[32];
    startPos += 1;
    System.arraycopy(filecontent, startPos, cx, 0, cx.length);
    startPos += 32;
    System.arraycopy(filecontent, startPos, cy, 0, cy.length);
    startPos += 32;

    ZksnarkUtils.sort(cx);
    ZksnarkUtils.sort(cy);

    System.out.println((new BigInteger(cx)).toString(10));
    System.out.println((new BigInteger(cy)).toString(10));
    System.out.println("");

    return new G1Point(cx, cy);
  }

  public static G2Point readG2( int startPos, final byte[] filecontent  ) {
    byte[] cx0 = new byte[32];
    byte[] cx1 = new byte[32];
    byte[] cy0 = new byte[32];
    byte[] cy1 = new byte[32];
    startPos += 1;
    System.arraycopy(filecontent, startPos, cx0, 0, cx0.length);
    startPos += 32;
    System.arraycopy(filecontent, startPos, cx1, 0, cx1.length);
    startPos += 32;
    System.arraycopy(filecontent, startPos, cy0, 0, cy0.length);
    startPos += 32;
    System.arraycopy(filecontent, startPos, cy1, 0, cy1.length);
    startPos += 32;

    ZksnarkUtils.sort(cx0);
    ZksnarkUtils.sort(cx1);
    ZksnarkUtils.sort(cy0);
    ZksnarkUtils.sort(cy1);

    System.out.println((new BigInteger(cx1)).toString(10));
    System.out.println((new BigInteger(cx0)).toString(10));
    System.out.println((new BigInteger(cy1)).toString(10));
    System.out.println((new BigInteger(cy0)).toString(10));
    System.out.println("");

    return new G2Point(cx1, cx0, cy1, cy0);
  }


    public static G1Point readG1Raw( int startPos, final byte[] filecontent ) throws Exception {
        byte[] cx = new byte[32];
        byte[] cy = new byte[32];
        startPos += 1;
        System.arraycopy(filecontent, startPos, cx, 0, cx.length);
        startPos += 32;
        System.arraycopy(filecontent, startPos, cy, 0, cy.length);
        startPos += 32;

        ZksnarkUtils.sort(cx);
        ZksnarkUtils.sort(cy);

//        System.out.println((new BigInteger(cx)).toString(10));
//        System.out.println((new BigInteger(cy)).toString(10));
//        System.out.println("");

        BigInteger x = MulReduce.mul_reduce(new BigInteger(cx), BigInteger.ONE);
        BigInteger y = MulReduce.mul_reduce(new BigInteger(cy), BigInteger.ONE);

        System.out.println(x.toString(10));
        System.out.println(y.toString(10));
        System.out.println("");

        return new G1Point(x, y);
    }

    public static G2Point readG2Raw( int startPos, final byte[] filecontent  ) throws Exception {
        byte[] cx0 = new byte[32];
        byte[] cx1 = new byte[32];
        byte[] cy0 = new byte[32];
        byte[] cy1 = new byte[32];
        startPos += 1;
        System.arraycopy(filecontent, startPos, cx0, 0, cx0.length);
        startPos += 32;
        System.arraycopy(filecontent, startPos, cx1, 0, cx1.length);
        startPos += 32;
        System.arraycopy(filecontent, startPos, cy0, 0, cy0.length);
        startPos += 32;
        System.arraycopy(filecontent, startPos, cy1, 0, cy1.length);
        startPos += 32;

        ZksnarkUtils.sort(cx0);
        ZksnarkUtils.sort(cx1);
        ZksnarkUtils.sort(cy0);
        ZksnarkUtils.sort(cy1);

//        System.out.println((new BigInteger(cx1)).toString(10));
//        System.out.println((new BigInteger(cx0)).toString(10));
//        System.out.println((new BigInteger(cy1)).toString(10));
//        System.out.println((new BigInteger(cy0)).toString(10));
//        System.out.println("");

        BigInteger x1 = MulReduce.mul_reduce(new BigInteger(cx1), BigInteger.ONE);
        BigInteger x0 = MulReduce.mul_reduce(new BigInteger(cx0), BigInteger.ONE);
        BigInteger y1 = MulReduce.mul_reduce(new BigInteger(cy1), BigInteger.ONE);
        BigInteger y0 = MulReduce.mul_reduce(new BigInteger(cy0), BigInteger.ONE);

        System.out.println(x1.toString(10));
        System.out.println(x0.toString(10));
        System.out.println(y1.toString(10));
        System.out.println(y0.toString(10));
        System.out.println("");

        return new G2Point(x1, x0, y1, y0);
    }


  public static void main(String[] args) throws Exception {
      initVkFromFileRaw();
  }
}
