package org.tron.core.net.service.nodepersist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.beust.jcommander.internal.Lists;
import org.junit.Before;
import org.junit.Test;

public class DBNodeTest {
  private DBNode dbNode1;
  private DBNode dbNode2;

  @Before
  public void setUp() {
    dbNode1 = new DBNode("localhost", 3306);
    dbNode2 = new DBNode();
  }

  @Test
  public void testConstructorWithParameters() {
    assertEquals("localhost", dbNode1.getHost());
    assertEquals(3306, dbNode1.getPort());
  }

  @Test
  public void testDefaultConstructor() {
    assertNull(dbNode2.getHost());
    assertEquals(0, dbNode2.getPort());
  }

  @Test
  public void testSetAndGetHost() {
    dbNode2.setHost("127.0.0.1");
    assertEquals("127.0.0.1", dbNode2.getHost());
  }

  @Test
  public void testSetAndGetPort() {
    dbNode2.setPort(5432);
    assertEquals(5432, dbNode2.getPort());
  }


  @Test
  public void testDBNodes() {
    DBNodes dbNodes = new DBNodes();
    dbNodes.setNodes(Lists.newArrayList(dbNode1, dbNode2));
    assertEquals(3306, dbNodes.getNodes().get(0).getPort());
    assertEquals(0, dbNodes.getNodes().get(1).getPort());
  }
}