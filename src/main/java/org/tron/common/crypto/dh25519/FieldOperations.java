package org.tron.common.crypto.dh25519;

public class FieldOperations {

  public static void fe_0(FieldElement h)
  {
    h.x0=0;
    h.x1=0;
    h.x2=0;
    h.x3=0;
    h.x4=0;
    h.x5=0;
    h.x6=0;
    h.x7=0;
    h.x8=0;
    h.x9=0;
  }

  public static void fe_1(FieldElement h)
  {
    h.x0=1;
    h.x1=0;
    h.x2=0;
    h.x3=0;
    h.x4=0;
    h.x5=0;
    h.x6=0;
    h.x7=0;
    h.x8=0;
    h.x9=0;
  }

  /*
  h = f + g
  Can overlap h with f or g.

  Preconditions:
     |f| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
     |g| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.

  Postconditions:
     |h| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.
  */
  //void fe_add(fe h,const fe f,const fe g)
  static void fe_add(FieldElement h, FieldElement f, FieldElement g)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int g0 = g.x0;
    int g1 = g.x1;
    int g2 = g.x2;
    int g3 = g.x3;
    int g4 = g.x4;
    int g5 = g.x5;
    int g6 = g.x6;
    int g7 = g.x7;
    int g8 = g.x8;
    int g9 = g.x9;
    int h0 = f0 + g0;
    int h1 = f1 + g1;
    int h2 = f2 + g2;
    int h3 = f3 + g3;
    int h4 = f4 + g4;
    int h5 = f5 + g5;
    int h6 = f6 + g6;
    int h7 = f7 + g7;
    int h8 = f8 + g8;
    int h9 = f9 + g9;
    h.x0 = h0;
    h.x1 = h1;
    h.x2 = h2;
    h.x3 = h3;
    h.x4 = h4;
    h.x5 = h5;
    h.x6 = h6;
    h.x7 = h7;
    h.x8 = h8;
    h.x9 = h9;
  }

	/*
	Replace (f,g) with (g,g) if b == 1;
	replace (f,g) with (f,g) if b == 0.

	Preconditions: b in {0,1}.
	*/

  //void fe_cmov(fe f,const fe g,unsigned int b)
  static void fe_cmov(FieldElement f,  FieldElement g, int b)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int g0 = g.x0;
    int g1 = g.x1;
    int g2 = g.x2;
    int g3 = g.x3;
    int g4 = g.x4;
    int g5 = g.x5;
    int g6 = g.x6;
    int g7 = g.x7;
    int g8 = g.x8;
    int g9 = g.x9;
    int x0 = f0 ^ g0;
    int x1 = f1 ^ g1;
    int x2 = f2 ^ g2;
    int x3 = f3 ^ g3;
    int x4 = f4 ^ g4;
    int x5 = f5 ^ g5;
    int x6 = f6 ^ g6;
    int x7 = f7 ^ g7;
    int x8 = f8 ^ g8;
    int x9 = f9 ^ g9;
    b = -b;
    x0 &= b;
    x1 &= b;
    x2 &= b;
    x3 &= b;
    x4 &= b;
    x5 &= b;
    x6 &= b;
    x7 &= b;
    x8 &= b;
    x9 &= b;
    f.x0 = f0 ^ x0;
    f.x1 = f1 ^ x1;
    f.x2 = f2 ^ x2;
    f.x3 = f3 ^ x3;
    f.x4 = f4 ^ x4;
    f.x5 = f5 ^ x5;
    f.x6 = f6 ^ x6;
    f.x7 = f7 ^ x7;
    f.x8 = f8 ^ x8;
    f.x9 = f9 ^ x9;
  }

  /*
  h = f
  */
  static void fe_copy(FieldElement h,FieldElement f)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    h.x0 = f0;
    h.x1 = f1;
    h.x2 = f2;
    h.x3 = f3;
    h.x4 = f4;
    h.x5 = f5;
    h.x6 = f6;
    h.x7 = f7;
    h.x8 = f8;
    h.x9 = f9;
  }


  /*
  Replace (f,g) with (g,f) if b == 1;
  replace (f,g) with (f,g) if b == 0.

  Preconditions: b in {0,1}.
  */
  public static void fe_cswap(FieldElement f, FieldElement g, int b)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int g0 = g.x0;
    int g1 = g.x1;
    int g2 = g.x2;
    int g3 = g.x3;
    int g4 = g.x4;
    int g5 = g.x5;
    int g6 = g.x6;
    int g7 = g.x7;
    int g8 = g.x8;
    int g9 = g.x9;
    int x0 = f0 ^ g0;
    int x1 = f1 ^ g1;
    int x2 = f2 ^ g2;
    int x3 = f3 ^ g3;
    int x4 = f4 ^ g4;
    int x5 = f5 ^ g5;
    int x6 = f6 ^ g6;
    int x7 = f7 ^ g7;
    int x8 = f8 ^ g8;
    int x9 = f9 ^ g9;
    int negb = -b;
    x0 &= negb;
    x1 &= negb;
    x2 &= negb;
    x3 &= negb;
    x4 &= negb;
    x5 &= negb;
    x6 &= negb;
    x7 &= negb;
    x8 &= negb;
    x9 &= negb;
    f.x0 = f0 ^ x0;
    f.x1 = f1 ^ x1;
    f.x2 = f2 ^ x2;
    f.x3 = f3 ^ x3;
    f.x4 = f4 ^ x4;
    f.x5 = f5 ^ x5;
    f.x6 = f6 ^ x6;
    f.x7 = f7 ^ x7;
    f.x8 = f8 ^ x8;
    f.x9 = f9 ^ x9;
    g.x0 = g0 ^ x0;
    g.x1 = g1 ^ x1;
    g.x2 = g2 ^ x2;
    g.x3 = g3 ^ x3;
    g.x4 = g4 ^ x4;
    g.x5 = g5 ^ x5;
    g.x6 = g6 ^ x6;
    g.x7 = g7 ^ x7;
    g.x8 = g8 ^ x8;
    g.x9 = g9 ^ x9;
  }

  //	Ignores top bit of h.
  static void fe_frombytes( FieldElement h, byte[] data, int offset)
  {
    long h0 = ByteIntegerConverter.load_4(data, offset);
    long h1 = ByteIntegerConverter.load_3(data, offset + 4) << 6;
    long h2 = ByteIntegerConverter.load_3(data, offset + 7) << 5;
    long h3 = ByteIntegerConverter.load_3(data, offset + 10) << 3;
    long h4 = ByteIntegerConverter.load_3(data, offset + 13) << 2;
    long h5 = ByteIntegerConverter.load_4(data, offset + 16);
    long h6 = ByteIntegerConverter.load_3(data, offset + 20) << 7;
    long h7 = ByteIntegerConverter.load_3(data, offset + 23) << 5;
    long h8 = ByteIntegerConverter.load_3(data, offset + 26) << 4;
    long h9 = (ByteIntegerConverter.load_3(data, offset + 29) & 8388607) << 2;// mask top bit
    long carry0;
    long carry1;
    long carry2;
    long carry3;
    long carry4;
    long carry5;
    long carry6;
    long carry7;
    long carry8;
    long carry9;

    carry9 = (h9 + (long)(1 << 24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;
    carry1 = (h1 + (long)(1 << 24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
    carry3 = (h3 + (long)(1 << 24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
    carry5 = (h5 + (long)(1 << 24)) >> 25; h6 += carry5; h5 -= carry5 << 25;
    carry7 = (h7 + (long)(1 << 24)) >> 25; h8 += carry7; h7 -= carry7 << 25;

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
    carry2 = (h2 + (long)(1 << 25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
    carry6 = (h6 + (long)(1 << 25)) >> 26; h7 += carry6; h6 -= carry6 << 26;
    carry8 = (h8 + (long)(1 << 25)) >> 26; h9 += carry8; h8 -= carry8 << 26;

    h.x0 = (int)h0;
    h.x1 = (int)h1;
    h.x2 = (int)h2;
    h.x3 = (int)h3;
    h.x4 = (int)h4;
    h.x5 = (int)h5;
    h.x6 = (int)h6;
    h.x7 = (int)h7;
    h.x8 = (int)h8;
    h.x9 = (int)h9;
  }

  // does NOT ignore top bit
  static void fe_frombytes2(FieldElement h, byte[] data, int offset)
  {
    long h0 = ByteIntegerConverter.load_4(data, offset);
    long h1 = ByteIntegerConverter.load_3(data, offset + 4) << 6;
    long h2 = ByteIntegerConverter.load_3(data, offset + 7) << 5;
    long h3 = ByteIntegerConverter.load_3(data, offset + 10) << 3;
    long h4 = ByteIntegerConverter.load_3(data, offset + 13) << 2;
    long h5 = ByteIntegerConverter.load_4(data, offset + 16);
    long h6 = ByteIntegerConverter.load_3(data, offset + 20) << 7;
    long h7 = ByteIntegerConverter.load_3(data, offset + 23) << 5;
    long h8 = ByteIntegerConverter.load_3(data, offset + 26) << 4;
    long h9 = ByteIntegerConverter.load_3(data, offset + 29) << 2;// keep top bit
    long carry0;
    long carry1;
    long carry2;
    long carry3;
    long carry4;
    long carry5;
    long carry6;
    long carry7;
    long carry8;
    long carry9;

    carry9 = (h9 + (long)(1 << 24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;
    carry1 = (h1 + (long)(1 << 24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
    carry3 = (h3 + (long)(1 << 24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
    carry5 = (h5 + (long)(1 << 24)) >> 25; h6 += carry5; h5 -= carry5 << 25;
    carry7 = (h7 + (long)(1 << 24)) >> 25; h8 += carry7; h7 -= carry7 << 25;

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
    carry2 = (h2 + (long)(1 << 25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
    carry6 = (h6 + (long)(1 << 25)) >> 26; h7 += carry6; h6 -= carry6 << 26;
    carry8 = (h8 + (long)(1 << 25)) >> 26; h9 += carry8; h8 -= carry8 << 26;

    h.x0 = (int)h0;
    h.x1 = (int)h1;
    h.x2 = (int)h2;
    h.x3 = (int)h3;
    h.x4 = (int)h4;
    h.x5 = (int)h5;
    h.x6 = (int)h6;
    h.x7 = (int)h7;
    h.x8 = (int)h8;
    h.x9 = (int)h9;
  }

  static void fe_invert(FieldElement result, FieldElement z)
  {
    FieldElement t0=new FieldElement();
    FieldElement t1=new FieldElement();
    FieldElement t2=new FieldElement();
    FieldElement t3=new FieldElement();
    int i;

    /* qhasm: fe z1 */

    /* qhasm: fe z2 */

    /* qhasm: fe z8 */

    /* qhasm: fe z9 */

    /* qhasm: fe z11 */

    /* qhasm: fe z22 */

    /* qhasm: fe z_5_0 */

    /* qhasm: fe z_10_5 */

    /* qhasm: fe z_10_0 */

    /* qhasm: fe z_20_10 */

    /* qhasm: fe z_20_0 */

    /* qhasm: fe z_40_20 */

    /* qhasm: fe z_40_0 */

    /* qhasm: fe z_50_10 */

    /* qhasm: fe z_50_0 */

    /* qhasm: fe z_100_50 */

    /* qhasm: fe z_100_0 */

    /* qhasm: fe z_200_100 */

    /* qhasm: fe z_200_0 */

    /* qhasm: fe z_250_50 */

    /* qhasm: fe z_250_0 */

    /* qhasm: fe z_255_5 */

    /* qhasm: fe z_255_21 */

    /* qhasm: enter pow225521 */

    /* qhasm: z2 = z1^2^1 */
    /* asm 1: fe_sq(>z2=fe#1,<z1=fe#11); for (i = 1;i < 1;++i) fe_sq(>z2=fe#1,>z2=fe#1); */
    /* asm 2: fe_sq(>z2=t0,<z1=z); for (i = 1;i < 1;++i) fe_sq(>z2=t0,>z2=t0); */
    fe_sq(t0, z); //for (i = 1; i < 1; ++i) fe_sq(t0, t0);

    /* qhasm: z8 = z2^2^2 */
    /* asm 1: fe_sq(>z8=fe#2,<z2=fe#1); for (i = 1;i < 2;++i) fe_sq(>z8=fe#2,>z8=fe#2); */
    /* asm 2: fe_sq(>z8=t1,<z2=t0); for (i = 1;i < 2;++i) fe_sq(>z8=t1,>z8=t1); */
    fe_sq(t1, t0); for (i = 1; i < 2; ++i) fe_sq(t1, t1);

    /* qhasm: z9 = z1*z8 */
    /* asm 1: fe_mul(>z9=fe#2,<z1=fe#11,<z8=fe#2); */
    /* asm 2: fe_mul(>z9=t1,<z1=z,<z8=t1); */
    fe_mul(t1, z, t1);

    /* qhasm: z11 = z2*z9 */
    /* asm 1: fe_mul(>z11=fe#1,<z2=fe#1,<z9=fe#2); */
    /* asm 2: fe_mul(>z11=t0,<z2=t0,<z9=t1); */
    fe_mul(t0, t0, t1);

    /* qhasm: z22 = z11^2^1 */
    /* asm 1: fe_sq(>z22=fe#3,<z11=fe#1); for (i = 1;i < 1;++i) fe_sq(>z22=fe#3,>z22=fe#3); */
    /* asm 2: fe_sq(>z22=t2,<z11=t0); for (i = 1;i < 1;++i) fe_sq(>z22=t2,>z22=t2); */
    fe_sq(t2, t0); //for (i = 1; i < 1; ++i) fe_sq(t2, t2);

    /* qhasm: z_5_0 = z9*z22 */
    /* asm 1: fe_mul(>z_5_0=fe#2,<z9=fe#2,<z22=fe#3); */
    /* asm 2: fe_mul(>z_5_0=t1,<z9=t1,<z22=t2); */
    fe_mul(t1, t1, t2);

    /* qhasm: z_10_5 = z_5_0^2^5 */
    /* asm 1: fe_sq(>z_10_5=fe#3,<z_5_0=fe#2); for (i = 1;i < 5;++i) fe_sq(>z_10_5=fe#3,>z_10_5=fe#3); */
    /* asm 2: fe_sq(>z_10_5=t2,<z_5_0=t1); for (i = 1;i < 5;++i) fe_sq(>z_10_5=t2,>z_10_5=t2); */
    fe_sq(t2, t1); for (i = 1; i < 5; ++i) fe_sq(t2, t2);

    /* qhasm: z_10_0 = z_10_5*z_5_0 */
    /* asm 1: fe_mul(>z_10_0=fe#2,<z_10_5=fe#3,<z_5_0=fe#2); */
    /* asm 2: fe_mul(>z_10_0=t1,<z_10_5=t2,<z_5_0=t1); */
    fe_mul(t1, t2, t1);

    /* qhasm: z_20_10 = z_10_0^2^10 */
    /* asm 1: fe_sq(>z_20_10=fe#3,<z_10_0=fe#2); for (i = 1;i < 10;++i) fe_sq(>z_20_10=fe#3,>z_20_10=fe#3); */
    /* asm 2: fe_sq(>z_20_10=t2,<z_10_0=t1); for (i = 1;i < 10;++i) fe_sq(>z_20_10=t2,>z_20_10=t2); */
    fe_sq(t2, t1); for (i = 1; i < 10; ++i) fe_sq(t2, t2);

    /* qhasm: z_20_0 = z_20_10*z_10_0 */
    /* asm 1: fe_mul(>z_20_0=fe#3,<z_20_10=fe#3,<z_10_0=fe#2); */
    /* asm 2: fe_mul(>z_20_0=t2,<z_20_10=t2,<z_10_0=t1); */
    fe_mul(t2, t2, t1);

    /* qhasm: z_40_20 = z_20_0^2^20 */
    /* asm 1: fe_sq(>z_40_20=fe#4,<z_20_0=fe#3); for (i = 1;i < 20;++i) fe_sq(>z_40_20=fe#4,>z_40_20=fe#4); */
    /* asm 2: fe_sq(>z_40_20=t3,<z_20_0=t2); for (i = 1;i < 20;++i) fe_sq(>z_40_20=t3,>z_40_20=t3); */
    fe_sq(t3, t2); for (i = 1; i < 20; ++i) fe_sq(t3, t3);

    /* qhasm: z_40_0 = z_40_20*z_20_0 */
    /* asm 1: fe_mul(>z_40_0=fe#3,<z_40_20=fe#4,<z_20_0=fe#3); */
    /* asm 2: fe_mul(>z_40_0=t2,<z_40_20=t3,<z_20_0=t2); */
    fe_mul(t2, t3, t2);

    /* qhasm: z_50_10 = z_40_0^2^10 */
    /* asm 1: fe_sq(>z_50_10=fe#3,<z_40_0=fe#3); for (i = 1;i < 10;++i) fe_sq(>z_50_10=fe#3,>z_50_10=fe#3); */
    /* asm 2: fe_sq(>z_50_10=t2,<z_40_0=t2); for (i = 1;i < 10;++i) fe_sq(>z_50_10=t2,>z_50_10=t2); */
    fe_sq(t2, t2); for (i = 1; i < 10; ++i) fe_sq(t2, t2);

    /* qhasm: z_50_0 = z_50_10*z_10_0 */
    /* asm 1: fe_mul(>z_50_0=fe#2,<z_50_10=fe#3,<z_10_0=fe#2); */
    /* asm 2: fe_mul(>z_50_0=t1,<z_50_10=t2,<z_10_0=t1); */
    fe_mul(t1, t2, t1);

    /* qhasm: z_100_50 = z_50_0^2^50 */
    /* asm 1: fe_sq(>z_100_50=fe#3,<z_50_0=fe#2); for (i = 1;i < 50;++i) fe_sq(>z_100_50=fe#3,>z_100_50=fe#3); */
    /* asm 2: fe_sq(>z_100_50=t2,<z_50_0=t1); for (i = 1;i < 50;++i) fe_sq(>z_100_50=t2,>z_100_50=t2); */
    fe_sq(t2, t1); for (i = 1; i < 50; ++i) fe_sq(t2, t2);

    /* qhasm: z_100_0 = z_100_50*z_50_0 */
    /* asm 1: fe_mul(>z_100_0=fe#3,<z_100_50=fe#3,<z_50_0=fe#2); */
    /* asm 2: fe_mul(>z_100_0=t2,<z_100_50=t2,<z_50_0=t1); */
    fe_mul(t2, t2, t1);

    /* qhasm: z_200_100 = z_100_0^2^100 */
    /* asm 1: fe_sq(>z_200_100=fe#4,<z_100_0=fe#3); for (i = 1;i < 100;++i) fe_sq(>z_200_100=fe#4,>z_200_100=fe#4); */
    /* asm 2: fe_sq(>z_200_100=t3,<z_100_0=t2); for (i = 1;i < 100;++i) fe_sq(>z_200_100=t3,>z_200_100=t3); */
    fe_sq(t3, t2); for (i = 1; i < 100; ++i) fe_sq(t3, t3);

    /* qhasm: z_200_0 = z_200_100*z_100_0 */
    /* asm 1: fe_mul(>z_200_0=fe#3,<z_200_100=fe#4,<z_100_0=fe#3); */
    /* asm 2: fe_mul(>z_200_0=t2,<z_200_100=t3,<z_100_0=t2); */
    fe_mul(t2, t3, t2);

    /* qhasm: z_250_50 = z_200_0^2^50 */
    /* asm 1: fe_sq(>z_250_50=fe#3,<z_200_0=fe#3); for (i = 1;i < 50;++i) fe_sq(>z_250_50=fe#3,>z_250_50=fe#3); */
    /* asm 2: fe_sq(>z_250_50=t2,<z_200_0=t2); for (i = 1;i < 50;++i) fe_sq(>z_250_50=t2,>z_250_50=t2); */
    fe_sq(t2, t2); for (i = 1; i < 50; ++i) fe_sq(t2, t2);

    /* qhasm: z_250_0 = z_250_50*z_50_0 */
    /* asm 1: fe_mul(>z_250_0=fe#2,<z_250_50=fe#3,<z_50_0=fe#2); */
    /* asm 2: fe_mul(>z_250_0=t1,<z_250_50=t2,<z_50_0=t1); */
    fe_mul(t1, t2, t1);

    /* qhasm: z_255_5 = z_250_0^2^5 */
    /* asm 1: fe_sq(>z_255_5=fe#2,<z_250_0=fe#2); for (i = 1;i < 5;++i) fe_sq(>z_255_5=fe#2,>z_255_5=fe#2); */
    /* asm 2: fe_sq(>z_255_5=t1,<z_250_0=t1); for (i = 1;i < 5;++i) fe_sq(>z_255_5=t1,>z_255_5=t1); */
    fe_sq(t1, t1); for (i = 1; i < 5; ++i) fe_sq(t1, t1);

    /* qhasm: z_255_21 = z_255_5*z11 */
    /* asm 1: fe_mul(>z_255_21=fe#12,<z_255_5=fe#2,<z11=fe#1); */
    /* asm 2: fe_mul(>z_255_21=out,<z_255_5=t1,<z11=t0); */
    fe_mul(result, t1, t0);

    /* qhasm: return */


    return;
  }

  /*
  return 1 if f is in {1,3,5,...,q-2}
  return 0 if f is in {0,2,4,...,q-1}

  Preconditions:
  |f| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.
  */
  //int fe_isnegative(const fe f)
  public static int fe_isnegative(FieldElement f)
  {
    FieldElement fr=new FieldElement();
    fe_reduce(fr, f);
    return fr.x0 & 1;
  }

  /*
  return 1 if f == 0
  return 0 if f != 0

  Preconditions:
     |f| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.
  */
  // Todo: Discuss this with upstream
  // Above comment is from the original code. But I believe the original code returned
  //   0 if f == 0
  //  -1 if f != 0
  // This code actually returns 0 if f==0 and 1 if f != 0
  static int fe_isnonzero(FieldElement f)
  {
    FieldElement fr=new FieldElement();
    fe_reduce(fr, f);
    int differentBits = 0;
    differentBits |= fr.x0;
    differentBits |= fr.x1;
    differentBits |= fr.x2;
    differentBits |= fr.x3;
    differentBits |= fr.x4;
    differentBits |= fr.x5;
    differentBits |= fr.x6;
    differentBits |= fr.x7;
    differentBits |= fr.x8;
    differentBits |= fr.x9;
    return ((differentBits - 1) >>> 31) ^ 1;
  }

	/*
	h = f * g
	Can overlap h with f or g.

	Preconditions:
	   |f| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.
	   |g| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.

	Postconditions:
	   |h| bounded by 1.01*2^25,1.01*2^24,1.01*2^25,1.01*2^24,etc.
	*/

	/*
	Notes on implementation strategy:

	Using schoolbook multiplication.
	Karatsuba would save a little in some cost models.

	Most multiplications by 2 and 19 are 32-bit precomputations;
	cheaper than 64-bit postcomputations.

	There is one remaining multiplication by 19 in the carry chain;
	one *19 precomputation can be merged into this,
	but the resulting data flow is considerably less clean.

	There are 12 carries below.
	10 of them are 2-way parallelizable and vectorizable.
	Can get away with 11 carries, but then data flow is much deeper.

	With tighter constraints on inputs can squeeze carries into int.
	*/

  static void fe_mul(FieldElement h, FieldElement f, FieldElement g)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int g0 = g.x0;
    int g1 = g.x1;
    int g2 = g.x2;
    int g3 = g.x3;
    int g4 = g.x4;
    int g5 = g.x5;
    int g6 = g.x6;
    int g7 = g.x7;
    int g8 = g.x8;
    int g9 = g.x9;
    int g1_19 = 19 * g1; /* 1.959375*2^29 */
    int g2_19 = 19 * g2; /* 1.959375*2^30; still ok */
    int g3_19 = 19 * g3;
    int g4_19 = 19 * g4;
    int g5_19 = 19 * g5;
    int g6_19 = 19 * g6;
    int g7_19 = 19 * g7;
    int g8_19 = 19 * g8;
    int g9_19 = 19 * g9;
    int f1_2 = 2 * f1;
    int f3_2 = 2 * f3;
    int f5_2 = 2 * f5;
    int f7_2 = 2 * f7;
    int f9_2 = 2 * f9;
    long f0g0 = f0 * (long)g0;
    long f0g1 = f0 * (long)g1;
    long f0g2 = f0 * (long)g2;
    long f0g3 = f0 * (long)g3;
    long f0g4 = f0 * (long)g4;
    long f0g5 = f0 * (long)g5;
    long f0g6 = f0 * (long)g6;
    long f0g7 = f0 * (long)g7;
    long f0g8 = f0 * (long)g8;
    long f0g9 = f0 * (long)g9;
    long f1g0 = f1 * (long)g0;
    long f1g1_2 = f1_2 * (long)g1;
    long f1g2 = f1 * (long)g2;
    long f1g3_2 = f1_2 * (long)g3;
    long f1g4 = f1 * (long)g4;
    long f1g5_2 = f1_2 * (long)g5;
    long f1g6 = f1 * (long)g6;
    long f1g7_2 = f1_2 * (long)g7;
    long f1g8 = f1 * (long)g8;
    long f1g9_38 = f1_2 * (long)g9_19;
    long f2g0 = f2 * (long)g0;
    long f2g1 = f2 * (long)g1;
    long f2g2 = f2 * (long)g2;
    long f2g3 = f2 * (long)g3;
    long f2g4 = f2 * (long)g4;
    long f2g5 = f2 * (long)g5;
    long f2g6 = f2 * (long)g6;
    long f2g7 = f2 * (long)g7;
    long f2g8_19 = f2 * (long)g8_19;
    long f2g9_19 = f2 * (long)g9_19;
    long f3g0 = f3 * (long)g0;
    long f3g1_2 = f3_2 * (long)g1;
    long f3g2 = f3 * (long)g2;
    long f3g3_2 = f3_2 * (long)g3;
    long f3g4 = f3 * (long)g4;
    long f3g5_2 = f3_2 * (long)g5;
    long f3g6 = f3 * (long)g6;
    long f3g7_38 = f3_2 * (long)g7_19;
    long f3g8_19 = f3 * (long)g8_19;
    long f3g9_38 = f3_2 * (long)g9_19;
    long f4g0 = f4 * (long)g0;
    long f4g1 = f4 * (long)g1;
    long f4g2 = f4 * (long)g2;
    long f4g3 = f4 * (long)g3;
    long f4g4 = f4 * (long)g4;
    long f4g5 = f4 * (long)g5;
    long f4g6_19 = f4 * (long)g6_19;
    long f4g7_19 = f4 * (long)g7_19;
    long f4g8_19 = f4 * (long)g8_19;
    long f4g9_19 = f4 * (long)g9_19;
    long f5g0 = f5 * (long)g0;
    long f5g1_2 = f5_2 * (long)g1;
    long f5g2 = f5 * (long)g2;
    long f5g3_2 = f5_2 * (long)g3;
    long f5g4 = f5 * (long)g4;
    long f5g5_38 = f5_2 * (long)g5_19;
    long f5g6_19 = f5 * (long)g6_19;
    long f5g7_38 = f5_2 * (long)g7_19;
    long f5g8_19 = f5 * (long)g8_19;
    long f5g9_38 = f5_2 * (long)g9_19;
    long f6g0 = f6 * (long)g0;
    long f6g1 = f6 * (long)g1;
    long f6g2 = f6 * (long)g2;
    long f6g3 = f6 * (long)g3;
    long f6g4_19 = f6 * (long)g4_19;
    long f6g5_19 = f6 * (long)g5_19;
    long f6g6_19 = f6 * (long)g6_19;
    long f6g7_19 = f6 * (long)g7_19;
    long f6g8_19 = f6 * (long)g8_19;
    long f6g9_19 = f6 * (long)g9_19;
    long f7g0 = f7 * (long)g0;
    long f7g1_2 = f7_2 * (long)g1;
    long f7g2 = f7 * (long)g2;
    long f7g3_38 = f7_2 * (long)g3_19;
    long f7g4_19 = f7 * (long)g4_19;
    long f7g5_38 = f7_2 * (long)g5_19;
    long f7g6_19 = f7 * (long)g6_19;
    long f7g7_38 = f7_2 * (long)g7_19;
    long f7g8_19 = f7 * (long)g8_19;
    long f7g9_38 = f7_2 * (long)g9_19;
    long f8g0 = f8 * (long)g0;
    long f8g1 = f8 * (long)g1;
    long f8g2_19 = f8 * (long)g2_19;
    long f8g3_19 = f8 * (long)g3_19;
    long f8g4_19 = f8 * (long)g4_19;
    long f8g5_19 = f8 * (long)g5_19;
    long f8g6_19 = f8 * (long)g6_19;
    long f8g7_19 = f8 * (long)g7_19;
    long f8g8_19 = f8 * (long)g8_19;
    long f8g9_19 = f8 * (long)g9_19;
    long f9g0 = f9 * (long)g0;
    long f9g1_38 = f9_2 * (long)g1_19;
    long f9g2_19 = f9 * (long)g2_19;
    long f9g3_38 = f9_2 * (long)g3_19;
    long f9g4_19 = f9 * (long)g4_19;
    long f9g5_38 = f9_2 * (long)g5_19;
    long f9g6_19 = f9 * (long)g6_19;
    long f9g7_38 = f9_2 * (long)g7_19;
    long f9g8_19 = f9 * (long)g8_19;
    long f9g9_38 = f9_2 * (long)g9_19;
    long h0 = f0g0 + f1g9_38 + f2g8_19 + f3g7_38 + f4g6_19 + f5g5_38 + f6g4_19 + f7g3_38 + f8g2_19 + f9g1_38;
    long h1 = f0g1 + f1g0 + f2g9_19 + f3g8_19 + f4g7_19 + f5g6_19 + f6g5_19 + f7g4_19 + f8g3_19 + f9g2_19;
    long h2 = f0g2 + f1g1_2 + f2g0 + f3g9_38 + f4g8_19 + f5g7_38 + f6g6_19 + f7g5_38 + f8g4_19 + f9g3_38;
    long h3 = f0g3 + f1g2 + f2g1 + f3g0 + f4g9_19 + f5g8_19 + f6g7_19 + f7g6_19 + f8g5_19 + f9g4_19;
    long h4 = f0g4 + f1g3_2 + f2g2 + f3g1_2 + f4g0 + f5g9_38 + f6g8_19 + f7g7_38 + f8g6_19 + f9g5_38;
    long h5 = f0g5 + f1g4 + f2g3 + f3g2 + f4g1 + f5g0 + f6g9_19 + f7g8_19 + f8g7_19 + f9g6_19;
    long h6 = f0g6 + f1g5_2 + f2g4 + f3g3_2 + f4g2 + f5g1_2 + f6g0 + f7g9_38 + f8g8_19 + f9g7_38;
    long h7 = f0g7 + f1g6 + f2g5 + f3g4 + f4g3 + f5g2 + f6g1 + f7g0 + f8g9_19 + f9g8_19;
    long h8 = f0g8 + f1g7_2 + f2g6 + f3g5_2 + f4g4 + f5g3_2 + f6g2 + f7g1_2 + f8g0 + f9g9_38;
    long h9 = f0g9 + f1g8 + f2g7 + f3g6 + f4g5 + f5g4 + f6g3 + f7g2 + f8g1 + f9g0;
    long carry0;
    long carry1;
    long carry2;
    long carry3;
    long carry4;
    long carry5;
    long carry6;
    long carry7;
    long carry8;
    long carry9;

		/*
		|h0| <= (1.65*1.65*2^52*(1+19+19+19+19)+1.65*1.65*2^50*(38+38+38+38+38))
		  i.e. |h0| <= 1.4*2^60; narrower ranges for h2, h4, h6, h8
		|h1| <= (1.65*1.65*2^51*(1+1+19+19+19+19+19+19+19+19))
		  i.e. |h1| <= 1.7*2^59; narrower ranges for h3, h5, h7, h9
		*/

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
    /* |h0| <= 2^25 */
    /* |h4| <= 2^25 */
    /* |h1| <= 1.71*2^59 */
    /* |h5| <= 1.71*2^59 */

    carry1 = (h1 + (long)(1 << 24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
    carry5 = (h5 + (long)(1 << 24)) >> 25; h6 += carry5; h5 -= carry5 << 25;
    /* |h1| <= 2^24; from now on fits into int */
    /* |h5| <= 2^24; from now on fits into int */
    /* |h2| <= 1.41*2^60 */
    /* |h6| <= 1.41*2^60 */

    carry2 = (h2 + (long)(1 << 25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
    carry6 = (h6 + (long)(1 << 25)) >> 26; h7 += carry6; h6 -= carry6 << 26;
    /* |h2| <= 2^25; from now on fits into int unchanged */
    /* |h6| <= 2^25; from now on fits into int unchanged */
    /* |h3| <= 1.71*2^59 */
    /* |h7| <= 1.71*2^59 */

    carry3 = (h3 + (long)(1 << 24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
    carry7 = (h7 + (long)(1 << 24)) >> 25; h8 += carry7; h7 -= carry7 << 25;
    /* |h3| <= 2^24; from now on fits into int unchanged */
    /* |h7| <= 2^24; from now on fits into int unchanged */
    /* |h4| <= 1.72*2^34 */
    /* |h8| <= 1.41*2^60 */

    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
    carry8 = (h8 + (long)(1 << 25)) >> 26; h9 += carry8; h8 -= carry8 << 26;
    /* |h4| <= 2^25; from now on fits into int unchanged */
    /* |h8| <= 2^25; from now on fits into int unchanged */
    /* |h5| <= 1.01*2^24 */
    /* |h9| <= 1.71*2^59 */

    carry9 = (h9 + (long)(1 << 24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;
    /* |h9| <= 2^24; from now on fits into int unchanged */
    /* |h0| <= 1.1*2^39 */

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
    /* |h0| <= 2^25; from now on fits into int unchanged */
    /* |h1| <= 1.01*2^24 */

    h.x0 = (int)h0;
    h.x1 = (int)h1;
    h.x2 = (int)h2;
    h.x3 = (int)h3;
    h.x4 = (int)h4;
    h.x5 = (int)h5;
    h.x6 = (int)h6;
    h.x7 = (int)h7;
    h.x8 = (int)h8;
    h.x9 = (int)h9;
  }

	/*
	h = f * 121666
	Can overlap h with f.

	Preconditions:
	   |f| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.

	Postconditions:
	   |h| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
	*/

  public static void fe_mul121666(FieldElement h, FieldElement f)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    long h0 = f0 * (long)121666;
    long h1 = f1 * (long)121666;
    long h2 = f2 * (long)121666;
    long h3 = f3 * (long)121666;
    long h4 = f4 * (long)121666;
    long h5 = f5 * (long)121666;
    long h6 = f6 * (long)121666;
    long h7 = f7 * (long)121666;
    long h8 = f8 * (long)121666;
    long h9 = f9 * (long)121666;
    long carry0;
    long carry1;
    long carry2;
    long carry3;
    long carry4;
    long carry5;
    long carry6;
    long carry7;
    long carry8;
    long carry9;

    carry9 = (h9 + (long)(1 << 24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;
    carry1 = (h1 + (long)(1 << 24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
    carry3 = (h3 + (long)(1 << 24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
    carry5 = (h5 + (long)(1 << 24)) >> 25; h6 += carry5; h5 -= carry5 << 25;
    carry7 = (h7 + (long)(1 << 24)) >> 25; h8 += carry7; h7 -= carry7 << 25;

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
    carry2 = (h2 + (long)(1 << 25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
    carry6 = (h6 + (long)(1 << 25)) >> 26; h7 += carry6; h6 -= carry6 << 26;
    carry8 = (h8 + (long)(1 << 25)) >> 26; h9 += carry8; h8 -= carry8 << 26;

    h.x0 = (int)h0;
    h.x1 = (int)h1;
    h.x2 = (int)h2;
    h.x3 = (int)h3;
    h.x4 = (int)h4;
    h.x5 = (int)h5;
    h.x6 = (int)h6;
    h.x7 = (int)h7;
    h.x8 = (int)h8;
    h.x9 = (int)h9;
  }

  /*
  h = -f

  Preconditions:
     |f| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.

  Postconditions:
     |h| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
  */
  static void fe_neg(FieldElement h, FieldElement f)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int h0 = -f0;
    int h1 = -f1;
    int h2 = -f2;
    int h3 = -f3;
    int h4 = -f4;
    int h5 = -f5;
    int h6 = -f6;
    int h7 = -f7;
    int h8 = -f8;
    int h9 = -f9;
    h.x0 = h0;
    h.x1 = h1;
    h.x2 = h2;
    h.x3 = h3;
    h.x4 = h4;
    h.x5 = h5;
    h.x6 = h6;
    h.x7 = h7;
    h.x8 = h8;
    h.x9 = h9;
  }

  static void fe_pow22523(FieldElement result, FieldElement z)
  {
    FieldElement t0=new FieldElement();
    FieldElement t1=new FieldElement();
    FieldElement t2=new FieldElement();
    int i;

    /* qhasm: fe z1 */

    /* qhasm: fe z2 */

    /* qhasm: fe z8 */

    /* qhasm: fe z9 */

    /* qhasm: fe z11 */

    /* qhasm: fe z22 */

    /* qhasm: fe z_5_0 */

    /* qhasm: fe z_10_5 */

    /* qhasm: fe z_10_0 */

    /* qhasm: fe z_20_10 */

    /* qhasm: fe z_20_0 */

    /* qhasm: fe z_40_20 */

    /* qhasm: fe z_40_0 */

    /* qhasm: fe z_50_10 */

    /* qhasm: fe z_50_0 */

    /* qhasm: fe z_100_50 */

    /* qhasm: fe z_100_0 */

    /* qhasm: fe z_200_100 */

    /* qhasm: fe z_200_0 */

    /* qhasm: fe z_250_50 */

    /* qhasm: fe z_250_0 */

    /* qhasm: fe z_252_2 */

    /* qhasm: fe z_252_3 */

    /* qhasm: enter pow22523 */

    /* qhasm: z2 = z1^2^1 */
    /* asm 1: fe_sq(>z2=fe#1,<z1=fe#11); for (i = 1;i < 1;++i) fe_sq(>z2=fe#1,>z2=fe#1); */
    /* asm 2: fe_sq(>z2=t0,<z1=z); for (i = 1;i < 1;++i) fe_sq(>z2=t0,>z2=t0); */
    fe_sq(t0, z); //for (i = 1; i < 1; ++i) fe_sq(t0, t0);

    /* qhasm: z8 = z2^2^2 */
    /* asm 1: fe_sq(>z8=fe#2,<z2=fe#1); for (i = 1;i < 2;++i) fe_sq(>z8=fe#2,>z8=fe#2); */
    /* asm 2: fe_sq(>z8=t1,<z2=t0); for (i = 1;i < 2;++i) fe_sq(>z8=t1,>z8=t1); */
    fe_sq(t1, t0); for (i = 1; i < 2; ++i) fe_sq(t1, t1);

    /* qhasm: z9 = z1*z8 */
    /* asm 1: fe_mul(>z9=fe#2,<z1=fe#11,<z8=fe#2); */
    /* asm 2: fe_mul(>z9=t1,<z1=z,<z8=t1); */
    fe_mul(t1, z, t1);

    /* qhasm: z11 = z2*z9 */
    /* asm 1: fe_mul(>z11=fe#1,<z2=fe#1,<z9=fe#2); */
    /* asm 2: fe_mul(>z11=t0,<z2=t0,<z9=t1); */
    fe_mul(t0,  t0,  t1);

    /* qhasm: z22 = z11^2^1 */
    /* asm 1: fe_sq(>z22=fe#1,<z11=fe#1); for (i = 1;i < 1;++i) fe_sq(>z22=fe#1,>z22=fe#1); */
    /* asm 2: fe_sq(>z22=t0,<z11=t0); for (i = 1;i < 1;++i) fe_sq(>z22=t0,>z22=t0); */
    fe_sq(t0, t0); //for (i = 1; i < 1; ++i) fe_sq(t0,  t0);

    /* qhasm: z_5_0 = z9*z22 */
    /* asm 1: fe_mul(>z_5_0=fe#1,<z9=fe#2,<z22=fe#1); */
    /* asm 2: fe_mul(>z_5_0=t0,<z9=t1,<z22=t0); */
    fe_mul(t0, t1,  t0);

    /* qhasm: z_10_5 = z_5_0^2^5 */
    /* asm 1: fe_sq(>z_10_5=fe#2,<z_5_0=fe#1); for (i = 1;i < 5;++i) fe_sq(>z_10_5=fe#2,>z_10_5=fe#2); */
    /* asm 2: fe_sq(>z_10_5=t1,<z_5_0=t0); for (i = 1;i < 5;++i) fe_sq(>z_10_5=t1,>z_10_5=t1); */
    fe_sq(t1, t0); for (i = 1; i < 5; ++i) fe_sq(t1,  t1);

    /* qhasm: z_10_0 = z_10_5*z_5_0 */
    /* asm 1: fe_mul(>z_10_0=fe#1,<z_10_5=fe#2,<z_5_0=fe#1); */
    /* asm 2: fe_mul(>z_10_0=t0,<z_10_5=t1,<z_5_0=t0); */
    fe_mul(t0,  t1,  t0);

    /* qhasm: z_20_10 = z_10_0^2^10 */
    /* asm 1: fe_sq(>z_20_10=fe#2,<z_10_0=fe#1); for (i = 1;i < 10;++i) fe_sq(>z_20_10=fe#2,>z_20_10=fe#2); */
    /* asm 2: fe_sq(>z_20_10=t1,<z_10_0=t0); for (i = 1;i < 10;++i) fe_sq(>z_20_10=t1,>z_20_10=t1); */
    fe_sq(t1,  t0); for (i = 1; i < 10; ++i) fe_sq(t1,  t1);

    /* qhasm: z_20_0 = z_20_10*z_10_0 */
    /* asm 1: fe_mul(>z_20_0=fe#2,<z_20_10=fe#2,<z_10_0=fe#1); */
    /* asm 2: fe_mul(>z_20_0=t1,<z_20_10=t1,<z_10_0=t0); */
    fe_mul(t1,  t1,  t0);

    /* qhasm: z_40_20 = z_20_0^2^20 */
    /* asm 1: fe_sq(>z_40_20=fe#3,<z_20_0=fe#2); for (i = 1;i < 20;++i) fe_sq(>z_40_20=fe#3,>z_40_20=fe#3); */
    /* asm 2: fe_sq(>z_40_20=t2,<z_20_0=t1); for (i = 1;i < 20;++i) fe_sq(>z_40_20=t2,>z_40_20=t2); */
    fe_sq(t2,  t1); for (i = 1; i < 20; ++i) fe_sq(t2,  t2);

    /* qhasm: z_40_0 = z_40_20*z_20_0 */
    /* asm 1: fe_mul(>z_40_0=fe#2,<z_40_20=fe#3,<z_20_0=fe#2); */
    /* asm 2: fe_mul(>z_40_0=t1,<z_40_20=t2,<z_20_0=t1); */
    fe_mul(t1, t2,  t1);

    /* qhasm: z_50_10 = z_40_0^2^10 */
    /* asm 1: fe_sq(>z_50_10=fe#2,<z_40_0=fe#2); for (i = 1;i < 10;++i) fe_sq(>z_50_10=fe#2,>z_50_10=fe#2); */
    /* asm 2: fe_sq(>z_50_10=t1,<z_40_0=t1); for (i = 1;i < 10;++i) fe_sq(>z_50_10=t1,>z_50_10=t1); */
    fe_sq(t1, t1); for (i = 1; i < 10; ++i) fe_sq(t1, t1);

    /* qhasm: z_50_0 = z_50_10*z_10_0 */
    /* asm 1: fe_mul(>z_50_0=fe#1,<z_50_10=fe#2,<z_10_0=fe#1); */
    /* asm 2: fe_mul(>z_50_0=t0,<z_50_10=t1,<z_10_0=t0); */
    fe_mul(t0,  t1,  t0);

    /* qhasm: z_100_50 = z_50_0^2^50 */
    /* asm 1: fe_sq(>z_100_50=fe#2,<z_50_0=fe#1); for (i = 1;i < 50;++i) fe_sq(>z_100_50=fe#2,>z_100_50=fe#2); */
    /* asm 2: fe_sq(>z_100_50=t1,<z_50_0=t0); for (i = 1;i < 50;++i) fe_sq(>z_100_50=t1,>z_100_50=t1); */
    fe_sq(t1,  t0); for (i = 1; i < 50; ++i) fe_sq(t1,  t1);

    /* qhasm: z_100_0 = z_100_50*z_50_0 */
    /* asm 1: fe_mul(>z_100_0=fe#2,<z_100_50=fe#2,<z_50_0=fe#1); */
    /* asm 2: fe_mul(>z_100_0=t1,<z_100_50=t1,<z_50_0=t0); */
    fe_mul(t1, t1,  t0);

    /* qhasm: z_200_100 = z_100_0^2^100 */
    /* asm 1: fe_sq(>z_200_100=fe#3,<z_100_0=fe#2); for (i = 1;i < 100;++i) fe_sq(>z_200_100=fe#3,>z_200_100=fe#3); */
    /* asm 2: fe_sq(>z_200_100=t2,<z_100_0=t1); for (i = 1;i < 100;++i) fe_sq(>z_200_100=t2,>z_200_100=t2); */
    fe_sq(t2,  t1); for (i = 1; i < 100; ++i) fe_sq(t2,  t2);

    /* qhasm: z_200_0 = z_200_100*z_100_0 */
    /* asm 1: fe_mul(>z_200_0=fe#2,<z_200_100=fe#3,<z_100_0=fe#2); */
    /* asm 2: fe_mul(>z_200_0=t1,<z_200_100=t2,<z_100_0=t1); */
    fe_mul(t1,  t2,  t1);

    /* qhasm: z_250_50 = z_200_0^2^50 */
    /* asm 1: fe_sq(>z_250_50=fe#2,<z_200_0=fe#2); for (i = 1;i < 50;++i) fe_sq(>z_250_50=fe#2,>z_250_50=fe#2); */
    /* asm 2: fe_sq(>z_250_50=t1,<z_200_0=t1); for (i = 1;i < 50;++i) fe_sq(>z_250_50=t1,>z_250_50=t1); */
    fe_sq(t1, t1); for (i = 1; i < 50; ++i) fe_sq(t1, t1);

    /* qhasm: z_250_0 = z_250_50*z_50_0 */
    /* asm 1: fe_mul(>z_250_0=fe#1,<z_250_50=fe#2,<z_50_0=fe#1); */
    /* asm 2: fe_mul(>z_250_0=t0,<z_250_50=t1,<z_50_0=t0); */
    fe_mul(t0,  t1,  t0);

    /* qhasm: z_252_2 = z_250_0^2^2 */
    /* asm 1: fe_sq(>z_252_2=fe#1,<z_250_0=fe#1); for (i = 1;i < 2;++i) fe_sq(>z_252_2=fe#1,>z_252_2=fe#1); */
    /* asm 2: fe_sq(>z_252_2=t0,<z_250_0=t0); for (i = 1;i < 2;++i) fe_sq(>z_252_2=t0,>z_252_2=t0); */
    fe_sq(t0,  t0); for (i = 1; i < 2; ++i) fe_sq(t0, t0);

    /* qhasm: z_252_3 = z_252_2*z1 */
    /* asm 1: fe_mul(>z_252_3=fe#12,<z_252_2=fe#1,<z1=fe#11); */
    /* asm 2: fe_mul(>z_252_3=out,<z_252_2=t0,<z1=z); */
    fe_mul(result,  t0, z);

    /* qhasm: return */
  }

	/*
	h = f * f
	Can overlap h with f.

	Preconditions:
	   |f| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.

	Postconditions:
	   |h| bounded by 1.01*2^25,1.01*2^24,1.01*2^25,1.01*2^24,etc.
	*/

  /*
  See fe_mul.c for discussion of implementation strategy.
  */
  static void fe_sq(FieldElement h, FieldElement f)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int f0_2 = 2 * f0;
    int f1_2 = 2 * f1;
    int f2_2 = 2 * f2;
    int f3_2 = 2 * f3;
    int f4_2 = 2 * f4;
    int f5_2 = 2 * f5;
    int f6_2 = 2 * f6;
    int f7_2 = 2 * f7;
    int f5_38 = 38 * f5; /* 1.959375*2^30 */
    int f6_19 = 19 * f6; /* 1.959375*2^30 */
    int f7_38 = 38 * f7; /* 1.959375*2^30 */
    int f8_19 = 19 * f8; /* 1.959375*2^30 */
    int f9_38 = 38 * f9; /* 1.959375*2^30 */
    long f0f0 = f0 * (long)f0;
    long f0f1_2 = f0_2 * (long)f1;
    long f0f2_2 = f0_2 * (long)f2;
    long f0f3_2 = f0_2 * (long)f3;
    long f0f4_2 = f0_2 * (long)f4;
    long f0f5_2 = f0_2 * (long)f5;
    long f0f6_2 = f0_2 * (long)f6;
    long f0f7_2 = f0_2 * (long)f7;
    long f0f8_2 = f0_2 * (long)f8;
    long f0f9_2 = f0_2 * (long)f9;
    long f1f1_2 = f1_2 * (long)f1;
    long f1f2_2 = f1_2 * (long)f2;
    long f1f3_4 = f1_2 * (long)f3_2;
    long f1f4_2 = f1_2 * (long)f4;
    long f1f5_4 = f1_2 * (long)f5_2;
    long f1f6_2 = f1_2 * (long)f6;
    long f1f7_4 = f1_2 * (long)f7_2;
    long f1f8_2 = f1_2 * (long)f8;
    long f1f9_76 = f1_2 * (long)f9_38;
    long f2f2 = f2 * (long)f2;
    long f2f3_2 = f2_2 * (long)f3;
    long f2f4_2 = f2_2 * (long)f4;
    long f2f5_2 = f2_2 * (long)f5;
    long f2f6_2 = f2_2 * (long)f6;
    long f2f7_2 = f2_2 * (long)f7;
    long f2f8_38 = f2_2 * (long)f8_19;
    long f2f9_38 = f2 * (long)f9_38;
    long f3f3_2 = f3_2 * (long)f3;
    long f3f4_2 = f3_2 * (long)f4;
    long f3f5_4 = f3_2 * (long)f5_2;
    long f3f6_2 = f3_2 * (long)f6;
    long f3f7_76 = f3_2 * (long)f7_38;
    long f3f8_38 = f3_2 * (long)f8_19;
    long f3f9_76 = f3_2 * (long)f9_38;
    long f4f4 = f4 * (long)f4;
    long f4f5_2 = f4_2 * (long)f5;
    long f4f6_38 = f4_2 * (long)f6_19;
    long f4f7_38 = f4 * (long)f7_38;
    long f4f8_38 = f4_2 * (long)f8_19;
    long f4f9_38 = f4 * (long)f9_38;
    long f5f5_38 = f5 * (long)f5_38;
    long f5f6_38 = f5_2 * (long)f6_19;
    long f5f7_76 = f5_2 * (long)f7_38;
    long f5f8_38 = f5_2 * (long)f8_19;
    long f5f9_76 = f5_2 * (long)f9_38;
    long f6f6_19 = f6 * (long)f6_19;
    long f6f7_38 = f6 * (long)f7_38;
    long f6f8_38 = f6_2 * (long)f8_19;
    long f6f9_38 = f6 * (long)f9_38;
    long f7f7_38 = f7 * (long)f7_38;
    long f7f8_38 = f7_2 * (long)f8_19;
    long f7f9_76 = f7_2 * (long)f9_38;
    long f8f8_19 = f8 * (long)f8_19;
    long f8f9_38 = f8 * (long)f9_38;
    long f9f9_38 = f9 * (long)f9_38;
    long h0 = f0f0 + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
    long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
    long h2 = f0f2_2 + f1f1_2 + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
    long h3 = f0f3_2 + f1f2_2 + f4f9_38 + f5f8_38 + f6f7_38;
    long h4 = f0f4_2 + f1f3_4 + f2f2 + f5f9_76 + f6f8_38 + f7f7_38;
    long h5 = f0f5_2 + f1f4_2 + f2f3_2 + f6f9_38 + f7f8_38;
    long h6 = f0f6_2 + f1f5_4 + f2f4_2 + f3f3_2 + f7f9_76 + f8f8_19;
    long h7 = f0f7_2 + f1f6_2 + f2f5_2 + f3f4_2 + f8f9_38;
    long h8 = f0f8_2 + f1f7_4 + f2f6_2 + f3f5_4 + f4f4 + f9f9_38;
    long h9 = f0f9_2 + f1f8_2 + f2f7_2 + f3f6_2 + f4f5_2;
    long carry0;
    long carry1;
    long carry2;
    long carry3;
    long carry4;
    long carry5;
    long carry6;
    long carry7;
    long carry8;
    long carry9;

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;

    carry1 = (h1 + (long)(1 << 24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
    carry5 = (h5 + (long)(1 << 24)) >> 25; h6 += carry5; h5 -= carry5 << 25;

    carry2 = (h2 + (long)(1 << 25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
    carry6 = (h6 + (long)(1 << 25)) >> 26; h7 += carry6; h6 -= carry6 << 26;

    carry3 = (h3 + (long)(1 << 24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
    carry7 = (h7 + (long)(1 << 24)) >> 25; h8 += carry7; h7 -= carry7 << 25;

    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
    carry8 = (h8 + (long)(1 << 25)) >> 26; h9 += carry8; h8 -= carry8 << 26;

    carry9 = (h9 + (long)(1 << 24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;

    h.x0 = (int)h0;
    h.x1 = (int)h1;
    h.x2 = (int)h2;
    h.x3 = (int)h3;
    h.x4 = (int)h4;
    h.x5 = (int)h5;
    h.x6 = (int)h6;
    h.x7 = (int)h7;
    h.x8 = (int)h8;
    h.x9 = (int)h9;
  }

    /*
    h = 2 * f * f
    Can overlap h with f.

    Preconditions:
       |f| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.

    Postconditions:
       |h| bounded by 1.01*2^25,1.01*2^24,1.01*2^25,1.01*2^24,etc.
    */

  /*
  See fe_mul.c for discussion of implementation strategy.
  */
  static void fe_sq2(FieldElement h, FieldElement f)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int f0_2 = 2 * f0;
    int f1_2 = 2 * f1;
    int f2_2 = 2 * f2;
    int f3_2 = 2 * f3;
    int f4_2 = 2 * f4;
    int f5_2 = 2 * f5;
    int f6_2 = 2 * f6;
    int f7_2 = 2 * f7;
    int f5_38 = 38 * f5; /* 1.959375*2^30 */
    int f6_19 = 19 * f6; /* 1.959375*2^30 */
    int f7_38 = 38 * f7; /* 1.959375*2^30 */
    int f8_19 = 19 * f8; /* 1.959375*2^30 */
    int f9_38 = 38 * f9; /* 1.959375*2^30 */
    long f0f0 = f0 * (long)f0;
    long f0f1_2 = f0_2 * (long)f1;
    long f0f2_2 = f0_2 * (long)f2;
    long f0f3_2 = f0_2 * (long)f3;
    long f0f4_2 = f0_2 * (long)f4;
    long f0f5_2 = f0_2 * (long)f5;
    long f0f6_2 = f0_2 * (long)f6;
    long f0f7_2 = f0_2 * (long)f7;
    long f0f8_2 = f0_2 * (long)f8;
    long f0f9_2 = f0_2 * (long)f9;
    long f1f1_2 = f1_2 * (long)f1;
    long f1f2_2 = f1_2 * (long)f2;
    long f1f3_4 = f1_2 * (long)f3_2;
    long f1f4_2 = f1_2 * (long)f4;
    long f1f5_4 = f1_2 * (long)f5_2;
    long f1f6_2 = f1_2 * (long)f6;
    long f1f7_4 = f1_2 * (long)f7_2;
    long f1f8_2 = f1_2 * (long)f8;
    long f1f9_76 = f1_2 * (long)f9_38;
    long f2f2 = f2 * (long)f2;
    long f2f3_2 = f2_2 * (long)f3;
    long f2f4_2 = f2_2 * (long)f4;
    long f2f5_2 = f2_2 * (long)f5;
    long f2f6_2 = f2_2 * (long)f6;
    long f2f7_2 = f2_2 * (long)f7;
    long f2f8_38 = f2_2 * (long)f8_19;
    long f2f9_38 = f2 * (long)f9_38;
    long f3f3_2 = f3_2 * (long)f3;
    long f3f4_2 = f3_2 * (long)f4;
    long f3f5_4 = f3_2 * (long)f5_2;
    long f3f6_2 = f3_2 * (long)f6;
    long f3f7_76 = f3_2 * (long)f7_38;
    long f3f8_38 = f3_2 * (long)f8_19;
    long f3f9_76 = f3_2 * (long)f9_38;
    long f4f4 = f4 * (long)f4;
    long f4f5_2 = f4_2 * (long)f5;
    long f4f6_38 = f4_2 * (long)f6_19;
    long f4f7_38 = f4 * (long)f7_38;
    long f4f8_38 = f4_2 * (long)f8_19;
    long f4f9_38 = f4 * (long)f9_38;
    long f5f5_38 = f5 * (long)f5_38;
    long f5f6_38 = f5_2 * (long)f6_19;
    long f5f7_76 = f5_2 * (long)f7_38;
    long f5f8_38 = f5_2 * (long)f8_19;
    long f5f9_76 = f5_2 * (long)f9_38;
    long f6f6_19 = f6 * (long)f6_19;
    long f6f7_38 = f6 * (long)f7_38;
    long f6f8_38 = f6_2 * (long)f8_19;
    long f6f9_38 = f6 * (long)f9_38;
    long f7f7_38 = f7 * (long)f7_38;
    long f7f8_38 = f7_2 * (long)f8_19;
    long f7f9_76 = f7_2 * (long)f9_38;
    long f8f8_19 = f8 * (long)f8_19;
    long f8f9_38 = f8 * (long)f9_38;
    long f9f9_38 = f9 * (long)f9_38;
    long h0 = f0f0 + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
    long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
    long h2 = f0f2_2 + f1f1_2 + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
    long h3 = f0f3_2 + f1f2_2 + f4f9_38 + f5f8_38 + f6f7_38;
    long h4 = f0f4_2 + f1f3_4 + f2f2 + f5f9_76 + f6f8_38 + f7f7_38;
    long h5 = f0f5_2 + f1f4_2 + f2f3_2 + f6f9_38 + f7f8_38;
    long h6 = f0f6_2 + f1f5_4 + f2f4_2 + f3f3_2 + f7f9_76 + f8f8_19;
    long h7 = f0f7_2 + f1f6_2 + f2f5_2 + f3f4_2 + f8f9_38;
    long h8 = f0f8_2 + f1f7_4 + f2f6_2 + f3f5_4 + f4f4 + f9f9_38;
    long h9 = f0f9_2 + f1f8_2 + f2f7_2 + f3f6_2 + f4f5_2;
    long carry0;
    long carry1;
    long carry2;
    long carry3;
    long carry4;
    long carry5;
    long carry6;
    long carry7;
    long carry8;
    long carry9;

    h0 += h0;
    h1 += h1;
    h2 += h2;
    h3 += h3;
    h4 += h4;
    h5 += h5;
    h6 += h6;
    h7 += h7;
    h8 += h8;
    h9 += h9;

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;

    carry1 = (h1 + (long)(1 << 24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
    carry5 = (h5 + (long)(1 << 24)) >> 25; h6 += carry5; h5 -= carry5 << 25;

    carry2 = (h2 + (long)(1 << 25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
    carry6 = (h6 + (long)(1 << 25)) >> 26; h7 += carry6; h6 -= carry6 << 26;

    carry3 = (h3 + (long)(1 << 24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
    carry7 = (h7 + (long)(1 << 24)) >> 25; h8 += carry7; h7 -= carry7 << 25;

    carry4 = (h4 + (long)(1 << 25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
    carry8 = (h8 + (long)(1 << 25)) >> 26; h9 += carry8; h8 -= carry8 << 26;

    carry9 = (h9 + (long)(1 << 24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;

    carry0 = (h0 + (long)(1 << 25)) >> 26; h1 += carry0; h0 -= carry0 << 26;

    h.x0 = (int)h0;
    h.x1 = (int)h1;
    h.x2 = (int)h2;
    h.x3 = (int)h3;
    h.x4 = (int)h4;
    h.x5 = (int)h5;
    h.x6 = (int)h6;
    h.x7 = (int)h7;
    h.x8 = (int)h8;
    h.x9 = (int)h9;
  }

	/*
	h = f - g
	Can overlap h with f or g.

	Preconditions:
	   |f| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
	   |g| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.

	Postconditions:
	   |h| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.
	*/

  static void fe_sub(FieldElement h, FieldElement f, FieldElement g)
  {
    int f0 = f.x0;
    int f1 = f.x1;
    int f2 = f.x2;
    int f3 = f.x3;
    int f4 = f.x4;
    int f5 = f.x5;
    int f6 = f.x6;
    int f7 = f.x7;
    int f8 = f.x8;
    int f9 = f.x9;
    int g0 = g.x0;
    int g1 = g.x1;
    int g2 = g.x2;
    int g3 = g.x3;
    int g4 = g.x4;
    int g5 = g.x5;
    int g6 = g.x6;
    int g7 = g.x7;
    int g8 = g.x8;
    int g9 = g.x9;
    int h0 = f0 - g0;
    int h1 = f1 - g1;
    int h2 = f2 - g2;
    int h3 = f3 - g3;
    int h4 = f4 - g4;
    int h5 = f5 - g5;
    int h6 = f6 - g6;
    int h7 = f7 - g7;
    int h8 = f8 - g8;
    int h9 = f9 - g9;
    h.x0 = h0;
    h.x1 = h1;
    h.x2 = h2;
    h.x3 = h3;
    h.x4 = h4;
    h.x5 = h5;
    h.x6 = h6;
    h.x7 = h7;
    h.x8 = h8;
    h.x9 = h9;
  }

  /*
  Preconditions:
    |h| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.

  Write p=2^255-19; q=floor(h/p).
  Basic claim: q = floor(2^(-255)(h + 19 2^(-25)h9 + 2^(-1))).

  Proof:
    Have |h|<=p so |q|<=1 so |19^2 2^(-255) q|<1/4.
    Also have |h-2^230 h9|<2^231 so |19 2^(-255)(h-2^230 h9)|<1/4.

    Write y=2^(-1)-19^2 2^(-255)q-19 2^(-255)(h-2^230 h9).
    Then 0<y<1.

    Write r=h-pq.
    Have 0<=r<=p-1=2^255-20.
    Thus 0<=r+19(2^-255)r<r+19(2^-255)2^255<=2^255-1.

    Write x=r+19(2^-255)r+y.
    Then 0<x<2^255 so floor(2^(-255)x) = 0 so floor(q+2^(-255)x) = q.

    Have q+2^(-255)x = 2^(-255)(h + 19 2^(-25) h9 + 2^(-1))
    so floor(2^(-255)(h + 19 2^(-25) h9 + 2^(-1))) = q.
  */
  static void fe_tobytes(byte[] s, int offset, FieldElement h)
  {
    FieldElement hr=new FieldElement();
    fe_reduce(hr, h);

    int h0 = hr.x0;
    int h1 = hr.x1;
    int h2 = hr.x2;
    int h3 = hr.x3;
    int h4 = hr.x4;
    int h5 = hr.x5;
    int h6 = hr.x6;
    int h7 = hr.x7;
    int h8 = hr.x8;
    int h9 = hr.x9;

        /*
        Goal: Output h0+...+2^255 h10-2^255 q, which is between 0 and 2^255-20.
        Have h0+...+2^230 h9 between 0 and 2^255-1;
        evidently 2^255 h10-2^255 q = 0.
        Goal: Output h0+...+2^230 h9.
        */

    s[offset + 0] = (byte) (h0 >> 0);
    s[offset + 1] = (byte) (h0 >> 8);
    s[offset + 2] = (byte) (h0 >> 16);
    s[offset + 3] = (byte) ((h0 >> 24) | (h1 << 2));
    s[offset + 4] = (byte) (h1 >> 6);
    s[offset + 5] = (byte) (h1 >> 14);
    s[offset + 6] = (byte) ((h1 >> 22) | (h2 << 3));
    s[offset + 7] = (byte) (h2 >> 5);
    s[offset + 8] = (byte) (h2 >> 13);
    s[offset + 9] = (byte) ((h2 >> 21) | (h3 << 5));
    s[offset + 10] = (byte) (h3 >> 3);
    s[offset + 11] = (byte) (h3 >> 11);
    s[offset + 12] = (byte) ((h3 >> 19) | (h4 << 6));
    s[offset + 13] = (byte) (h4 >> 2);
    s[offset + 14] = (byte) (h4 >> 10);
    s[offset + 15] = (byte) (h4 >> 18);
    s[offset + 16] = (byte) (h5 >> 0);
    s[offset + 17] = (byte) (h5 >> 8);
    s[offset + 18] = (byte) (h5 >> 16);
    s[offset + 19] = (byte) ((h5 >> 24) | (h6 << 1));
    s[offset + 20] = (byte) (h6 >> 7);
    s[offset + 21] = (byte) (h6 >> 15);
    s[offset + 22] = (byte) ((h6 >> 23) | (h7 << 3));
    s[offset + 23] = (byte) (h7 >> 5);
    s[offset + 24] = (byte) (h7 >> 13);
    s[offset + 25] = (byte) ((h7 >> 21) | (h8 << 4));
    s[offset + 26] = (byte) (h8 >> 4);
    s[offset + 27] = (byte) (h8 >> 12);
    s[offset + 28] = (byte) ((h8 >> 20) | (h9 << 6));
    s[offset + 29] = (byte) (h9 >> 2);
    s[offset + 30] = (byte) (h9 >> 10);
    s[offset + 31] = (byte) (h9 >> 18);
  }

  static void fe_reduce(FieldElement hr, FieldElement h)
  {
    int h0 = h.x0;
    int h1 = h.x1;
    int h2 = h.x2;
    int h3 = h.x3;
    int h4 = h.x4;
    int h5 = h.x5;
    int h6 = h.x6;
    int h7 = h.x7;
    int h8 = h.x8;
    int h9 = h.x9;
    int q;
    int carry0;
    int carry1;
    int carry2;
    int carry3;
    int carry4;
    int carry5;
    int carry6;
    int carry7;
    int carry8;
    int carry9;

    q = (19 * h9 + (((int)1) << 24)) >> 25;
    q = (h0 + q) >> 26;
    q = (h1 + q) >> 25;
    q = (h2 + q) >> 26;
    q = (h3 + q) >> 25;
    q = (h4 + q) >> 26;
    q = (h5 + q) >> 25;
    q = (h6 + q) >> 26;
    q = (h7 + q) >> 25;
    q = (h8 + q) >> 26;
    q = (h9 + q) >> 25;

    /* Goal: Output h-(2^255-19)q, which is between 0 and 2^255-20. */
    h0 += 19 * q;
    /* Goal: Output h-2^255 q, which is between 0 and 2^255-20. */

    carry0 = h0 >> 26; h1 += carry0; h0 -= carry0 << 26;
    carry1 = h1 >> 25; h2 += carry1; h1 -= carry1 << 25;
    carry2 = h2 >> 26; h3 += carry2; h2 -= carry2 << 26;
    carry3 = h3 >> 25; h4 += carry3; h3 -= carry3 << 25;
    carry4 = h4 >> 26; h5 += carry4; h4 -= carry4 << 26;
    carry5 = h5 >> 25; h6 += carry5; h5 -= carry5 << 25;
    carry6 = h6 >> 26; h7 += carry6; h6 -= carry6 << 26;
    carry7 = h7 >> 25; h8 += carry7; h7 -= carry7 << 25;
    carry8 = h8 >> 26; h9 += carry8; h8 -= carry8 << 26;
    carry9 = h9 >> 25; h9 -= carry9 << 25;
    /* h10 = carry9 */

    hr.x0 = h0;
    hr.x1 = h1;
    hr.x2 = h2;
    hr.x3 = h3;
    hr.x4 = h4;
    hr.x5 = h5;
    hr.x6 = h6;
    hr.x7 = h7;
    hr.x8 = h8;
    hr.x9 = h9;
  }
}
