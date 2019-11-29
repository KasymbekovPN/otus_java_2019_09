package ru.otus.logging.ex03;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sergey
 * created on 13.08.18.
 */
public class LoggerRollingPolicyExample {
  private static Logger logger = LoggerFactory.getLogger("LoggerRollingPolicyExampleLogName");
  private long counter = 0;

  public static void main(String[] args) throws InterruptedException {
    new LoggerRollingPolicyExample().loop();
  }

  private void loop() throws InterruptedException {
    while (!Thread.currentThread().isInterrupted()) {
      logger.error("message for file:{}", counter);
      counter++;

      Thread.sleep(10);
    }
  }
}
