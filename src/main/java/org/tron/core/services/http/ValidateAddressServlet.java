package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;


@Component
@Slf4j
public class ValidateAddressServlet extends HttpServlet {

  private String validAddress(String input) {
    byte[] address = null;
    boolean result = true;
    String msg;
    try {
      if (input.length() == Constant.ADDRESS_SIZE) {
        //hex
        address = ByteArray.fromHexString(input);
        msg = "Hex string format";
      } else if (input.length() == 34) {
        //base58check
        address = Wallet.decodeFromBase58Check(input);
        msg = "Base58check format";
      } else if (input.length() == 28) {
        //base64
        address = Base64.getDecoder().decode(input);
        msg = "Base64 format";
      } else {
        result = false;
        msg = "Length error";
      }
      if (result) {
        result = Wallet.addressValid(address);
        if (!result) {
          msg = "Invalid address";
        }
      }
    } catch (Exception e) {
      result = false;
      msg = e.getMessage();
    }

    JSONObject jsonAddress = new JSONObject();
    jsonAddress.put("result", result);
    jsonAddress.put("message", msg);

    return jsonAddress.toJSONString();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    String input = request.getParameter("address");
    try {
      response.getWriter().println(validAddress(input));
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONObject jsonAddress = JSON.parseObject(input);
      response.getWriter().println(validAddress(jsonAddress.getString("address")));
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
    }
  }
}