package stest.tron.wallet.common.client.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class DailyBuildReport extends TestListenerAdapter {

  private Integer passedNum = 0;
  private Integer failedNum = 0;
  private Integer skippedNum = 0;
  private String reportPath;
  StringBuilder passedDescriptionList = new StringBuilder("");
  StringBuilder failedDescriptionList = new StringBuilder("");
  StringBuilder skippedDescriptionList = new StringBuilder("");

  @Override
  public void onStart(ITestContext context) {
    reportPath = "Daily_Build_Report";
    StringBuilder sb = new StringBuilder("3.Stest report:  ");
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
    sb.append("Total: " + (passedNum + failedNum + skippedNum) + ",  " + "Passed: " + passedNum
        + ",  " + "Failed: " + failedNum + ",  " + "Skipped: " + skippedNum + "\n");
    sb.append("------------------------------------------------------------------------------\n");
    sb.append("Passed list " + "\n");
    //sb.append("Passed case List: " + "\n");
    sb.append(passedDescriptionList.toString());
    sb.append("------------------------------------------------------------------------------\n");
    sb.append("Failed list: " + "\n");
    //sb.append("Failed case List: " + "\n");
    sb.append(failedDescriptionList.toString());
    sb.append("------------------------------------------------------------------------------\n");
    sb.append("Skipped list: " + "\n");
    //sb.append("Skipped case List: " + "\n");
    sb.append(skippedDescriptionList.toString());
    sb.append("----------------------------------------------------------------\n");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)), res.getBytes("utf-8"), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}

