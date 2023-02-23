package com.ihm.pam.monitoring;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;

import com.jcraft.jsch.Session;
import com.kpmg.ihm.pam.ConfigUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;

import java.net.URLConnection;
import java.net.URLEncoder;

import java.sql.Timestamp;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;


/**
 * Monitoring Utilities
 */
public class MonitoringUtils {
    private MonitoringUtils() {
        super();
    }

    private static String peerURL = null;
    private static String reportURL = null;

    private static final Logger LOGGER = Logger.getLogger(MonitoringUtils.class.getName());

    public static String getPeerStatus() {

        if (peerURL == null) {
            peerURL = ConfigUtils.getString("monitoring.HA.peerServer");

        }

        try {
            URL url = new URL(peerURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();


            con.setConnectTimeout(30000); //timeout 30 second
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return content.toString().trim();

        } catch (SocketTimeoutException te) {
            return "-1"; //timeout
        } catch (ConnectException ce) {
            return "-1"; //conn refused
        } catch (MalformedURLException murle) {
            LOGGER.log(Level.SEVERE, "Invalid URL" + peerURL, murle);
            throw new RuntimeException(murle);
        } catch (Throwable ta) {
            LOGGER.log(Level.SEVERE, "Unexpected error when connect to " + peerURL, ta);
            throw new RuntimeException(ta);
        }
    }


    public static String getClientIp(HttpServletRequest request) {

        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getParameter("ip");
            if (remoteAddr == null) {
                remoteAddr = request.getHeader("X-FORWARDED-FOR");
                if (remoteAddr == null || "".equals(remoteAddr)) {
                    remoteAddr = request.getRemoteAddr();
                }
            }
        }

        return remoteAddr;
    }

    public static String findHostFromIp(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.getHostName();
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }


    public static String triggerIPSoftReport() {


        if (reportURL == null) {
            reportURL = ConfigUtils.getString("monitoring.ipsoftreport.url");

        }

        try {
            URL url = new URL(reportURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();


            con.setConnectTimeout(30000); //timeout 30 second
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return content.toString().trim();

        } catch (SocketTimeoutException te) {
            return "-1"; //timeout
        } catch (ConnectException ce) {
            return "-1"; //conn refused
        } catch (MalformedURLException murle) {
            LOGGER.log(Level.SEVERE, "Invalid URL" + reportURL, murle);
            throw new RuntimeException(murle);
        } catch (Throwable ta) {
            LOGGER.log(Level.SEVERE, "Unexpected error when connect to " + reportURL, ta);
            throw new RuntimeException(ta);
        }


    }


    public static void testSSH(String host, String user, String password) {
        String command1 = "ls -latr";
        try {

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();

            /*
        System.out.println("Connected");

        Channel channel=session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command1);
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);

        InputStream in=channel.getInputStream();
        channel.connect();
        byte[] tmp=new byte[1024];
        while(true){
          while(in.available()>0){
            int i=in.read(tmp, 0, 1024);
            if(i<0)break;
            System.out.print(new String(tmp, 0, i));
          }
          if(channel.isClosed()){
            System.out.println("exit-status: "+channel.getExitStatus());
            break;
          }
          try{Thread.sleep(1000);}catch(Exception ee){}
        }
        channel.disconnect();
*/
            session.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static long getTime(String s) {
        try {
            Date parsedDate = dateFormat.parse(s);
            return parsedDate.getTime();
        } catch (Exception e) { //this generic but you can control another types of exception
            // look the origin of excption
            throw new RuntimeException(e);
        }
    }

}


