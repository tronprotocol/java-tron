package org.tron.common.crypto.zksnark;


import java.math.BigInteger;

public class MulReduce {
    private static final BigInteger INV = longToUnsignedLong((new BigInteger("-8659850874718887031")).longValue());
    private static final BigInteger MODULUS = new BigInteger(
            "21888242871839275222246405745257275088696311157297823662689037894645226208583");


    static class BigIntegerResult {
        public BigInteger  resultValue;
        public BigInteger  carryOut;
    }

    /*
    public static BigInteger mul_reduce(final  BigInteger value, final BigInteger other) {

        System.out.println("1this:" + value.toString(16));
        System.out.println("2other:" + other.toString(16));

        BigInteger result = value.multiply(other);

        System.out.println("3res:" + result.toString(16));
        for (int i=0; i< 4; i++) {
            System.out.println("31res[i]:" + bigIntegerToUnsignedLong(result, i).toString(16));
        }

        System.out.println("4inv:" + INV.toString(16));

        for (int i = 0; i < 4; i++) {
            long k =  INV.multiply( bigIntegerToUnsignedLong(result, i) ).longValue();
            BigInteger rp = getLongArrayFromBigInteger(result, i, i+4);

            System.out.println("5K:" + longToUnsignedLong(k).toString(16) + "  res[i]:" +
                    bigIntegerToUnsignedLong(result, i).toString(16));
            System.out.println("6modulus:" + MODULUS.toString(16));
            for(int j=0; j<4;j++) {
                System.out.println("6-1modulus:" + bigIntegerToUnsignedLong(MODULUS, j).toString(16));
            }
            System.out.println("7res+i:" + rp.toString(16));


            // mpn_addmul_1
            BigIntegerResult addMulResult = mpnAddMul_1(rp, MODULUS, 4, k);

            // 将rp合并到result中
            BigInteger temp = getLongArrayFromBigInteger(addMulResult.resultValue, 0, 4);
            //result = result.subtract(rp).add(temp);
            result = result.subtract(rp.shiftLeft(64*i)).add(temp.shiftLeft(64*i));
            // 当 i= 1时，该值已经不正确了


            System.out.println("7res:" + result.toString(16));
            System.out.println("8carryout:" + addMulResult.carryOut.toString(16));
            System.out.println("9res+i:" + getLongArrayFromBigInteger(addMulResult.resultValue, 0
                    , 4).toString(16));
            System.out.println("10res+n+i:" + getLongArrayFromBigInteger(result
                    , 4+i, 8).toString(16));

            BigInteger addOther = getLongArrayFromBigInteger(result, 4+i, 8);

            //private static BigIntegerResult mpnAdd_1( BigInteger s1p, int n, BigInteger s2limb ) {

            BigIntegerResult addResult = mpnAdd_1(addOther,4-i,  addMulResult.carryOut);

            System.out.println("11res+n+i:" + addResult.resultValue.toString(16));
            System.out.println("12carryout:" + addResult.carryOut.toString(16));

            // 这里必须等于0
            assert (addMulResult.carryOut.equals(BigInteger.ZERO));


            // 合并result
            result = result.add( addMulResult.carryOut.shiftLeft(64*(4+i)));

            System.out.println("12res:" + result.toString(16));


            System.out.println("\n\n");

        }
        System.out.println("result:" + result.shiftRight(64*4).toString(16));

        BigInteger finalResult = result.shiftRight(64*4);

        System.out.println("final Result:" + finalResult.toString(16));
        System.out.println("module _resu:" + MODULUS.toString(16));
        int x = finalResult.compareTo(MODULUS);
        if (finalResult.compareTo(MODULUS) >= 0) {
            System.out.println("hahha->>>>>>>");
            finalResult = finalResult.subtract(MODULUS);
        }

        return finalResult;
    }
    */

    public static BigInteger mul_reduce(final  BigInteger value, final BigInteger other) throws Exception {

        BigInteger result = value.multiply(other);
        for (int i = 0; i < 4; i++) {
            long k =  INV.multiply( bigIntegerToUnsignedLong(result, i) ).longValue();
            BigInteger rp = getLongArrayFromBigInteger(result, i, i+4);
            // calculate res = res + k * mod * b^i
            BigIntegerResult addMulResult = mpnAddMul_1(rp, MODULUS, 4, k);

            BigInteger temp = getLongArrayFromBigInteger(addMulResult.resultValue, 0, 4);
            result = result.subtract(rp.shiftLeft(64*i)).add(temp.shiftLeft(64*i));

            BigInteger addOther = getLongArrayFromBigInteger(result, 4+i, 8);

            BigIntegerResult addResult = mpnAdd_1(addOther,4-i,  addMulResult.carryOut);
            if ( !addResult.carryOut.equals(BigInteger.ZERO) ) {
                throw  new Exception("Carryout must be zero");
            }

            result = result.add( addMulResult.carryOut.shiftLeft(64*(4+i)));
        }

        BigInteger finalResult = result.shiftRight(64*4);
        if (finalResult.compareTo(MODULUS) >= 0) {
            finalResult = finalResult.subtract(MODULUS);
            if (finalResult.compareTo(BigInteger.ZERO) < 0 ) {
                throw new Exception("Result must be positive value");
            }
        }

        return finalResult;
    }


    private static BigIntegerResult mpnAddMul_1(BigInteger rp, BigInteger s1p, int n, long s2limb) {
        BigIntegerResult  bigRet = new BigIntegerResult();
        bigRet.resultValue = rp;

        BigInteger bigIntegerLong = longToUnsignedLong(s2limb);
        for(int i=0; i<n; i++) {
            BigInteger s1pi = bigIntegerToUnsignedLong(s1p, i);
            s1pi = s1pi.multiply(bigIntegerLong);
            BigInteger rpi = bigIntegerToUnsignedLong(bigRet.resultValue, i).add(s1pi);

            BigInteger addResultPart = s1pi.shiftLeft( 64*i );
            bigRet.resultValue = bigRet.resultValue.add(addResultPart);
            bigRet.carryOut = bigIntegerToUnsignedLong(rpi, 1);
        }

        return bigRet;
    }


    private static BigIntegerResult mpnAdd_1( BigInteger s1p, int n, BigInteger s2limb ) {
        BigIntegerResult  bigRet = new BigIntegerResult();
        bigRet.resultValue = s1p.add(s2limb);
        bigRet.carryOut = longToUnsignedLong( bigRet.resultValue.shiftRight(64*n).longValue() );

        return bigRet;
    }

    private static BigInteger bigIntegerToUnsignedLong(final BigInteger bigInteger,
                                                      final int index ){
        long value = bigInteger.shiftRight( index*64 ).longValue();
        return longToUnsignedLong( value );
    }

    private static BigInteger longToUnsignedLong(long value ) {
        if (value >= 0) {
            return BigInteger.valueOf(value);
        } else {
            long lowValue = value & 0x7fffffffffffffffL;
            return BigInteger.valueOf(lowValue).add(BigInteger.valueOf(Long.MAX_VALUE)).add(BigInteger.valueOf(1));
        }
    }

    private static BigInteger getLongArrayFromBigInteger(final BigInteger value, int start,
                                                        int stop ) {
        BigInteger result = BigInteger.ZERO;
        for (int i=start; i<stop; i++) {
            result = result.add(bigIntegerToUnsignedLong(value, i).shiftLeft(64*i));
        }
        return result.shiftRight((start-0)*64);
    }


    public static void main(String[] args){
        try {
//           BigInteger x = new BigInteger(
//                "10855362814118872746921150895317950015833966554628950873068002980441989956485");

            BigInteger x = new BigInteger(
                    "257F32472B344E825B4AABF1CC7FE91CD14BE10E9E386F8C6AA2467940247C2D",
                    16);

            System.out.println("\n\nfinal result:" + mul_reduce(x, BigInteger.ONE).toString(16));

//        BigInteger a = new BigInteger(
//                "216D0B17F4E44A58C49833D53BB808553FE3AB1E35C59E31BB8E645AE216DA7",16);
//        BigInteger b = new BigInteger(
//                "5",16);
//        System.out.println("\n\nfinal result:" + mul_reduce(a, b).toString());

        } catch ( Exception e) {
            System.out.println(e.getMessage());
        }

    }



}
