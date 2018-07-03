package org.tron.core.services.http;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AddressPrKeyPairMessage;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;


@Component
@Slf4j
public class GenerateAddressServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] priKey = ecKey.getPrivKeyBytes();
    byte[] address = ecKey.getAddress();
    String addressStr = Wallet.encode58Check(address);
    String priKeyStr = Hex.encodeHexString(priKey);
    AddressPrKeyPairMessage.Builder builder = AddressPrKeyPairMessage.newBuilder();
    builder.setAddress(addressStr);
    builder.setPrivateKey(priKeyStr);
    response.getWriter().println(JsonFormat.printToString(builder.build()));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
  }
}