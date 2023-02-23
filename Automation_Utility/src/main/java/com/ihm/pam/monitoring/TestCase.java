package com.ihm.pam.monitoring;

import basicpbpsdemo.Client;

/**
 * Test case imlemented methods
 */
public interface TestCase extends Runnable {
    void init(String[] args);
    String getDescription();
    boolean notifyWhenGobackNormal();
    RunningState getRunningState();
    void setName(String name);
    TestResult getResult();
    long getIntervalInSecond();  
    String getCurrentMessage();
    long getMaxRunningTimeInMinute();
}
