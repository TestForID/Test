package com.ihm.pam.monitoring;

/**
 * Test result.
 */
public class TestResult {
    public TestResult() {
        super();
    }

    private int code;  //0 means no error
    private String message;
    private Severity severity;

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }


    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }
}
