package org.tron.core.capsule.utils;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.MerkleRoot;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.utils.MerkleTree.Leaf;

@Slf4j
public class MerkleTreeTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static List<Sha256Hash> getHash(int hashNum) {
    List<Sha256Hash> hashList = new ArrayList<Sha256Hash>();
    for (int i = 0; i < hashNum; i++) {
      byte[] bytes = new byte[4];
      bytes[3] = (byte) (i & 0xFF);
      bytes[2] = (byte) ((i >> 8) & 0xFF);
      bytes[1] = (byte) ((i >> 16) & 0xFF);
      bytes[0] = (byte) ((i >> 24) & 0xFF);
      hashList.add(Sha256Hash.of(CommonParameter
          .getInstance().isECKeyCryptoEngine(), bytes));
    }
    return hashList;
  }

  private static Sha256Hash computeHash(Sha256Hash leftHash, Sha256Hash rightHash) {
    return Sha256Hash.of(CommonParameter
            .getInstance().isECKeyCryptoEngine(),
        leftHash.getByteString().concat(rightHash.getByteString()).toByteArray());
  }

  //number: the number of hash
  private static void pareTree(Leaf head, List<Sha256Hash> hashList, int maxRank, int curBank,
      int number) {
    Leaf left = head.getLeft();
    Leaf right = head.getRight();
    if (curBank < maxRank) {
      curBank++;
      number = number << 1;
      pareTree(left, hashList, maxRank, curBank, number);
      number++;
      if ((number << (maxRank - curBank)) >= hashList
          .size()) {    //The smallest leaf child number = number<<(maxRank-curBank)
        Assert.assertTrue(right == null);
        Assert.assertEquals(head.getHash(), left.getHash());  //No right, leaf = left
      } else {
        pareTree(right, hashList, maxRank, curBank, number);
        Assert.assertEquals(head.getHash(),
            computeHash(left.getHash(), right.getHash())); //hash = sha256(left || right)
      }
    } else {
      // last rank, no child, it is real leaf. Its hash in hashList.
      Assert.assertTrue(left == null);
      Assert.assertTrue(right == null);
      Assert.assertEquals(head.getHash(), hashList.get(number));
      System.out.println("curBank :" + curBank + " number :" + number);
      System.out.println(ByteArray.toHexString(head.getHash().getBytes()));
    }
  }

  private static int getRank(int num) {
    if (num <= 0) {
      return 0;
    }
    if (num == 1) {
      return 1;
    }
    int rank = 0;
    int temp = num;
    while (num > 0) {
      num = num >> 1;
      rank++;
    }
    if (temp == Math.pow(2, rank - 1)) {
      rank -= 1;
    }
    return rank;
  }

  @Test
  /**
   * Make a merkletree with no hash.
   * Will throw a exception.
   */
  public void test0HashNum() {
    List<Sha256Hash> hashList = getHash(0);  //Empty list.
    try {
      MerkleTree.getInstance().createTree(hashList);
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IndexOutOfBoundsException);
    }
  }

  @Test
  /**
   * Make a merkletree with 1 hash.
   *      root
   *      /    \
   *    H1   null
   *   /   \
   *  null null
   */
  public void test1HashNum() {
    List<Sha256Hash> hashList = getHash(1);
    MerkleTree tree = MerkleTree.getInstance().createTree(hashList);
    Leaf root = tree.getRoot();
    Assert.assertEquals(root.getHash(), hashList.get(0));

    Leaf left = root.getLeft();
    Assert.assertEquals(left.getHash(), hashList.get(0));
    Assert.assertTrue(left.getLeft() == null);
    Assert.assertTrue(left.getRight() == null);

    Assert.assertTrue(root.getRight() == null);
  }

  @Test
  /**
   * Make a merkletree with 2 hash.
   *        root
   *      /     \
   *    H1       H2
   *  /   \    /   \
   *null null null null
   */
  public void test2HashNum() {
    List<Sha256Hash> hashList = getHash(2);
    MerkleTree tree = MerkleTree.getInstance().createTree(hashList);
    Leaf root = tree.getRoot();
    Assert.assertEquals(root.getHash(), computeHash(hashList.get(0), hashList.get(1)));

    Leaf left = root.getLeft();
    Assert.assertEquals(left.getHash(), hashList.get(0));
    Assert.assertTrue(left.getLeft() == null);
    Assert.assertTrue(left.getRight() == null);

    Leaf right = root.getRight();
    Assert.assertEquals(right.getHash(), hashList.get(1));
    Assert.assertTrue(right.getLeft() == null);
    Assert.assertTrue(right.getRight() == null);
  }

  @Test
  /**
   * Make a merkletree with any num hash.
   *
   *rank0                                 root
   *rank1                  0                                    1
   *rank2          0                1                      2
   *rank3      0      1        2         3             4
   *rank4    0  1   2   3    4   5     6     7      8     9
   *rank5  0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18
   *
   * leftNum = 2 * headNum
   * rightNum = leftNum + 1
   * curBank < maxRank, there must have left child
   * if have left child but no right child,  headHash = leftHash
   * if both have left child and right child, headHash = SHA256(leftHash||rightHash)
   * curBank = maxRank, no child, it is real leaf. Its hash in hashList.
   */
  public void testAnyHashNum() {
    int maxNum = 128;
    for (int hashNum = 1; hashNum <= maxNum; hashNum++) {
      int maxRank = getRank(hashNum);
      List<Sha256Hash> hashList = getHash(hashNum);
      MerkleTree tree = MerkleTree.getInstance().createTree(hashList);
      Leaf root = tree.getRoot();
      pareTree(root, hashList, maxRank, 0, 0);
    }
  }

  @Test
  @Ignore
  public void testConcurrent() {
    Sha256Hash root1 = Sha256Hash.wrap(
        ByteString.fromHex("6cb38b4f493db8bacf26123cd4253bbfc530c708b97b3747e782f64097c3c482"));
    Sha256Hash root2 = Sha256Hash.wrap(
        ByteString.fromHex("4bfc60ea3de4f5d1476f839874df0aba38eec4e524d6fa63f5b19c4bf527eaf3"));
    List<Sha256Hash> list1 = IntStream.range(0, 10000).mapToObj(i ->
            Sha256Hash.of(true, ("byte1-" + i).getBytes(StandardCharsets.UTF_8)))
        .collect(Collectors.toList());
    List<Sha256Hash> list2 = IntStream.range(0, 10000).mapToObj(i ->
            Sha256Hash.of(true, ("byte2-" + i).getBytes(StandardCharsets.UTF_8)))
        .collect(Collectors.toList());
    Assert.assertEquals(root1, MerkleTree.getInstance().createTree(list1).getRoot().getHash());
    Assert.assertEquals(root2, MerkleTree.getInstance().createTree(list2).getRoot().getHash());
    Assert.assertEquals(root1, MerkleRoot.root(list1));
    Assert.assertEquals(root2, MerkleRoot.root(list2));
    exception.expect(ArrayIndexOutOfBoundsException.class);
    IntStream.range(0, 1000).parallel().forEach(i -> Assert.assertEquals(
        MerkleTree.getInstance().createTree(i % 2 == 0 ? list1 : list2).getRoot().getHash(),
        MerkleRoot.root(i % 2 == 0 ? list1 : list2))
    );
  }
}
