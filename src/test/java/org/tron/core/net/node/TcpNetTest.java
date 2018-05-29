package org.tron.core.net.node;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TcpNetTest extends BaseNetTest {

  private static final String dbPath = "output-nodeImplTest/tcpNet";
  private static final String dbDirectory = "db_tcp_test";
  private static final String indexDirectory = "index_tcp_test";

  public TcpNetTest() {
    super(dbPath, dbDirectory, indexDirectory);
  }

  @Test
  public void test() throws InterruptedException {
    Thread.sleep(2000);
    
  }
}
