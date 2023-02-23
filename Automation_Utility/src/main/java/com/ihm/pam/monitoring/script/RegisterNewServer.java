package com.ihm.pam.monitoring.script;

import basicpbpsdemo.Client;

import basicpbpsdemo.Client.APIException;

import basicpbpsdemo.ClientManager;

import com.ihm.pam.CommonUtils;
import com.ihm.pam.Constants;
import com.ihm.pam.monitoring.MonitoringMaster;
import com.ihm.pam.monitoring.MonitoringUtils;
import com.ihm.pam.monitoring.Severity;
import com.kpmg.ihm.pam.ConfigUtils;
import com.kpmg.ihm.pam.Utils;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Register new servser:
 * create asset use IP and hostname of the IP if IP not exists
 * tag the platform
 * trigger the smart rule execution
 *
 * create account
 * trigger the account management smart rule execution
 *
 */
public class RegisterNewServer {
    public RegisterNewServer() {
        super();
    }

    private static final Logger LOGGER = Logger.getLogger(RegisterNewServer.class.getName());


    public static String registerTemp(String ip, String platform) {
        if (MonitoringMaster.getState() == 2) {
            return "This is the standby server, ignore registration request for : " + platform + "/" + ip;
        }
        try {
            String host = MonitoringUtils.findHostFromIp(ip);
            LOGGER.severe("Logging new assert for: " + platform + "/" + ip + "/" + host);
            int code = 0;
            
            ScriptResult result = new ScriptResult();
            result.setCode(code);
            result.setMessage("OK");
            result.setAction("DetaildScan");
            result.setSeverity(code == 0 ? Severity.NONE : Severity.HIGH);
            result.setName(RegisterNewServer.class.getSimpleName());
            result.setInstanceId(platform + "/" + ip + "/" + host);
            ScriptMaster.add(result);
            return "_REG_OK_";
        } catch (Throwable th) {
            LOGGER.log(Level.SEVERE, "Unexpected error!", th);
            return th.getMessage();
        }
    }
    

    public static String register(String ip, String platform) {
        StringBuilder status = new StringBuilder();
        int code = 0;
        String host = null;
        try {

            if (!Utils.validate(ip)) {
                code = 1;
                throw new RuntimeException("Invalid/NULL IP in asset: " + platform + "/" + ip);
            }

            if (MonitoringMaster.getState() == 2) {
                //standby mode, do nothing
                return "This is the standby server, ignore registration request for : " + platform + "/" + ip;
            } else {
                List list = ClientManager.getInstance().searchAssetByIP(ip);
                int size = list == null ? 0 : list.size();
                if (size > 1) {
                    code = 11;
                    status.append("_ERROR_. " + platform + "/" + ip + " duplicate IP exists. Need manually resolve the issue!");
                } else if (size == 1) {

                    /*                     Map asset = (Map) list.get(0);
                    String assetId = Utils.toIDString(asset.get("AssetID"));
                    
                    LOGGER.info("Asset exists: " + platform + "/" + ip);
                    Map<String, String> attTypes = ClientManager.getInstance().getAttributeTypes();
                    String acctTypeAttId = attTypes.get(Constants.ATT_TYPE_PLATFORM);
                    Map<String, String> acctTypeAtts = ClientManager.getInstance().getAttributesForType(acctTypeAttId);
                    String attValueId = acctTypeAtts.get(platform);
                    if (attValueId == null) {
                        throw new RuntimeException("Expect attribute type " + Constants.ATT_TYPE_PLATFORM + "/" +
                                                   platform + " exist already!");
                    }
                    LOGGER.info("Got attriubte id for " + platform + " = " + attValueId);
                    LOGGER.info("tagging platform attribute ...");
                    try {
                        ClientManager.getInstance().setAttributeForAsset(assetId, attValueId);
                        LOGGER.info("Asset's platform is set to " + platform + "  ASSET details:" + asset);
                    } catch (Client.APIException apie) {
                        LOGGER.warning("Attribute may exists: " + apie);
                    }

                    String smartGroupTitle =
                        ConfigUtils.getString("monitoring.script.registerNewServer.smartRule." + platform);
                    String sgId = CommonUtils.getSmartRuleId(smartGroupTitle);
                    LOGGER.info("Got the smart rule id = " + sgId + ", executing it ...");
                    ClientManager.getInstance().executeSmartRule(sgId);


                    String sysID = null;
                    LOGGER.info("Smart rule excution complete, get the managed system ID ...");
                    for (int i = 0; i < 3; i++) {
                        Map mgdSys = ClientManager.getInstance().getManagedSystemByAssetId(assetId);
                        if (mgdSys == null) {
                            LOGGER.info("System is not managed syet, wait 10 second ...");
                            Thread.sleep(10000);
                        } else {
                            sysID = Utils.toIDString(mgdSys.get("ManagedSystemID"));
                            break;
                        }
                    }

                    if (sysID == null) {
                        throw new RuntimeException("The asset is not managed by smart rule! ip = " + ip);
                    }

                    String acctStr = ConfigUtils.getString("DefaultAccounts." + platform + ".accounts", null);
                    if (acctStr == null) {
                        throw new RuntimeException("DefaultAccount for " + platform +
                                                   " is not configured: DefaultAccounts." + platform + ".accounts");
                    }

                    String acctSmartRule = ConfigUtils.getString("DefaultAccounts." + platform + ".smartrule", null);
                    if (acctSmartRule == null) {
                        throw new RuntimeException("smart rule for " + platform +
                                                   " is not configured: DefaultAccounts." + platform + ".smartrule");
                    }

                    String[] accts = acctStr.split(",");
                    for (String acctName : accts) {
                        acctName = acctName.trim();
                        
//                    postManagedAccount(String systemID, String accountName, String password, String description,
  //                  String passwordRuleID, String apiEnabled, String releaseNotificationEmail, String changeServices,
    //                String restartServices, String releaseDuration, String maxReleaseDuration, String isaReleaseDuration,
      //              String autoManagement, String checkPasswordFlag, String resetPasswordOnMismatch,
        //            String changePasswordAfterAnyRelease, String changeFrequencyType, String changeFrequencyDays,
          //          String changeTime)
                    
                        try {
                            String respStr =
                                ClientManager.getInstance()
                                .postManagedAccount(sysID, acctName, "the default password$123", "The default account",
                                                    null, "true", null, null, null, null, null, null, "false", null, null,
                                                    null, null, null, null);
                            LOGGER.info("Managed account : " + respStr);
                        } catch (Client.APIException apie) {
                            LOGGER.warning("Default acct may already exists: " + apie);
                        }
                    }

                    sgId = CommonUtils.getSmartRuleId(acctSmartRule);
                    LOGGER.info("Got the account management smart rule id = " + sgId + ", executing it ...");
                    ClientManager.getInstance().executeSmartRule(sgId);

                    LOGGER.info("Complete!"); */
                    LOGGER.info("IP " + ip + " already exist, ignore it!");
                } else {
                    //new asset
                    host = MonitoringUtils.findHostFromIp(ip);
                    LOGGER.info("Creating new assert for " + platform + "/" + ip + "-" + host + "...");

                    String resp =
                        ClientManager.getInstance().postAsset(Constants.ASSET_GROUP, host, host, null, ip, null, null);
                    Map asset = Utils.jsonToMap(resp);
                    String assetId = Utils.toIDString(asset.get("AssetID"));
                    LOGGER.info("Created new assert for " + platform + "/" + ip);
                    Map<String, String> attTypes = ClientManager.getInstance().getAttributeTypes();
                    String acctTypeAttId = attTypes.get(Constants.ATT_TYPE_PLATFORM);
                    Map<String, String> acctTypeAtts = ClientManager.getInstance().getAttributesForType(acctTypeAttId);
                    String attValueId = acctTypeAtts.get(platform);
                    if (attValueId == null) {
                        throw new RuntimeException("Expect attribute type " + Constants.ATT_TYPE_PLATFORM + "/" +
                                                   platform + " exist already!");
                    }
                    LOGGER.info("Got attriubte id for " + platform + " = " + attValueId);
                    LOGGER.info("tagging platform attribute ...");
                    ClientManager.getInstance().setAttributeForAsset(assetId, attValueId);
                    LOGGER.info("Asset's platform is set to " + platform + "  ASSET details:" + asset);


                    String smartGroupTitle =
                        ConfigUtils.getString("monitoring.script.registerNewServer.smartRule." + platform);
                    String sgId = CommonUtils.getSmartRuleId(smartGroupTitle);
                    LOGGER.info("Got the smart rule id = " + sgId + " for " + smartGroupTitle + ", executing it ...");
                    ClientManager.getInstance().executeSmartRule(sgId);

                    //now create account and execute smart rule to manage it
                    /*
                    DefaultAccounts.Windows.accounts=OSTEAM
                    DefaultAccounts.Linux.accounts=root
                    DefaultAccounts.AIX.accounts=root

                    DefaultAccounts.Windows.smartrule=PAM_Win_MA
                    DefaultAccounts.Linux.smartrule=PAM_Linux_MA
                    DefaultAccounts.AIX.smartrule=PAM_Unix_MA
                     */
                    String sysID = null;
                    LOGGER.info("Smart rule excution complete, get the managed system ID ...");
                    for (int i = 0; i < 3; i++) {
                        Map mgdSys = ClientManager.getInstance().getManagedSystemByAssetId(assetId);
                        if (mgdSys == null) {
                            LOGGER.info("System is not managed syet, wait 10 second ...");
                            Thread.sleep(10000);
                        } else {
                            sysID = Utils.toIDString(mgdSys.get("ManagedSystemID"));
                            break;
                        }
                    }

                    if (sysID == null) {
                        throw new RuntimeException("The asset is not managed by smart rule! ip = " + ip);
                    }

                    String acctStr = ConfigUtils.getString("DefaultAccounts." + platform + ".accounts", null);
                    if (acctStr == null) {
                        throw new RuntimeException("DefaultAccount for " + platform +
                                                   " is not configured: DefaultAccounts." + platform + ".accounts");
                    }

                    String acctSmartRule = ConfigUtils.getString("DefaultAccounts." + platform + ".smartrule", null);
                    if (acctSmartRule == null) {
                        throw new RuntimeException("smart rule for " + platform +
                                                   " is not configured: DefaultAccounts." + platform + ".smartrule");
                    }


                    String[] accts = acctStr.split(",");
                    for (String acctName : accts) {
                        acctName = acctName.trim();
                        /*
        postManagedAccount(String systemID, String accountName, String password, String description,
            String passwordRuleID, String apiEnabled, String releaseNotificationEmail, String changeServices,
            String restartServices, String releaseDuration, String maxReleaseDuration, String isaReleaseDuration,
            String autoManagement, String checkPasswordFlag, String resetPasswordOnMismatch,
            String changePasswordAfterAnyRelease, String changeFrequencyType, String changeFrequencyDays,
            String changeTime)
                 */
                        String respStr =
                            ClientManager.getInstance()
                            .postManagedAccount(sysID, acctName, "the default password$123", "The default account",
                                                null, "true", null, null,
                                                null, null, null, null, 
                                                "false", null, null,
                                                null, null, null, null);
                        LOGGER.info("Managed account : " + respStr);
                    }
                    
                    sgId = CommonUtils.getSmartRuleId(acctSmartRule);
                    LOGGER.info("Got the account management smart rule id = " + sgId + " for " + acctSmartRule + ", executing it ...");
                    ClientManager.getInstance().executeSmartRule(sgId);
                    LOGGER.info("Complete!");
                    status.append("successfully registered new server " + platform + "/" + ip);

                } //if not exist
            }
        } catch (Throwable th) {
            if (code == 0)
                code = 10;
            LOGGER.log(Level.SEVERE, "Unexpected error!", th);
            status.append("Unexpected error: " + th.getMessage());
        }

        ScriptResult result = new ScriptResult();
        result.setCode(code);
        result.setMessage(status.toString());
        result.setSeverity(code == 0 ? Severity.NONE : Severity.HIGH);
        result.setAction(code == 0 ? "DetaildScan" : "None");
        result.setName(RegisterNewServer.class.getSimpleName());
        result.setInstanceId(platform + "/" + ip + "/" + host);
        ScriptMaster.add(result);
        LOGGER.info("Reported status " + status);
        return code == 0 ? "_REG_OK_" : status.toString();
    }

}
