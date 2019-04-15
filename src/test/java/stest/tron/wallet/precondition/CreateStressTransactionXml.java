package stest.tron.wallet.precondition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;

@Slf4j
public class CreateStressTransactionXml {

  private String reportPath = "TransactionConfiguration.xml";
  private Integer finishedNum = 0;
  String[] stressTypeList = {
      "TriggerContract(transferTokenWithSingleSign)",
      "TransferAsset",
      "TriggerContract(transferTokenWithMultiSign)",
      "Sencoin(SingleSign)",
      "SendCoin(MultiSign)",
      "ParticipateAssetIssue",
      "FreezeBalance(DelegateNet)",
      "FreezeBalance(DelegateEnergy)",
      "ExchangeInject",
      "ExchangeWithdraw",
      "ExchangeTransaction",
      "TriggerContract(timeout)",
      "UnFreezeBalance(DelegateNet)",
      "UnFreezeBalance(DelegateEnergy)",
      "createAccount",
      "voteWitness",
      "witnessUpdate",
      "updateAsset",
      "deployContract",
      "updateSetting",
      "exchangeCreate",
      "proposalCreate",
      "updateEnergyLimit",
      "triggerTimeoutContractCreatorMulti",
      "SendCoin(Delay)"

  };

  @BeforeClass
  public void beforeClass() {
    beforeWrite();

  }


  @Test(enabled = true)
  public void createStressTransactionXml() {
    HashMap<String, Integer> stressType = new HashMap<String, Integer>();
    for (String key : stressTypeList) {
      try {
        if (Configuration.getByPath("stress.conf").getInt("stressType." + key) > 0) {
          stressType.put(Configuration.getByPath("stress.conf").getString("type2IdName." + key),
              Configuration.getByPath("stress.conf").getInt("stressType." + key));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

    writeRef(stressType);
    writeDetail(stressType);

  }

  @AfterClass
  public void afterClasss() {
    afterWrite();
  }


  public void beforeWrite() {
    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n"
        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
        + "  xmlns:context=\"http://www.springframework.org/schema/context\"\n"
        + "  xsi:schemaLocation=\"http://www.springframework.org/schema/beans\n"
        + "       http://www.springframework.org/schema/beans/spring-beans.xsd\n"
        + "       http://www.springframework.org/schema/context\n"
        + "       http://www.springframework.org/schema/context/spring-context.xsd\">\n"
        + "  <context:component-scan base-package=\"org.tron\"/>\n"
        + "\n"
        + "  <bean id=\"creatorCounter\" class=\"org.tron.stresstest.dispatch.creator.CreatorCounter\"></bean>\n"
        + "  <bean id=\"dispatcher\" class=\"org.tron.stresstest.dispatch.strategy.Dispatcher\">\n"
        + "    <property name=\"source\">\n"
        + "      <list>\n"
        + "        <ref bean=\"mutil\"/>\n"
        + "      </list>\n"
        + "    </property>\n"
        + "  </bean>\n"
        + "  <bean id=\"level1Strategy\" class=\"org.tron.stresstest.dispatch.strategy.Level1Strategy\"/>\n"
        + "  <bean id=\"mutil\" parent=\"level1Strategy\">\n"
        + "    <property name=\"name\" value=\"mutil\"/>\n"
        + "    <property name=\"begin\" value=\"0\"/>\n"
        + "    <property name=\"end\" value=\"99\"/>\n"
        + "    <property name=\"source\">\n");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void afterWrite() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n</beans>");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void writeDetail(HashMap<String, Integer> stressType) {
    StringBuilder sb = new StringBuilder();
    for (String key : stressType.keySet()) {
      sb.append("<bean id=\"" + key + "\" class=\"" + Configuration.getByPath("stress.conf")
          .getString("className." + key) + "\">\n");
      sb.append("<property name=\"name\" value=\"" + key + "\"/>\n");
      sb.append("<property name=\"begin\" value=\"" + finishedNum + "\"/>\n");
      finishedNum = finishedNum + stressType.get(key) - 1;
      sb.append("<property name=\"end\" value=\"" + finishedNum + "\"/>\n");
      sb.append("</bean>\n");
      finishedNum++;
    }

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void writeRef(HashMap<String, Integer> stressType) {
    StringBuilder sb = new StringBuilder();
    sb.append("  <list>\n");

    for (String key : stressType.keySet()) {
      //String id = Configuration.getByPath("stress.conf").getString("type2IdName." + key);
      sb.append("<ref bean=\"" + key + "\"/>\n");
    }

    sb.append("      </list>\n" + "    </property>\n" + "  </bean>\n\n");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}




