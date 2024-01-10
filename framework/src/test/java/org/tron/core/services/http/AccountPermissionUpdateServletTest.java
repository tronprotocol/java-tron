package org.tron.core.services.http;

import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import javax.annotation.Resource;

import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class AccountPermissionUpdateServletTest extends BaseTest {

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Resource
  private AccountPermissionUpdateServlet accountPermissionUpdateServlet;

  private String getParam() {
    return  "{"
            + "    \"owner_address\":\"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ\","
            + "    \"owner\":{"
            + "        \"type\":0,"
            + "        \"permission_name\":\"owner\","
            + "        \"threshold\":1,"
            + "        \"keys\":["
            + "            {"
            + "                \"address\":\"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ\","
            + "                \"weight\":1"
            + "            }"
            + "        ]"
            + "    },"
            + "    \"witness\":{"
            + "        \"type\":1,"
            + "        \"permission_name\":\"witness\","
            + "        \"threshold\":1,"
            + "        \"keys\":["
            + "            {"
            + "                \"address\":\"TRGhNNfnmgLegT4zHNjEqDSADjgmnHvubJ\","
            + "                \"weight\":1"
            + "            }"
            + "        ]"
            + "    },"
            + "    \"actives\":["
            + "        {"
            + "            \"type\":2,"
            + "            \"permission_name\":\"active12323\","
            + "            \"threshold\":2,"
            + "            \"operations\":"
            + "\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "            \"keys\":["
            + "                {"
            + "                    \"address\":\"TNhXo1GbRNCuorvYu5JFWN3m2NYr9QQpVR\","
            + "                    \"weight\":1"
            + "                },"
            + "                {"
            + "                    \"address\":\"TKwhcDup8L2PH5r6hxp5CQvQzZqJLmKvZP\","
            + "                    \"weight\":1"
            + "                }"
            + "            ]"
            + "        }"
            + "    ],"
            + "    \"visible\": true"
            + "}";
  }

  @Test
  public void test() {
    String jsonParam = getParam();
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();

    accountPermissionUpdateServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
  }
}
