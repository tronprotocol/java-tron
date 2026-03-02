package org.tron.common.prometheus;

import io.prometheus.client.Histogram;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MetricAspect {

  @Around("@annotation(metricTime)")
  public Object aroundAdviceMetricTime(ProceedingJoinPoint pjp, MetricTime metricTime)
      throws Throwable {
    Object result;
    Histogram.Timer requestTimer = Metrics.histogramStartTimer(
        MetricKeys.Histogram.INTERNAL_SERVICE_LATENCY,
        pjp.getSignature().getDeclaringType().getSimpleName(),
        metricTime.value().isEmpty() ? pjp.getSignature().getName() : metricTime.value());
    try {
      result = pjp.proceed();
    } catch (Throwable throwable) {
      Metrics.counterInc(
          MetricKeys.Counter.INTERNAL_SERVICE_FAIL, 1,
          pjp.getSignature().getDeclaringType().getSimpleName(),
          metricTime.value().isEmpty() ? pjp.getSignature().getName() : metricTime.value());
      throw throwable;
    } finally {
      Metrics.histogramObserve(requestTimer);
    }
    return result;
  }

  @Around("execution(public * org.tron.core.Wallet.*(..))")
  public Object walletAroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
    Object result;
    Histogram.Timer requestTimer = Metrics.histogramStartTimer(
        MetricKeys.Histogram.INTERNAL_SERVICE_LATENCY,
        pjp.getSignature().getDeclaringType().getSimpleName(),
        pjp.getSignature().getName());
    try {
      result = pjp.proceed();
    } catch (Throwable throwable) {
      Metrics.counterInc(
          MetricKeys.Counter.INTERNAL_SERVICE_FAIL, 1,
          pjp.getSignature().getDeclaringType().getSimpleName(),
          pjp.getSignature().getName());
      throw throwable;
    } finally {
      Metrics.histogramObserve(requestTimer);
    }
    return result;
  }
} 