package com.ihm.pam.monitoring.web;

import com.ihm.pam.Constants;
import com.ihm.pam.monitoring.MonitoringMaster;
import com.ihm.pam.monitoring.MonitoringUtils;
import com.ihm.pam.monitoring.script.RegisterNewServer;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * The servelet that return status and also take command to generate IPSoft logs.
 */
public class Admin extends HttpServlet {
    private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
    private static String status = "-1";
    private static final Logger LOGGER = Logger.getLogger(Admin.class.getName());
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // use this call to get the status, 0 means waiting to start, 1 means active, 2 means standby

        String command = request.getParameter("command");
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();

        if ("check".equals(command)) {
            LOGGER.info("Received check command, detecting if two primary ...");
            int state = MonitoringMaster.getState();
            if (state == 1) {
                String peerState = MonitoringUtils.getPeerStatus();
                if ("1".equals(peerState)) {
                    LOGGER.warning("The peer is in primary mode, switch to stand by ...");
                    MonitoringMaster.setState(2);
                    LOGGER.warning("Switched to standby!");
                    out.println("Switched from primary to standby ...");

                } else {
                    LOGGER.info("The peer is not in primary mode, ignore " + peerState);
                    out.println("The peer is not in primary mode, ignore " + peerState);
                }
            } else {
                LOGGER.info("Ignore the command as this server is not in primary mode." + state);
                out.println("Ignore the command as this server is not in primary mode." + state);
            }

            out.close();

        } else if ("generatereport".equals(command)) {
            
            String url = request.getRequestURL().toString();
            if (url.startsWith("http://localhost")) {
                String name = MonitoringMaster.generateAlert();
                int p = name.indexOf('|');
                out.println("New Health Summary completed: " + name.substring(0, p));;                
                out.println("New Health Report completed: " + name.substring(p + 1));
            }
            out.close();
        } else if ("registerwindows".equals(command)) {
            String status = RegisterNewServer.registerTemp(MonitoringUtils.getClientIp(request), Constants.PLATFORM_WINDOWS);
            out.println(status);
            out.close();
        }  else if ("registerlinux".equals(command)) {
            String status = RegisterNewServer.registerTemp(MonitoringUtils.getClientIp(request), Constants.PLATFORM_LINUX);
            out.println(status);
            out.close();
        }  else if ("registeraix".equals(command)) {
            String status = RegisterNewServer.registerTemp(MonitoringUtils.getClientIp(request), Constants.PLATFORM_AIX);
            out.println(status);
            out.close();
        } else {
            out.println(MonitoringMaster.getState());
            out.close();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Admin</title></head>");
        out.println("<body>");
        out.println("<p>POST method reserved for future usage!</p>");
        out.println("</body></html>");
        out.close();
    }
}
