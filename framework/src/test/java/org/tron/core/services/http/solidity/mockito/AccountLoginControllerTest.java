package org.tron.core.services.http.solidity.mockito;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.testng.Assert;

import javax.servlet.http.HttpServletRequest;

import java.util.Set;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
//import org.powermock.reflect.Whitebox;

/**
 * @author alberto
 * @version 1.0.0
 * @Description
 * @date 2019-12-08 23:08
 **/
@RunWith(MockitoJUnitRunner.class)
public class AccountLoginControllerTest {
  private AccountDao accountDao;
  private HttpServletRequest request;
  private AccountLoginController accountLoginController;

  @Before
  public void setUp() {
    this.accountDao = Mockito.mock(AccountDao.class);
    this.request = Mockito.mock(HttpServletRequest.class);
    this.accountLoginController = new AccountLoginController(accountDao);
  }

  @Test
  public void testLoginSuccess() {
    Account account = new Account();
    when(request.getParameter("username")).thenReturn("alberto");
    when(request.getParameter("password")).thenReturn("520131");
    when(accountDao.findAccount(anyString(), anyString())).thenReturn(account);
    String result = accountLoginController.login(request);
    Assert.assertEquals(accountLoginController.login(request), "/index");
  }

  @Test
  public void testPeopleHello() {
    People mockPeople = Mockito.mock(People.class);
    Mockito.doAnswer(new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        System.out.println("--------->" + args);

        Assert.assertEquals(args, "other");
//        Assert.assertEquals(args,"Hello");
        return "called with arguments: " + args;
      }
    }).when(mockPeople).sayHello("Hello");

  }

  @Test
  public void testAddService() throws Exception {
    ServiceHolder tested = new ServiceHolder();
    final Object service = new Object();

    tested.addService(service);

    // 获得私有变量今昔国内验证
//    Set<String> services = Whitebox.getInternalState(tested, "services");

//    assertEquals("Size of the \"services\" Set should be 1", 1, services.size());
//    assertSame("The services Set should didn't contain the expect service",
//            service, services.iterator().next());
//  }
  }
}
