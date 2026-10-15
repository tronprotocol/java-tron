package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.core.db.Manager;

public class GetTransactionListFromPendingServletTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private GetTransactionListFromPendingServlet getTransactionListFromPendingServlet;
  private Manager manager;

  @Before
  public void setUp() {
    manager = mock(Manager.class);
    getTransactionListFromPendingServlet = new GetTransactionListFromPendingServlet();
    ReflectionTestUtils.setField(getTransactionListFromPendingServlet, "manager", manager);
  }

  @Test
  public void testGet() throws Exception {
    List<String> transactionIds = Arrays.asList(
        "0000000000000000000000000000000000000000000000000000000000000001",
        "0000000000000000000000000000000000000000000000000000000000000002");
    when(manager.getTxListFromPending()).thenReturn(transactionIds);

    MockHttpServletResponse response = getPendingList();

    JsonNode result = mapper.readTree(response.getContentAsString());
    assertFalse(result.has("Error"));
    assertEquals(1, result.size());
    assertEquals(mapper.valueToTree(transactionIds), result.get("txId"));
  }

  @Test
  public void testGetWithEmptyPendingList() throws Exception {
    when(manager.getTxListFromPending()).thenReturn(Collections.emptyList());

    MockHttpServletResponse response = getPendingList();

    assertEquals("{}", response.getContentAsString().trim());
  }

  @Test
  public void testGetWithManagerFailure() throws Exception {
    when(manager.getTxListFromPending())
        .thenThrow(new IllegalStateException("internal pending queue detail"));

    MockHttpServletResponse response = getPendingList();

    assertEquals(mapper.readTree("{\"Error\":\"internal server error\"}"),
        mapper.readTree(response.getContentAsString()));
  }

  private MockHttpServletResponse getPendingList() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET",
        "/wallet/gettransactionlistfrompending");
    MockHttpServletResponse response = new MockHttpServletResponse();
    getTransactionListFromPendingServlet.doGet(request, response);
    assertEquals(200, response.getStatus());
    verify(manager).getTxListFromPending();
    return response;
  }
}
