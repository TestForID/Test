package com.ihm.pam.monitoring.script;

import java.util.ArrayList;
import java.util.List;

public class ScriptMaster {
    public ScriptMaster() {
        super();
    }
    
    private static List<ScriptResult> results = new ArrayList<ScriptResult>(); 
    
    public static synchronized List<ScriptResult> clear() {
        List<ScriptResult> tmp = results;
        results = new ArrayList<ScriptResult>();
        return tmp;
    }
    
    
    public static synchronized void add(ScriptResult result) {
        results.add(result);
    }    
}
