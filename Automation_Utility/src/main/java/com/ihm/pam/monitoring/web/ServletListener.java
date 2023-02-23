package com.ihm.pam.monitoring.web;


import com.ihm.pam.monitoring.MonitoringMaster;
import com.ihm.pam.monitoring.MonitoringUtils;
import com.kpmg.ihm.pam.ConfigUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * listener that init the startup process.
 */
public class ServletListener implements ServletContextListener {
    
    private static final Logger LOGGER = Logger.getLogger(ServletListener.class.getName());
    
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
    }

    //Run this before web application is started
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        Thread t = new Thread(new PeerChecker());
        t.start();
    }
    
    
    public static class PeerChecker implements Runnable {
        public void run() {
            String peerUrl = ConfigUtils.getString("monitoring.HA.peerServer", null);
            if (peerUrl == null) {
                LOGGER.info("No peer sever configured, start standalong mode!");
                MonitoringMaster.setState(1);
                MonitoringMaster.start();
                return;
            }
            
            LOGGER.info("Peer sever configured, ping peer " + peerUrl + " to determine primary or stand by ...");
            
            
            String state = "-1";
            try {
                Thread.sleep(60000);  //sleep 60 second to avoid peer competition
                state = MonitoringUtils.getPeerStatus();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during peer ping, start self as active!", e);        
            }
            if ("-1".equals(state)) {
                LOGGER.severe("Get state -1 (down) from peer, set this server primary!");
                MonitoringMaster.setState(1);  //make this priary
            } else if ("0".equals(state)) {
                MonitoringMaster.setState("Y".equalsIgnoreCase(ConfigUtils.getString("monitoring.isPrimary")) ? 1 : 2);  //make this priary
                LOGGER.warning("Get state 0 from peer, set this server to default " + (MonitoringMaster.getState() == 1 ? "primary" : "standby"));
            } else if ("1".equals(state)) {
                LOGGER.warning("Get state 1 from peer, set this server standby!");
                MonitoringMaster.setState(2);
            } else if ("2".equals(state)) {
                LOGGER.warning("Get state 2 from peer, set this server primary!");
                MonitoringMaster.setState(1);
            } else {
                LOGGER.severe("Invalid state from peer " + state + ", set this server primary!");
                MonitoringMaster.setState(1);
            }
            
            MonitoringMaster.start();
            
        }
    }
    
}
