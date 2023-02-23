package com.ihm.pam;

import basicpbpsdemo.Client;

import basicpbpsdemo.Client.APIException;

import basicpbpsdemo.ClientManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.kpmg.ihm.pam.ConfigUtils;
import com.kpmg.ihm.pam.Utils;


/**
 * Common utils, register attribute etc.
 */
public class CommonUtils {
    private CommonUtils() {
        super();
    }
    
    private static final Logger LOGGER = Logger.getLogger(CommonUtils.class.getName());
    
    
    
    public static String getSmartRuleId(String smartGroupTitle) throws APIException {
        String sgId = null;
        try {
            Map sg = ClientManager.getInstance().getSmartRuleByTitle(smartGroupTitle);
            sgId = Utils.toIDString(sg.get("SmartRuleID"));
        } catch (APIException apie) {
            if (apie.getMessage().indexOf("Smart Rule not found") >= 0) {
                LOGGER.severe("Smart rule [" + smartGroupTitle + "] doesn't exist, or the API user doesn't have access!");
            }
            throw apie;
        }
        return sgId;
    }
    
    public static void registerAttribute(String type, String[] values) {
        try {
            Client client = ClientManager.getInstance();
    
            Map<String, String> attTypes = client.getAttributeTypes();
    
            LOGGER.info("Found existing attribute types:\n" + attTypes);
            
            if (!attTypes.containsKey(type)) {
                LOGGER.info("Platform attribute doesn't exist, create it ...");
                String attId = client.createAttributeType(type);
                attTypes.put(type, attId);
                LOGGER.info("Added platform type.");
            } else {
                LOGGER.info("Platform attribute already exists!");
            }
            
            String attTypeId = attTypes.get(type);
            Map<String, String> attTypeAtts = client.getAttributesForType(attTypeId);

            for(String platform : values) {
                String attId = attTypeAtts.get(platform);
                if (attId == null) {
                    LOGGER.info("Platform [" + platform + "] doesn't exist, create it ...");
                    attId = client.createAttributeForType(attTypeId, platform);
                    LOGGER.info("Platform [" + platform + "] created with ID = " + attId);
                }
            }
            
            LOGGER.info("Done");            
            
        } catch (Throwable tr) {
            LOGGER.log(Level.SEVERE, "Unexptected error!", tr);
        }

    }
    
    
    public static void removeAttribute(String type) {
        try {
            Client client = ClientManager.getInstance();
    
            Map<String, String> attTypes = client.getAttributeTypes();
    
            LOGGER.info("Found existing attribute types:\n" + attTypes);
            
            if (!attTypes.containsKey(type)) {
                LOGGER.info("Platform attribute doesn't exist, skip");
            } else {
                LOGGER.info("Platform attribute exists, delete it ...");
                String attTypeId = attTypes.get(type);
                client.deleteAttributeType(attTypeId);
                LOGGER.info("Platform attribute deleted!");
            }
            
            LOGGER.info("Done");            
            
        } catch (Throwable tr) {
            LOGGER.log(Level.SEVERE, "Unexptected error!", tr);
        }

    }
    
    
    public static void tagAttributeForSmartGroup(String smartGroupTitle, String attName, String attVal) {
        try {        
            LOGGER.info("tagging smart group " + smartGroupTitle + " : " + attName + " = " + attVal);
            
            Client client = ClientManager.getInstance();
            String sgId = null;
            try {
                Map sg = client.getSmartRuleByTitle(smartGroupTitle);
                sgId = Utils.toIDString(sg.get("SmartRuleID"));
            } catch (APIException apie) {
                if (apie.getMessage().indexOf("Smart Rule not found") >= 0) {
                    LOGGER.severe("Smart rule [" + smartGroupTitle + "] doesn't exist, or the API user doesn't have access!");
                }
                throw apie;
            }


            Map<String, String> attTypes = client.getAttributeTypes();

            LOGGER.info("Attribute Types:" + attTypes);

            if (!attTypes.containsKey(attName)) {
                throw new RuntimeException("Expect attribute type " + attName + " does not exist, please run the create attribute task first!");
            }
            
            String acctTypeAttId = attTypes.get(attName);
            Map<String, String> acctTypeAtts = client.getAttributesForType(acctTypeAttId);
            String attValueId = acctTypeAtts.get(attVal);
            if (attValueId == null) {
                throw new RuntimeException("Expect attribute type " + attName + "/" + attVal + " does not exist, please run the create attribute task first!");
            }
            
            
            List assets = client.getSmartRuleAsset(sgId);
            LOGGER.info("$$$$$$$$$$$$$$$$$TOTAL " + assets.size() + " found in smart group " + smartGroupTitle);
            int ct = 1;
            for(Object o : assets) {
                Map m = (Map) o;
                LOGGER.info("" + (ct ++) + " : Process asset: " + m);
                String assetId = Utils.toIDString(m.get("AssetID"));
                
                client.deleteAssetAttributes(assetId);
                try {
                    client.setAttributeForAsset(assetId, attValueId);
                    LOGGER.info("Tagged it with " + attName + " = " + attVal);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            
            
        } catch (Throwable tr) {
            LOGGER.log(Level.SEVERE, "Unexptected error!", tr);
        }
        
    }
    
    
    
    public static void appendAttributeForSmartGroup(String smartGroupTitle, String attName, String attVal) {
        try {        
            LOGGER.info("tagging smart group " + smartGroupTitle + " : " + attName + " = " + attVal);
            
            Client client = ClientManager.getInstance();
            String sgId = null;
            try {
                Map sg = client.getSmartRuleByTitle(smartGroupTitle);
                sgId = Utils.toIDString(sg.get("SmartRuleID"));
            } catch (APIException apie) {
                if (apie.getMessage().indexOf("Smart Rule not found") >= 0) {
                    LOGGER.severe("Smart rule [" + smartGroupTitle + "] doesn't exist, or the API user doesn't have access!");
                }
                throw apie;
            }


            Map<String, String> attTypes = client.getAttributeTypes();

            LOGGER.info("Attribute Types:" + attTypes);

            if (!attTypes.containsKey(attName)) {
                throw new RuntimeException("Expect attribute type " + attName + " does not exist, please run the create attribute task first!");
            }
            
            String acctTypeAttId = attTypes.get(attName);
            Map<String, String> acctTypeAtts = client.getAttributesForType(acctTypeAttId);
            String attValueId = acctTypeAtts.get(attVal);
            if (attValueId == null) {
                throw new RuntimeException("Expect attribute type " + attName + "/" + attVal + " does not exist, please run the create attribute task first!");
            }
            
            
            List assets = client.getSmartRuleAsset(sgId);
            LOGGER.info("$$$$$$$$$$$$$$$$$TOTAL " + assets.size() + " found in smart group " + smartGroupTitle);
            int ct = 1;
            for(Object o : assets) {
                Map m = (Map) o;
                LOGGER.info("" + (ct ++) + " : Process asset: " + m);
                String assetId = Utils.toIDString(m.get("AssetID"));
                
                try {
                    client.setAttributeForAsset(assetId, attValueId);
                    LOGGER.info("Tagged it with " + attName + " = " + attVal);
                } catch (Exception e) {
                    LOGGER.warning("Attribute may exists, skip ..." + e);
                }
            }
            
            
        } catch (Throwable tr) {
            LOGGER.log(Level.SEVERE, "Unexptected error!", tr);
        }
        
    }
    
    /**
     *  return all valid asset (in platfrom smart group), key is IP
     */
    public static Map<String, Map> getAssets() throws APIException {
        String platformGroupsStr = ConfigUtils.getString("onboarding.platformGroups");
        String[] sgNames = platformGroupsStr.split(",");
        Map<String, Map> result = new HashMap<String, Map>();
        for(String smartGroupTitle : sgNames) {
            LOGGER.info("Get all assets in " + smartGroupTitle);
            Client client = ClientManager.getInstance();
            String sgId = null;
            try {
                Map sg = client.getSmartRuleByTitle(smartGroupTitle);
                sgId = Utils.toIDString(sg.get("SmartRuleID"));
            } catch (APIException apie) {
                if (apie.getMessage().indexOf("Smart Rule not found") >= 0) {
                    LOGGER.severe("Smart rule [" + smartGroupTitle + "] doesn't exist, or the API user doesn't have access!");
                }
                throw apie;
            }

            
            List assets = client.getSmartRuleAsset(sgId);
            LOGGER.info("Found " + assets.size() + " assets in " + smartGroupTitle);
            
            for(Object o : assets) {
                Map m = (Map) o;
                String ip = (String) m.get("IPAddress");
                if (ip == null) {
                    LOGGER.warning("No IP found for asset: " + m);
                } else {
                    result.put(ip, m);
                }
            }
            
        }
        
        return result;
    }
    
    public static List<String> getWindowsIPs() throws APIException {
        String title = ConfigUtils.getString("monitoring.script.registerNewServer.smartRule.Windows");
        return getSmartRuleIPs(title);
    }
    
    public static List<String> getLinuxIPs() throws APIException {
        String title = ConfigUtils.getString("monitoring.script.registerNewServer.smartRule.Linux");
        return getSmartRuleIPs(title);
    }

    public static List<String> getAIXIPs() throws APIException {
        String title = ConfigUtils.getString("monitoring.script.registerNewServer.smartRule.AIX");
        return getSmartRuleIPs(title);
    }


    private static List<String> getSmartRuleIPs(String smartGroupTitle) throws APIException {
        ArrayList<String> result = new ArrayList<String>(50);
            LOGGER.info("Get all assets in " + smartGroupTitle);
            Client client = ClientManager.getInstance();
            String sgId = null;
            try {
                Map sg = client.getSmartRuleByTitle(smartGroupTitle);
                sgId = Utils.toIDString(sg.get("SmartRuleID"));
            } catch (APIException apie) {
                if (apie.getMessage().indexOf("Smart Rule not found") >= 0) {
                    LOGGER.severe("Smart rule [" + smartGroupTitle + "] doesn't exist, or the API user doesn't have access!");
                }
                throw apie;
            }

            
            List assets = client.getSmartRuleAsset(sgId);
            LOGGER.info("Found " + assets.size() + " assets in " + smartGroupTitle);
            
            for(Object o : assets) {
                Map m = (Map) o;
                String ip = (String) m.get("IPAddress");
                if (ip == null) {
                    LOGGER.warning("No IP found for asset: " + m);
                } else {
                    result.add(ip);
                }
            }
            
        
        return result;

    }
    
    
    public static void main(String[] args) throws APIException {
        getAssets();
    }
    
    
    public static String findIPFromHost(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.getHostAddress();
        } catch (UnknownHostException uhe) {
            // TODO: Add catch code
            return null;
        }
    }
}
