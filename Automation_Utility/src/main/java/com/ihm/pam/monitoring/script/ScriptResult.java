package com.ihm.pam.monitoring.script;

import com.ihm.pam.monitoring.TestResult;

public class ScriptResult extends TestResult {
    public ScriptResult() {
        super();
    }
    private String name;
    private String instanceId = null;
    private String action = null;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
