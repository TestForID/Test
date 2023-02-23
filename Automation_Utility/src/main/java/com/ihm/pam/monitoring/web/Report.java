package com.ihm.pam.monitoring.web;

import com.ihm.pam.monitoring.MonitoringMaster;
import com.kpmg.ihm.pam.ConfigUtils;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * The servlet that display the dashboard
 */
public class Report extends HttpServlet {
    private static final String CONTENT_TYPE = "text/html; charset=UTF-8";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ConfigUtils.getString("monitoring.alertFileBase");  //make sure it exists.
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Report</title><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\"><br>" + 
        "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js\"></script><br>" + 
        "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js\"></script><br>" + 
        "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js\"></script></head>");
        
        out.println("<body>");
        out.println("<div class=\"container-fluid\">");
        out.println("<center><h1><p class='text-secondary'>PAM Monitoring Status</p></h1></center>");
        out.println(MonitoringMaster.getHTMLReport());
        out.println("</div>");
        out.println("</body></html>");
        out.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Report</title></head>");
        out.println("<body>");
        out.println("<p>POST method reserved for future usage!</p>");
        out.println("</body></html>");
        out.close();
    }
}
