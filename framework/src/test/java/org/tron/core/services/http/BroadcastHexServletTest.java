package org.tron.core.services.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.FileUtil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class BroadcastHexServletTest {
    private BroadcastHexServlet broadcastHexServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() {
        broadcastHexServlet = new BroadcastHexServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @After
    public void tearDown() {
        if (FileUtil.deleteDir(new File("temp.txt"))) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
    }

    @Test
    public void doPostTest() throws IOException {

        //send Post request
        URL url = new URL("http://127.0.0.1:8090/wallet/broadcasttransaction");
        String postData = "{\n" +
                "\t\"transaction\":\"0A8A010A0202DB2208C89D4811359A28004098A4E0A6B52D5A730802126F0A32747970652E676F6F676C65617069732E636F6D2F70726F746F636F6C2E5472616E736665724173736574436F6E747261637412390A07313030303030311215415A523B449890854C8FC460AB602DF9F31FE4293F1A15416B0580DA195542DDABE288FEC436C7D5AF769D24206412418BF3F2E492ED443607910EA9EF0A7EF79728DAAAAC0EE2BA6CB87DA38366DF9AC4ADE54B2912C1DEB0EE6666B86A07A6C7DF68F1F9DA171EEE6A370B3CA9CBBB00\"\n" +
                "}";
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Length", "" + postData.length());
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        out.write(postData);
        out.flush();
        out.close();
        PrintWriter writer = new PrintWriter("temp.txt");
        when(response.getWriter()).thenReturn(writer);

        broadcastHexServlet.doPost(request, response);
        //Get Response Body
        String line = "";
        String result = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        while ((line = in.readLine()) != null) {
            result += line + "\n";
        }
        in.close();
        logger.info(result);
        Assert.assertTrue(result.contains("class java.lang.NullPointerException : null"));
        writer.flush();
        conn.disconnect();
    }
}

