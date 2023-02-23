package com.kpmg.ihm.pam;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.ihm.pam.monitoring.TestResult;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Send email.
 */
public class SendMail {

    public static Logger LOGGER = Logger.getLogger(SendMail.class.getName());
    String smtpServer = null;
    String smtpPort = null;
    String smtpAuth = null;
    String smtpStartTLSEnable = null;
    String emailUserName = null;
    String emailPassword = null;

    public SendMail(String smtpServer, String smtpPort, String smtpAuth, String smtpStartTLSEnable,
                    String emailUserName, String emailPassword) {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpAuth = smtpAuth;
        this.smtpStartTLSEnable = smtpStartTLSEnable;
        this.emailUserName = emailUserName;
        this.emailPassword = emailPassword;
    }

    public void sendEmail(String testCaseName, TestResult result) {
        String smtpServer = ConfigUtils.getString("smtpServer");

        String emailFrom = ConfigUtils.getString("emailFrom");
        String emailTo = ConfigUtils.getString("emailTo");
        String emailCc = ConfigUtils.getString("emailCc");
        String emailSubject = ConfigUtils.getString("emailSubject");
        String emailText = ConfigUtils.getString("emailText");

        java.util.Date currentDate = Calendar.getInstance().getTime();

/*
        Map<String, String> substitutes = new HashMap<>();
        //substitutes.put("HOSTNAME", hostKey);
        //substitutes.put("IPADDRESS", hostValue);
        substitutes.put("CURRENTDATE", currentDate.toString());
        StrSubstitutor sub = new StrSubstitutor(substitutes);

        emailText = sub.replace(emailText);
*/
        
        emailText = emailText.replaceAll("CURRENTDATE", currentDate.toString());

        try {
            email(emailFrom, emailTo, emailCc, emailSubject, emailText);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    //Email function start
    public void email(String emailFrom, String emailTo, String emailCc, String emailSubject,
                      String emailText) throws IOException {


        try {
            Properties prop = new Properties();

            java.util.Date currentDate = Calendar.getInstance().getTime();

            LOGGER.info("Entering email fromAddress=" + emailFrom + ", toAddress=" + emailTo + ", subject=" +
                        emailSubject + ", body=" + emailText);


            prop.put("mail.smtp.host", smtpServer);
            //            prop.put("mail.smtp.ssl.enable", true);
            prop.put("mail.smtp.port", smtpPort);
            prop.put("mail.smtp.auth", smtpAuth);
            prop.put("mail.smtp.starttls.enable", smtpStartTLSEnable);

            //ServerConfig serverConfig = new ServerConfig(host, port, user, password);
            //TransportStrategy transportStrategy = TransportStrategy.SMTP_TLS;
            //Mailer mailer = new Mailer(serverConfig, transportStrategy);


            //          Session session = Session.getInstance(prop, null);
            Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
                //override the getPasswordAuthentication method
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUserName, emailPassword);
                }
            });

            session.setDebug(true);
            Message msg = new MimeMessage(session);

            // from
            msg.setFrom(new InternetAddress(emailFrom));

            // to
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo, false));

            //cc
            msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(emailCc, false));

            // subject
            msg.setSubject(emailSubject);

            // body content
            msg.setText(emailText);
            //current date
            msg.setSentDate(currentDate);

            LOGGER.info("Sending email: date=" + currentDate + "fromAddress=" + emailFrom + ", toAddress=" + emailTo +
                        ", subject=" + emailSubject + ", body=" + emailText);

            Transport.send(msg);

            LOGGER.info("Exiting method email.");

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "SendMail1", e);
        }

    }

}
