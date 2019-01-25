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
    StringBuilder sb = new StringBuilder("Daily Build for stest report:\n");
    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)),res.getBytes("utf-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    passedDescriptionList.append(result.getMethod().getDescription() + "\n");
    passedNum++;
  }

  @Override
  public void onTestFailure(ITestResult result) {
    failedNum++;
    //StringBuilder sb = new StringBuilder();
    failedDescriptionList.append(result.getMethod().getDescription() + " in "
        + result.getMethod().getRealClass() + "\n");
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    skippedNum++;
    skippedDescriptionList.append(result.getMethod().getDescription() + " in "
        + result.getMethod().getRealClass() + "\n");
  }



  @Override
  public void onFinish(ITestContext testContext) {
    StringBuilder sb = new StringBuilder();
    sb.append("-------------------------------------------------\n");
    sb.append("Total test case number: " + (passedNum + failedNum + skippedNum) + "\n");
    sb.append("-------------------------------------------------\n");
    sb.append("Passed case number: " + passedNum + "\n");
    //sb.append("Passed case List: " + "\n");
    sb.append(passedDescriptionList.toString());
    sb.append("-------------------------------------------------\n");
    sb.append("Failed case number: " + failedNum + "\n");
    //sb.append("Failed case List: " + "\n");
    sb.append(failedDescriptionList.toString());
    sb.append("-------------------------------------------------\n");
    sb.append("Skipped case number: " + skippedNum + "\n");
    //sb.append("Skipped case List: " + "\n");
    sb.append(skippedDescriptionList.toString());
    sb.append("-------------------------------------------------\n");

    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)),res.getBytes("utf-8"),StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}

