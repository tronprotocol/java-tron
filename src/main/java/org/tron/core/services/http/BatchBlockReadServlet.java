package org.tron.core.services.http;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;


@Component
@Slf4j(topic = "API")
public class BatchBlockReadServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean visible = Util.getVisible(request);
            long start = Long.parseLong(request.getParameter("num"));
            long count = Long.parseLong(request.getParameter("count"));

            JSONArray blocks = new JSONArray();
            for(long i = 0; i<count;i++){
                Block reply = wallet.getBlockByNum(start+i);
                if (reply != null) {
                    blocks.add(printBlockToJSON(reply, visible));
                }
            }

            response.getWriter().println(blocks.toJSONString());

        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            try {
                response.getWriter().println(Util.printErrorMsg(e));
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
        }
    }


    private JSONObject printBlockToJSON(Block block, boolean selfType) {
        BlockCapsule blockCapsule = new BlockCapsule(block);
        String blockID = ByteArray.toHexString(blockCapsule.getBlockId().getBytes());
        JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(block, selfType));
        jsonObject.put("blockID", blockID);
        if (!blockCapsule.getTransactions().isEmpty()) {
            JSONArray transactions = new JSONArray();
            for(TransactionCapsule tc : blockCapsule.getTransactions()){
                Protocol.Transaction transaction = tc.getInstance();
                Protocol.TransactionInfo transactionInfo = wallet.getTransactionInfoById(ByteString.copyFrom(
                        Sha256Hash.hash(transaction.getRawData().toByteArray())));

                JSONObject jsonTx = new JSONObject();


                jsonTx.put("request", Util.printTransactionToJSON(transaction, selfType));

                if(transactionInfo!=null){
                    JSONObject jsonTransactionInfo = JSONObject.parseObject(JsonFormat.printToString(transactionInfo,
                            selfType));

                    jsonTx.put("response", jsonTransactionInfo);
                }else{
                    jsonTx.put("response", new JSONObject());
                }

                transactions.add(jsonTx);
            }
            jsonObject.put("transactions", transactions);
        }
        return jsonObject;
    }



}