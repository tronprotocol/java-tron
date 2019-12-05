package org.tron.core.services.http;

//import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.arquillian.junit.Arquillian;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.asset.EmptyAsset;
//import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.MockUtil.createMock;

//@RunWith(Arquillian.class)
public class AccountPermissionUpdateServletTest {

    private AccountPermissionUpdateServlet accountPermissionUpdateServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp(){
        accountPermissionUpdateServlet=new AccountPermissionUpdateServlet();
//        request = createMock(HttpServletRequest.class);                          //加载
//        response = createMock(HttpServletResponse.class);
    }

    @After
    public void tearDown(){
        verify(request);                //验证
        verify(response);
    }



    @Test
    public void doPostTest() throws IOException {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("username")).thenReturn("me");
        when(request.getParameter("password")).thenReturn("secret");
        PrintWriter writer = new PrintWriter("somefile.txt");
        when(response.getWriter()).thenReturn(writer);

        accountPermissionUpdateServlet.doPost(request, response);

        verify(request, atLeast(1)).getParameter("username"); // only if you want to verify username was called...
        writer.flush(); // it may not have been flushed yet...
        assertTrue(FileUtils.readFileToString(new File("somefile.txt"), "UTF-8")
                .contains("My Expected String"));

    }



}
