package stest.tron.wallet.common.client.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class exportTransactionConfiguration extends TestListenerAdapter {

  private Integer passedNum = 0;
  private Integer failedNum = 0;
  private Integer skippedNum = 0;
  private String reportPath;
  StringBuilder passedDescriptionList = new StringBuilder("");
  StringBuilder failedDescriptionList = new StringBuilder("");
  StringBuilder skippedDescriptionList = new StringBuilder("");

  @Override
  public void onStart(ITestContext context) {
    reportPath = "TransactionConfiguration_test.xml";
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

  @Override
  public void onTestSuccess(ITestResult result) {
    passedDescriptionList.append(result.getMethod().getRealClass() + ": "
        + result.getMethod().getDescription() + "\n");
    passedNum++;
  }

  @Override
  public void onTestFailure(ITestResult result) {
    failedDescriptionList.append(result.getMethod().getRealClass() + ": "
        + result.getMethod().getDescription() + "\n");
    failedNum++;
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    skippedDescriptionList.append(result.getMethod().getRealClass() + ": "
        + result.getMethod().getDescription() + "\n");
    skippedNum++;
  }


  @Override
  public void onFinish(ITestContext testContext) {
    StringBuilder sb = new StringBuilder();
    sb.append("\n</beans>");


    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}

