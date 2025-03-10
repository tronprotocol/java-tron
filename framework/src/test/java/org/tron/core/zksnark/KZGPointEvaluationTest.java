package org.tron.core.zksnark;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.zen.KZGPointEvaluationInitService;

@Slf4j
public class KZGPointEvaluationTest extends BaseTest {

  @Test
  public void testKZGPointEvaluation() {
    KZGPointEvaluationInitService.freeSetup();

    KZGPointEvaluationInitService.initCKZG4844();




  }
}
