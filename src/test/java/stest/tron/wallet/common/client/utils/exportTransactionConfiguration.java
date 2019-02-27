package stest.tron.wallet.common.client.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import stest.tron.wallet.common.client.Configuration;
@Slf4j
public class exportTransactionConfiguration extends TestListenerAdapter {

  private String reportPath = "TransactionConfiguration_test2.xml";
  private Integer finishedNum = 0;
  private String type2IdName;
  private String idValue;

  String[] stressTypeList = {
      "TriggerContract(transferTokenWithSingleSign)",
      "TransferAsset"
  };


  @Override
  public void onStart(ITestContext context) {
    beforeWrite();
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    HashMap<String, Integer> stressType = new HashMap<String, Integer>();
    for (String key: stressTypeList) {
      if (Configuration.getByPath("stress.conf").getInt("stressType." + key) > 0) {
        stressType.put(Configuration.getByPath("stress.conf").getString("type2IdName." + key),
            Configuration.getByPath("stress.conf").getInt("stressType." + key));
      }
    }
    writeRef(stressType);
  }


  @Override
  public void onFinish(ITestContext testContext) {
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
        + "    <property name=\"source\">\n"
        + "      <list>");
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

  public void writeStressContent(String id,Integer percentage) {
    StringBuilder sb = new StringBuilder();
    sb.append("<bean id=\"");
    sb.append(id + "\"\n");
    sb.append("class=" + getClassName(id) + "\">\n");
    sb.append("<property name=\"name\" value=\"");
    sb.append(id +"\"/>\n");
    sb.append("<property name=\"begin\" value=\"" + finishedNum + "\"/>\n");
    finishedNum = finishedNum + percentage;
    sb.append("<property name=\"end\" value=\"" + finishedNum + "\"/>\n");
    sb.append("  </bean>\n\n");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getClassName(String id) {
    String idName = "className." + id;
    String description = Configuration.getByPath("stress.conf")
        .getString(idName);
    //logger.info(description);
    return description;
  }

  public void writeRef(HashMap<String, Integer> stressType) {
    StringBuilder sb = new StringBuilder();
    sb.append("  <list>\n");

    for (String key : stressType.keySet()) {
      String id = Configuration.getByPath("stress.conf").getString("type2IdName." + key);
      sb.append("<ref bean=\"" + id + "\"/>\n");
    }

    sb.append("      </list>\n" + "    </property>\n" + "  </bean>");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }



}

