package org.tron.core.services.http;

//import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.arquillian.junit.Arquillian;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.asset.EmptyAsset;
//import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

import static org.junit.Assert.*;

//@RunWith(Arquillian.class)
public class AccountPermissionUpdateServletTest {

    private AccountPermissionUpdateServlet accountPermissionUpdateServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
//    @Deployment
//    public static JavaArchive createDeployment() {
//        return ShrinkWrap.create(JavaArchive.class)
//                .addClass(AccountPermissionUpdateServlet.class)
//                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
//    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void doGet() {
    }

    @Test
    public void doPost() {
    }
    //带参数的post请求
    @Test
    public void doPostWithParam(){

        String url = "http://127.0.0.1:8090/wallet/accountpermissionupdate";
        HashMap<String, String> paramMap = new HashMap<String,String>();


        accountPermissionUpdateServlet.doPost(request,response);


    }

}
