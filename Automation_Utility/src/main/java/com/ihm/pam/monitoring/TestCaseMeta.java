package com.ihm.pam.monitoring;

/**
 * Test case runtime metadata.
 */
public class TestCaseMeta {
    public TestCaseMeta() {
        super();
    }

    public TestCaseMeta (TestCase testCase, int code, long nextRunTime) {
        super();
        this.testCase = testCase;
        this.lastRunCode = code;
        this.nextRuntime = nextRunTime;
    }
    
    TestCase testCase;
    int lastRunCode;
    long nextRuntime = -1;
    long lastRuntime = 0;
    boolean maxRunningExceeded = false;
}
