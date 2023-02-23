package com.ihm.pam.monitoring;


import com.kpmg.ihm.pam.ConfigUtils;
import com.kpmg.ihm.pam.Utils;
import com.ihm.pam.monitoring.script.RegisterNewServer;
import com.ihm.pam.monitoring.script.ScriptMaster;

import com.ihm.pam.monitoring.script.ScriptResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.net.InetAddress;

import java.text.SimpleDateFormat;

import java.time.Instant;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The master daemon thread.
 */
public class MonitoringMaster implements Runnable {
    private MonitoringMaster() {
        super();
    }

    private static final Logger LOGGER = Logger.getLogger(MonitoringMaster.class.getName());

    private static Map<String, TestCaseMeta> providers = new HashMap<String, TestCaseMeta>();

    private static long sleepTime = 500L; //millsecond
    private static long defaultIntervalInSecond = 300L; //second

    private static ExecutorService executor = null;

    private static volatile long lastRunTimestamp = 0;

    private static int state = 0;

    private static long peerDownDetectedTime = 0;

    private static String localHostName = null;

    private static String alertFileBase = null;

    public static int getState() {
        return state;
    }

    public static void setState(int s) {
        state = s;
    }

    public static long getLastRunTimestamp() {
        return lastRunTimestamp;
    }

    public static void main(String[] args) {
        start();

    }


    public static String generateAlert() {

        if (localHostName == null) {
            alertFileBase = ConfigUtils.getString("monitoring.alertFileBase");
            try {
                InetAddress ip = InetAddress.getLocalHost();
                localHostName = ip.getHostName();
            } catch (Exception e) {
                localHostName = "localhost";
            }
        }

        boolean sysbadIn = false;
        
        String ts = new SimpleDateFormat("yy-MM-dd_HH-mm-ss").format(new Date());
        String sumFileName = alertFileBase + "/Summary_PAM_" + localHostName + "_" + ts + ".txt";
        String detailFileName = alertFileBase + "/Detail_PAM_" + localHostName + "_" + ts + ".txt";

        StringBuilder summary = new StringBuilder();
        StringBuilder detail = new StringBuilder();

        for (Map.Entry<String, TestCaseMeta> entry : providers.entrySet()) {
            try {
                String name = entry.getKey();
                TestCaseMeta meta = entry.getValue();

                TestResult lastResult = null;
                lastResult = meta.testCase.getResult();

                if (lastResult == null) {
                    lastResult = new TestResult();
                    lastResult.setCode(0);
                    lastResult.setMessage("Waiting for 1st run completes ...");
                    lastResult.setSeverity(Severity.NONE);
                }

                Severity sev = lastResult.getSeverity();
                detail.append("TaskName=")
                      .append(name).append("||TaskType=Monitoring||Severity=");
                if (sev == Severity.NONE) {
                    detail.append("OK").append("||TestOutcome= Successful");
                } else {
                    detail.append(sev)
                          .append("||TestOutcome=Failure||code=")
                          .append(lastResult.getCode())
                          .append("||msg=")
                          .append(lastResult.getMessage()
                                            .replaceAll("<br>", "#")
                                            .replaceAll("\\|\\|", "**"));
                }

                detail.append("\n");
                if (meta.maxRunningExceeded) {
                    detail.append("TaskName=")
                          .append(name + "_m")
                          .append("||TaskType=Monitoring||severity=")
                          .append("MEDIUM||TestOutcome=Failure||code=1||msg=Max execution time excedded!\n");
                }

                summary.append(name).append(":");
                if ((sev == Severity.NONE || sev == Severity.LOW) && !meta.maxRunningExceeded) {
                    summary.append("GOOD\n");
                } else {
                    summary.append("BAD\n");
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in testing!", e);
                detail.append("Monitoring System:HIGH:1:Unexpected error: " + e.getMessage() + "\n");
                if (!sysbadIn) {
                    summary.append("Monitoring System::BAD\n");
                    sysbadIn = true;
                }
            }

        }


        List<ScriptResult> scrptResults = ScriptMaster.clear();
        Set<String> goodset = new HashSet<String>();
        Set<String> badset = new HashSet<String>();

        for (ScriptResult lastResult : scrptResults) {
            try {

                Severity sev = lastResult.getSeverity();
                String name = lastResult.getName();

                detail.append("TaskName=")
                      .append(name).append("||InstanceID=").append(lastResult.getInstanceId())
                      .append("||Action=").append(lastResult.getAction())
                      .append("||TaskType=Script||severity=");
                if (sev == Severity.NONE) {
                    detail.append("OK||TestOutcome=Successful");
                } else {
                    detail.append(sev)
                          .append("||TestOutcome=Failure||code=")
                          .append(lastResult.getCode())
                          .append("||msg=")
                          .append(lastResult.getMessage()
                                            .replaceAll("<br>", "#")
                                            .replaceAll("\\|\\|", "**"));
                }

                detail.append("\n");
                
                if ((sev == Severity.NONE || sev == Severity.LOW)) {
                    goodset.add(name);
                } else {
                    badset.add(name);
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in testing!", e);
                detail.append("Monitoring System:HIGH:1:Unexpected error: " + e.getMessage() + "\n");
                if (!sysbadIn) {
                    summary.append("Monitoring System::BAD\n");
                    sysbadIn = true;
                }
            }
        }

        for(String name : goodset){
            summary.append(name).append(":").append("GOOD\n");
        }
        
        for(String name : badset) {
            summary.append(name).append(":").append("BAD\n");
        }

        BufferedWriter sumWriter = null;
        try {
            sumWriter = new BufferedWriter(new FileWriter(sumFileName));
            sumWriter.write(summary.toString());

        } catch (Throwable th) {
            throw new RuntimeException(th);
        } finally {
            if (sumWriter != null) {
                try {
                    sumWriter.close();
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, "Failed close file " + sumFileName, ioe);
                }
            }
        }


        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(detailFileName));
            writer.write(detail.toString());

        } catch (Throwable th) {
            throw new RuntimeException(th);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, "Failed close file " + detailFileName, ioe);
                }
            }
        }

        return sumFileName + "|" + detailFileName;
    }


    public static String getHTMLReport() {
        if (state == 0) {
            return "Monitoring service staring in HA mode, ping peer to determin primary or standby, this may take up to 1 - 2 min";
        } else if (state == 2) {
            return "Server is in standby mode!";
        }


        StringBuilder html = new StringBuilder();
        html.append("<ul class=\"list-group\">");
        LOGGER.info("start report generation ....");
        Severity[] sevs = Severity.values();
        HashMap<String, TestResult> tmp = new HashMap<String, TestResult>();
        for (int i = sevs.length - 1; i >= 0; i--) {

            for (Map.Entry<String, TestCaseMeta> entry : providers.entrySet()) {
                try {
                    String name = entry.getKey();
                    TestCaseMeta meta = entry.getValue();

                    TestResult lastResult = null;
                    if (i == sevs.length - 1) { //to gurantee the sev order
                        lastResult = meta.testCase.getResult();
                        if (lastResult != null)
                            tmp.put(name, lastResult);
                    } else
                        lastResult = tmp.get(name);

                    if (lastResult == null && i > 0 || lastResult != null && lastResult.getSeverity() != sevs[i])
                        continue; //so that sort high to low
                    if (lastResult == null && i == 0) {
                        lastResult = new TestResult();
                        lastResult.setCode(0);
                        lastResult.setMessage("Waiting for 1st run completes ...");
                        lastResult.setSeverity(Severity.NONE);
                    }

                    html.append("<li class=\"list-group-item\">");
                    String colorClass = null;
                    if (lastResult != null &&
                        (lastResult.getSeverity() == Severity.CRITICAL || lastResult.getSeverity() == Severity.HIGH ||
                         lastResult.getSeverity() == Severity.MEDIUM)) {
                        colorClass = "text-danger";
                    } else if (meta.maxRunningExceeded ||
                               lastResult != null && lastResult.getSeverity() == Severity.LOW) {
                        colorClass = "text-warning";
                    } else {
                        colorClass = "text-success";
                    }
                    html.append("<p class=\"" + colorClass + "\">");

                    html.append("<h2><p class='")
                        .append(colorClass)
                        .append("'>")
                        .append(name);
                    if (meta.maxRunningExceeded)
                        html.append(" [WARNING: max execution time exceeded!]");
                    html.append("</p></h2>");
                    html.append("<table class=\"table " + colorClass + "\">");

                    html.append("<tr><td width='300px'>Desc</td>");
                    html.append("<td>")
                        .append(meta.testCase.getDescription())
                        .append("</td></tr>");

                    html.append("<tr><td>Status</td>");
                    html.append("<td>")
                        .append(meta.testCase.getRunningState() == RunningState.RUNNING ?
                                "Runing test ...<br>" + meta.testCase.getCurrentMessage() : "Waiting for next run ...")
                        .append("</td></tr>");

                    html.append("<tr><td>Interval</td>");
                    long interval = meta.testCase.getIntervalInSecond();
                    if (interval < 0)
                        interval = defaultIntervalInSecond;
                    String t = formatSecond(interval);
                    html.append("<td>")
                        .append(t)
                        .append("</td></tr>");

                    html.append("<tr><td>Next Run</td>");
                    long dif = meta.nextRuntime - System.currentTimeMillis();
                    dif = dif / 1000L;
                    String s = "In " + formatSecond(dif);
                    html.append("<td>")
                        .append(s)
                        .append("</td></tr>");


                    if (lastResult == null) {
                        html.append("<tr><td>Last Run></td><td>Not available</td></tr>");
                    } else {
                        html.append("<tr><td>Last Run Code</td>");
                        html.append("<td>")
                            .append(lastResult.getCode())
                            .append("</td></tr>");

                        html.append("<tr><td>Last Run Time</td>");
                        LocalDateTime lt =
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(meta.lastRuntime),
                                                    TimeZone.getDefault().toZoneId());

                        String time = lt.toString();
                        time = time.replace("T", " ");
                        time = time.substring(0, time.lastIndexOf('.'));
                        html.append("<td>")
                            .append(time)
                            .append("</td></tr>");

                        html.append("<tr><td>Last Run Severity</td>");
                        html.append("<td>")
                            .append(lastResult.getSeverity())
                            .append("</td></tr>");

                        if (lastResult.getMessage() != null && lastResult.getMessage().length() > 0) {
                            html.append("<tr><td>Last Run Message</td>");
                            html.append("<td><div class='overflow-auto'>")
                                .append(lastResult.getMessage())
                                .append("</div></td></tr>");
                        }
                    }

                    html.append("</table></p></li>");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error in testing!", e);
                }
            }
        } //each sev
        html.append("</ul>");
        LOGGER.info("ended report generation ....");
        return html.toString();
    }


    private static String formatSecond(long t) {
        long h = t / 3600L;
        long m = (t % 3600L) / 60L;
        long s = t % 60L;

        if (h > 0) {
            return (new StringBuilder().append(h)
                                       .append(" hour(s) ")
                                       .append(m)
                                       .append(" minute(s) ")
                                       .append(s)
                                       .append(" second(s)")).toString();
        } else if (m > 0) {
            return (new StringBuilder().append(m)
                                       .append(" minute(s) ")
                                       .append(s)
                                       .append(" second(s)")).toString();
        } else {
            return (new StringBuilder().append(s).append(" second(s)")).toString();
        }

    }

    public static void start() {
        Thread s = new Thread(new MonitoringMaster());
        s.setDaemon(true);
        s.start();
    }

    public void run() {

        executor = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        });
        String defaultIntervalStr =
            ConfigUtils.getString("monitoring.defaultIntervalInSecond", String.valueOf(defaultIntervalInSecond));

        try {
            defaultIntervalInSecond = Long.parseLong(defaultIntervalStr);
        } catch (NumberFormatException nfe) {
            // TODO: Add catch code
            LOGGER.severe("Invalid integer value for monitoring.testcase.defaultIntervalInSecond " +
                          defaultIntervalStr);
            throw nfe;
        }


        while (true) {
            if (state == 1) {
                try {
                    Map<String, String> testCaseCfgs = ConfigUtils.getSectionAsMap("monitoring.testcase.");

                    //cleanup removed
                    Map<String, TestCaseMeta> tmp = new HashMap<String, TestCaseMeta>(providers);
                    tmp.keySet().retainAll(testCaseCfgs.keySet());
                    providers = tmp;

                    for (Map.Entry<String, String> entry : testCaseCfgs.entrySet()) {
                        String name = entry.getKey();
                        String clsStr = entry.getValue();

                        TestCaseMeta testCaseMeta = providers.get(name);

                        if (testCaseMeta == null) { //if first time to load a test
                            try {

                                String[] args = null;
                                int pp = clsStr.indexOf('(');
                                if (pp > 0) {
                                    if (clsStr.endsWith(")")) {
                                        args = clsStr.substring(pp + 1, clsStr.length() - 1).split(",");
                                        clsStr = clsStr.substring(0, pp);
                                    } else {
                                        throw new RuntimeException(clsStr + " is invalid class configuration!");
                                    }
                                } else if (pp == 0) {
                                    throw new RuntimeException(clsStr + " is invalid class configuration!");
                                }


                                TestCase testCase = (TestCase) Class.forName(clsStr).newInstance();
                                testCase.init(args);
                                testCase.setName(name);
                                long now = System.currentTimeMillis();
                                long interval = testCase.getIntervalInSecond();
                                if (interval < 0)
                                    interval = defaultIntervalInSecond;
                                testCaseMeta = new TestCaseMeta(testCase, 0, now + interval * 1000L);

                                tmp = new HashMap<String, TestCaseMeta>(providers);
                                tmp.put(name, testCaseMeta);
                                providers = tmp;

                                testCaseMeta.lastRuntime = now;
                                executor.submit(testCase);


                            } catch (Exception e) {
                                LOGGER.log(Level.SEVERE, "Error in class creation!", e);
                                throw new RuntimeException(e); //stop the process now to resolve the issue.
                            }
                        } else { //
                            TestCase testCase = testCaseMeta.testCase;
                            RunningState state = testCase.getRunningState();
                            long now = System.currentTimeMillis();

                            if (state == RunningState.RUNNING) {
                                if (!testCaseMeta.maxRunningExceeded && testCase.getMaxRunningTimeInMinute() > 0) {
                                    if (now - testCaseMeta.lastRuntime > testCase.getMaxRunningTimeInMinute() * 60000) {
                                        LOGGER.warning("Max running time exceeded for " + name);
                                        TestResult maxRunningExceededMsg = new TestResult();
                                        maxRunningExceededMsg.setCode(1);
                                        maxRunningExceededMsg.setMessage("Max running time exceeded for " + name);
                                        maxRunningExceededMsg.setSeverity(Severity.LOW);
                                        //EMAIL
                                        //SendEmail.sendEmai(name, maxRunningExceededMsg);
                                        testCaseMeta.maxRunningExceeded = true;
                                    }
                                }
                                continue; //next one
                            }
                            TestResult result = testCase.getResult();
                            if (result != null) {
                                int code = result.getCode();
                                if (code != testCaseMeta.lastRunCode) {
                                    testCaseMeta.lastRunCode = code;
                                    if (code != 0 ||
                                        code == 0 &&
                           testCase.notifyWhenGobackNormal()) {
                                        //EMAIL
                                        //SendEmail.sendEmai(name, testResult);
                                        System.out.println(name + " : Sending email [" + result.getSeverity() + ":" +
                                                           result.getMessage() + "]");

                                    }
                                }
                            }

                            if (now >= testCaseMeta.nextRuntime) { //run test if next runtime reached.
                                long interval = testCase.getIntervalInSecond();
                                if (interval < 0)
                                    interval = defaultIntervalInSecond;
                                testCaseMeta.nextRuntime = now + interval * 1000L;
                                testCaseMeta.lastRuntime = now;
                                executor.submit(testCase);
                                testCaseMeta.maxRunningExceeded = false;
                            }

                        }

                    } //each test case

                    lastRunTimestamp = System.currentTimeMillis();

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        LOGGER.log(Level.SEVERE, "Unexpected error from thread.sleep!", ie);
                    }
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "Unexpected error happened during testing!", t);
                }
            } else if (state == 2) {
                try {
                    String peerState = "-1";
                    try {
                        peerState = MonitoringUtils.getPeerStatus();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error during peer ping, start self as active!", e);
                    }
                    if ("-1".equals(peerState)) {
                        LOGGER.severe("Get state -1 (down) from peer!");
                        if (peerDownDetectedTime == 0)
                            peerDownDetectedTime = System.currentTimeMillis();
                        else {
                            if (System.currentTimeMillis() - peerDownDetectedTime > 300000) {
                                LOGGER.warning("Primary peer is not reachable for 5 min, promote self to primary!");
                                peerDownDetectedTime = 0;
                                state = 1;
                            }
                        }
                    } else {
                        peerDownDetectedTime = 0; //reset
                    }

                    if ("2".equals(peerState)) {
                        LOGGER.warning("Get state 2 from peer, set this server to primary!");
                        state = 1;
                    }

                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ie) {
                        LOGGER.log(Level.SEVERE, "Unexpected error from thread.sleep!", ie);
                    }
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "Unexpected error happened during ping primary!", t);
                }
            }

        } //while


    } //method


}
