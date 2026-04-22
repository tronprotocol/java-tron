package org.tron.p2p.dns.update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.alidns20150109.Client;
import com.aliyun.alidns20150109.models.AddDomainRecordRequest;
import com.aliyun.alidns20150109.models.AddDomainRecordResponse;
import com.aliyun.alidns20150109.models.DeleteDomainRecordRequest;
import com.aliyun.alidns20150109.models.DeleteDomainRecordResponse;
import com.aliyun.alidns20150109.models.DeleteSubDomainRecordsRequest;
import com.aliyun.alidns20150109.models.DeleteSubDomainRecordsResponse;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsRequest;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponse;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecords;
import com.aliyun.alidns20150109.models.DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord;
import com.aliyun.alidns20150109.models.UpdateDomainRecordRequest;
import com.aliyun.alidns20150109.models.UpdateDomainRecordResponse;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AliClientTest {

  private AliClient aliClient;
  private Client mockClient;

  @Before
  public void setUp() throws Exception {
    // Create real AliClient, then replace the internal Client with a mock
    aliClient = new AliClient("dns.aliyuncs.com", "testKeyId", "testKeySecret", 0.1);
    mockClient = mock(Client.class);
    Field field = AliClient.class.getDeclaredField("aliDnsClient");
    field.setAccessible(true);
    field.set(aliClient, mockClient);
  }

  @Test
  public void testDeleteDomainSuccess() throws Exception {
    DeleteSubDomainRecordsResponse response = new DeleteSubDomainRecordsResponse();
    response.statusCode = 200;
    when(mockClient.deleteSubDomainRecords(any(DeleteSubDomainRecordsRequest.class)))
        .thenReturn(response);

    boolean result = aliClient.deleteDomain("example.com");
    Assert.assertTrue(result);
  }

  @Test
  public void testDeleteDomainFailure() throws Exception {
    DeleteSubDomainRecordsResponse response = new DeleteSubDomainRecordsResponse();
    response.statusCode = 500;
    when(mockClient.deleteSubDomainRecords(any(DeleteSubDomainRecordsRequest.class)))
        .thenReturn(response);

    boolean result = aliClient.deleteDomain("example.com");
    Assert.assertFalse(result);
  }

  @Test
  public void testAddRecordSuccess() throws Exception {
    AddDomainRecordResponse response = new AddDomainRecordResponse();
    response.statusCode = 200;
    when(mockClient.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(response);

    boolean result = aliClient.addRecord("example.com", "test", "value", 3600);
    Assert.assertTrue(result);
  }

  @Test
  public void testAddRecordRetryThenSuccess() throws Exception {
    AddDomainRecordResponse failResponse = new AddDomainRecordResponse();
    failResponse.statusCode = 500;

    AddDomainRecordResponse successResponse = new AddDomainRecordResponse();
    successResponse.statusCode = 200;

    when(mockClient.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(failResponse)
        .thenReturn(successResponse);

    boolean result = aliClient.addRecord("example.com", "test", "value", 3600);
    Assert.assertTrue(result);
    verify(mockClient, times(2)).addDomainRecord(any(AddDomainRecordRequest.class));
  }

  @Test
  public void testAddRecordExhaustsRetries() throws Exception {
    AddDomainRecordResponse failResponse = new AddDomainRecordResponse();
    failResponse.statusCode = 500;

    when(mockClient.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(failResponse);

    // maxRetryCount is 3, so 1 initial + 3 retries = 4 calls, then returns false
    boolean result = aliClient.addRecord("example.com", "test", "value", 3600);
    Assert.assertFalse(result);
    verify(mockClient, times(4)).addDomainRecord(any(AddDomainRecordRequest.class));
  }

  @Test
  public void testUpdateRecordSuccess() throws Exception {
    UpdateDomainRecordResponse response = new UpdateDomainRecordResponse();
    response.statusCode = 200;
    when(mockClient.updateDomainRecord(any(UpdateDomainRecordRequest.class)))
        .thenReturn(response);

    boolean result = aliClient.updateRecord("rec-1", "test", "value", 3600);
    Assert.assertTrue(result);
  }

  @Test
  public void testUpdateRecordExhaustsRetries() throws Exception {
    UpdateDomainRecordResponse failResponse = new UpdateDomainRecordResponse();
    failResponse.statusCode = 500;

    when(mockClient.updateDomainRecord(any(UpdateDomainRecordRequest.class)))
        .thenReturn(failResponse);

    boolean result = aliClient.updateRecord("rec-1", "test", "value", 3600);
    Assert.assertFalse(result);
    verify(mockClient, times(4)).updateDomainRecord(any(UpdateDomainRecordRequest.class));
  }

  @Test
  public void testDeleteRecordSuccess() throws Exception {
    DeleteDomainRecordResponse response = new DeleteDomainRecordResponse();
    response.statusCode = 200;
    when(mockClient.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(response);

    boolean result = aliClient.deleteRecord("rec-1");
    Assert.assertTrue(result);
  }

  @Test
  public void testDeleteRecordExhaustsRetries() throws Exception {
    DeleteDomainRecordResponse failResponse = new DeleteDomainRecordResponse();
    failResponse.statusCode = 500;

    when(mockClient.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(failResponse);

    boolean result = aliClient.deleteRecord("rec-1");
    Assert.assertFalse(result);
    verify(mockClient, times(4)).deleteDomainRecord(any(DeleteDomainRecordRequest.class));
  }

  @Test
  public void testCollectRecordsEmpty() throws Exception {
    DescribeDomainRecordsResponse response = new DescribeDomainRecordsResponse();
    response.statusCode = 200;

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setTotalCount(0L);
    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(
        Collections.<DescribeDomainRecordsResponseBodyDomainRecordsRecord>emptyList());
    body.setDomainRecords(domainRecords);
    response.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(response);

    Map<String, DescribeDomainRecordsResponseBodyDomainRecordsRecord> records =
        aliClient.collectRecords("example.com");
    Assert.assertNotNull(records);
    Assert.assertTrue(records.isEmpty());
  }

  @Test
  public void testCollectRecordsSinglePage() throws Exception {
    DescribeDomainRecordsResponseBodyDomainRecordsRecord record =
        new DescribeDomainRecordsResponseBodyDomainRecordsRecord();
    record.setRR("test-sub");
    record.setValue("some-value");
    record.setRecordId("rec-123");
    record.setTTL(3600L);

    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(Arrays.asList(record));

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setDomainRecords(domainRecords);
    body.setTotalCount(1L);

    DescribeDomainRecordsResponse response = new DescribeDomainRecordsResponse();
    response.statusCode = 200;
    response.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(response);

    Map<String, DescribeDomainRecordsResponseBodyDomainRecordsRecord> records =
        aliClient.collectRecords("example.com");
    Assert.assertEquals(1, records.size());
    Assert.assertTrue(records.containsKey("test-sub"));
  }

  @Test(expected = Exception.class)
  public void testCollectRecordsFailedResponse() throws Exception {
    DescribeDomainRecordsResponse response = new DescribeDomainRecordsResponse();
    response.statusCode = 500;

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setTotalCount(0L);
    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(
        Collections.<DescribeDomainRecordsResponseBodyDomainRecordsRecord>emptyList());
    body.setDomainRecords(domainRecords);
    response.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(response);

    aliClient.collectRecords("example.com");
  }

  @Test
  public void testGetRecIdFound() throws Exception {
    DescribeDomainRecordsResponseBodyDomainRecordsRecord record =
        new DescribeDomainRecordsResponseBodyDomainRecordsRecord();
    record.setRR("test");
    record.setRecordId("rec-456");

    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(Arrays.asList(record));

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setDomainRecords(domainRecords);
    body.setTotalCount(1L);

    DescribeDomainRecordsResponse response = new DescribeDomainRecordsResponse();
    response.statusCode = 200;
    response.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(response);

    String recId = aliClient.getRecId("example.com", "test");
    Assert.assertEquals("rec-456", recId);
  }

  @Test
  public void testGetRecIdNotFound() throws Exception {
    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setTotalCount(0L);

    DescribeDomainRecordsResponse response = new DescribeDomainRecordsResponse();
    response.statusCode = 200;
    response.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(response);

    String recId = aliClient.getRecId("example.com", "nonexistent");
    Assert.assertNull(recId);
  }

  @Test
  public void testGetRecIdNoMatch() throws Exception {
    DescribeDomainRecordsResponseBodyDomainRecordsRecord record =
        new DescribeDomainRecordsResponseBodyDomainRecordsRecord();
    record.setRR("other");
    record.setRecordId("rec-789");

    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(Arrays.asList(record));

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setDomainRecords(domainRecords);
    body.setTotalCount(1L);

    DescribeDomainRecordsResponse response = new DescribeDomainRecordsResponse();
    response.statusCode = 200;
    response.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(response);

    String recId = aliClient.getRecId("example.com", "test");
    Assert.assertNull(recId);
  }

  @Test
  public void testGetRecIdException() throws Exception {
    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenThrow(new RuntimeException("network error"));

    String recId = aliClient.getRecId("example.com", "test");
    Assert.assertNull(recId);
  }

  @Test
  public void testUpdateMethodAddsNewRecord() throws Exception {
    // getRecId returns null => add path
    DescribeDomainRecordsResponseBody descBody = new DescribeDomainRecordsResponseBody();
    descBody.setTotalCount(0L);

    DescribeDomainRecordsResponse descResponse = new DescribeDomainRecordsResponse();
    descResponse.statusCode = 200;
    descResponse.setBody(descBody);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(descResponse);

    AddDomainRecordResponse addResponse = mock(AddDomainRecordResponse.class);
    com.aliyun.alidns20150109.models.AddDomainRecordResponseBody addBody =
        mock(com.aliyun.alidns20150109.models.AddDomainRecordResponseBody.class);
    when(addBody.getRecordId()).thenReturn("new-rec-1");
    when(addResponse.getBody()).thenReturn(addBody);

    when(mockClient.addDomainRecord(any(AddDomainRecordRequest.class)))
        .thenReturn(addResponse);

    String recId = aliClient.update("example.com", "test", "value", 3600);
    Assert.assertEquals("new-rec-1", recId);
  }

  @Test
  public void testUpdateMethodUpdatesExistingRecord() throws Exception {
    // getRecId returns existing id => update path
    DescribeDomainRecordsResponseBodyDomainRecordsRecord record =
        new DescribeDomainRecordsResponseBodyDomainRecordsRecord();
    record.setRR("test");
    record.setRecordId("existing-rec");

    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(Arrays.asList(record));

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setDomainRecords(domainRecords);
    body.setTotalCount(1L);

    DescribeDomainRecordsResponse descResponse = new DescribeDomainRecordsResponse();
    descResponse.statusCode = 200;
    descResponse.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(descResponse);

    UpdateDomainRecordResponse updateResponse = mock(UpdateDomainRecordResponse.class);
    com.aliyun.alidns20150109.models.UpdateDomainRecordResponseBody updateBody =
        mock(com.aliyun.alidns20150109.models.UpdateDomainRecordResponseBody.class);
    when(updateBody.getRecordId()).thenReturn("existing-rec");
    when(updateResponse.getBody()).thenReturn(updateBody);

    when(mockClient.updateDomainRecord(any(UpdateDomainRecordRequest.class)))
        .thenReturn(updateResponse);

    String recId = aliClient.update("example.com", "test", "new-value", 3600);
    Assert.assertEquals("existing-rec", recId);
  }

  @Test
  public void testUpdateMethodException() throws Exception {
    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenThrow(new RuntimeException("network error"));

    String recId = aliClient.update("example.com", "test", "value", 3600);
    Assert.assertNull(recId);
  }

  @Test
  public void testDeleteByRRSuccess() throws Exception {
    // getRecId finds a record
    DescribeDomainRecordsResponseBodyDomainRecordsRecord record =
        new DescribeDomainRecordsResponseBodyDomainRecordsRecord();
    record.setRR("test");
    record.setRecordId("rec-to-delete");

    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(Arrays.asList(record));

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setDomainRecords(domainRecords);
    body.setTotalCount(1L);

    DescribeDomainRecordsResponse descResponse = new DescribeDomainRecordsResponse();
    descResponse.statusCode = 200;
    descResponse.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(descResponse);

    DeleteDomainRecordResponse deleteResponse = new DeleteDomainRecordResponse();
    deleteResponse.statusCode = 200;

    when(mockClient.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(deleteResponse);

    boolean result = aliClient.deleteByRR("example.com", "test");
    Assert.assertTrue(result);
  }

  @Test
  public void testDeleteByRRNotFound() throws Exception {
    // getRecId returns null => nothing to delete => returns true
    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setTotalCount(0L);

    DescribeDomainRecordsResponse descResponse = new DescribeDomainRecordsResponse();
    descResponse.statusCode = 200;
    descResponse.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(descResponse);

    boolean result = aliClient.deleteByRR("example.com", "nonexistent");
    Assert.assertTrue(result);
  }

  @Test
  public void testDeleteByRRDeleteFails() throws Exception {
    DescribeDomainRecordsResponseBodyDomainRecordsRecord record =
        new DescribeDomainRecordsResponseBodyDomainRecordsRecord();
    record.setRR("test");
    record.setRecordId("rec-to-delete");

    DescribeDomainRecordsResponseBodyDomainRecords domainRecords =
        new DescribeDomainRecordsResponseBodyDomainRecords();
    domainRecords.setRecord(Arrays.asList(record));

    DescribeDomainRecordsResponseBody body = new DescribeDomainRecordsResponseBody();
    body.setDomainRecords(domainRecords);
    body.setTotalCount(1L);

    DescribeDomainRecordsResponse descResponse = new DescribeDomainRecordsResponse();
    descResponse.statusCode = 200;
    descResponse.setBody(body);

    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenReturn(descResponse);

    DeleteDomainRecordResponse deleteResponse = new DeleteDomainRecordResponse();
    deleteResponse.statusCode = 500;

    when(mockClient.deleteDomainRecord(any(DeleteDomainRecordRequest.class)))
        .thenReturn(deleteResponse);

    boolean result = aliClient.deleteByRR("example.com", "test");
    Assert.assertFalse(result);
  }

  @Test
  public void testDeleteByRRException() throws Exception {
    when(mockClient.describeDomainRecords(any(DescribeDomainRecordsRequest.class)))
        .thenThrow(new RuntimeException("network error"));

    // getRecId catches exceptions internally and returns null,
    // so deleteByRR sees recId==null, skips the delete, and returns true
    boolean result = aliClient.deleteByRR("example.com", "test");
    Assert.assertTrue(result);
  }

  @Test
  public void testAliyunRootConstant() {
    Assert.assertEquals("@", AliClient.aliyunRoot);
  }
}
