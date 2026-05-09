package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;
import static org.tron.core.services.http.Util.EVENTS_DEPRECATED_MSG;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;

public class ScanShieldedTRC20NotesServletTest extends BaseTest {

  @Resource
  private ScanShieldedTRC20NotesByIvkServlet scanShieldedTRC20NotesByIvkServlet;

  @Resource
  private ScanShieldedTRC20NotesByOvkServlet scanShieldedTRC20NotesByOvkServlet;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath()}, TestConstants.TEST_CONF);
  }

  @Test
  public void testIvkPostRejectsDeprecatedEvents() throws Exception {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent("{\"events\":[\"mint\"]}".getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByIvkServlet.doPost(request, response);
    Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertTrue(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

  @Test
  public void testIvkGetRejectsDeprecatedEvents() throws Exception {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    request.addParameter("events", "mint");
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByIvkServlet.doGet(request, response);
    Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertTrue(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

  @Test
  public void testOvkPostRejectsDeprecatedEvents() throws Exception {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent("{\"events\":[\"burn\"]}".getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByOvkServlet.doPost(request, response);
    Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertTrue(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

  @Test
  public void testOvkGetRejectsDeprecatedEvents() throws Exception {
    MockHttpServletRequest request = createRequest(HttpGet.METHOD_NAME);
    request.addParameter("events", "burn");
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByOvkServlet.doGet(request, response);
    Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertTrue(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

  @Test
  public void testIvkPostEmptyEventsPassesGuard() throws Exception {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent("{\"events\":[\"\"]}".getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByIvkServlet.doPost(request, response);
    Assert.assertNotEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertFalse(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

  @Test
  public void testOvkPostEmptyEventsPassesGuard() throws Exception {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent("{\"events\":[\"\"]}".getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByOvkServlet.doPost(request, response);
    Assert.assertNotEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertFalse(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

  @Test
  public void testIvkPostWithoutEventsPassesGuard() throws Exception {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent("{\"start_block_index\":0,\"end_block_index\":1}".getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByIvkServlet.doPost(request, response);
    Assert.assertNotEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertFalse(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

  @Test
  public void testOvkPostWithoutEventsPassesGuard() throws Exception {
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent("{\"start_block_index\":0,\"end_block_index\":1}".getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    scanShieldedTRC20NotesByOvkServlet.doPost(request, response);
    Assert.assertNotEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    Assert.assertFalse(response.getContentAsString().contains(EVENTS_DEPRECATED_MSG));
  }

}
