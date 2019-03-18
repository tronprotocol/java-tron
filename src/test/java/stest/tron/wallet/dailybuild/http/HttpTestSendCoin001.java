package stest.tron.wallet.dailybuild.http;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.junit.Assert;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestSendCoin001 {
    private final String testKey002 = Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
    private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
    private String httpnode = Configuration.getByPath("testng.conf")
        .getStringList("httpnode.ip.list").get(1);
    ECKey ecKey1= new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey1.getAddress();
    String receiverKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Long amount  = 1000L;
    private JSONObject responseContent;
    private HttpResponse response;

    /**
     * constructor.
     */
    @Test(enabled = true, description = "SendCoin by http")
    public void test1SendCoin() {
        response = HttpMethed.sendCoin(httpnode, fromAddress, receiverAddress, amount, testKey002);
        Assert.assertTrue(HttpMethed.verificationResult(response));
        HttpMethed.waitToProduceOneBlock(httpnode);
        Assert.assertEquals(HttpMethed.getBalance(httpnode, receiverAddress), amount);
    }



    /**
     * constructor.
     */
    @AfterClass
    public void shutdown() throws InterruptedException {
        HttpMethed.disConnect();
    }


}

