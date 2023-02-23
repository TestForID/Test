package com.kpmg.ihm.pam;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read config name=value from file.
 * use JVM property (-D) CFG_FILE to specify full primary file name.  or it loads primary file from config.cfg from class path.
 * If also looks for a file with name = primary file name + ".env", this can have some environment specific configs.
 * The one in .env append/overwirte primary entires.
 * 
 * In a file named app.name exist on claspath, the first line must be the app identifieer, usually the app name
 * in this case, "_"{APP_NAME} is appened to CFG_FILE, ad APP_CFG, e.g. -DAPP_CFG_app1=/local/user1/app.cfg
 * appKey is used in case multi app hosted in same app server so different app
 * can read different config on file system.
 * 
 */
public class ConfigUtils {

    private static final String ENV_EXT = ".env";

    private static Map<String, String> prop = null;
    private static List<NameAndValue> nameValueList = null;
    private static String path  = null;
   
    
    private ConfigUtils() {
    }
        
        
    public static void init(String[] args) {
        if (prop == null) {
            prop =  new HashMap<String, String>();
        } else {
            return;  //done already.  no need worry about concurrent access during startup phase.
        }
        
        
        String appKey = "";
        /* By Bijendra
        List<String> appKeys = readFileAsStringList("/app.name", true);
        

        if (appKeys != null && appKeys.size() > 0) {
            appKey = "_" + appKeys.get(0);
        }*/
        //appKey is used in case multi app hosted in same app server so different app
        //can read different config on file system.
        
        boolean singleConfig = (args != null && args.length == 1 && args[0] != null);
        
        String fileName =  singleConfig ? args[0] : System.getProperty("CFG_FILE" + appKey);
        //String fileName =  args[0] ;

        if (fileName == null) {
            fileName = "C:\\\\Users\\\\bijendrarawat\\\\Downloads\\\\Onboarding Utility\\\\automation\\\\src\\\\main\\\\java\\\\com\\\\kpmg\\\\ihm\\\\pam\\\\app.cfg"; 
        }
        
        nameValueList = readFileAsNameValueList(fileName, false);
        
        mergeConfigs(readFileAsNameValueList(fileName + ENV_EXT, true));  //BY Bijendra Comment out it
        
        if (!singleConfig) { //this the  global config
    
         /*   for(int i = 0; i < 20; i ++) {
                mergeConfigs(readFileAsNameValueList("/component" + i + ".cfg", true));  //each component can have its own setting, it overwrites config.cfg
            }
    */
            String appFileName = System.getProperty("APP_CFG_FILE" + appKey);
            if (appFileName == null) {
                appFileName = "C:\\Users\\bijendrarawat\\Downloads\\Onboarding Utility\\automation\\src\\main\\java\\com\\kpmg\\ihm\\pam\\app.cfg";
            }
            
            mergeConfigs(readFileAsNameValueList(appFileName, true));   //the one in app.cfg has highest priority
            mergeConfigs(readFileAsNameValueList(appFileName + ENV_EXT, true));
        }
       
        for(NameAndValue nv : nameValueList) {
            prop.put(nv.getName(), nv.getValue());
            System.out.println(appKey + ":" + nv.getName() + " = " + nv.getValue());
        }
        
        

    }



    private static List<NameAndValue> readFileAsNameValueList(String fileName, boolean nonExistsOK) {
        List<String> list = readFileAsStringList(fileName, nonExistsOK);
        if (list == null)
            return null;

        List<NameAndValue> result = null;
        if (list != null) {
            result = new ArrayList<NameAndValue>(list.size());
            for (String str : list) {
                int p = str.indexOf('=');
                if (p <= 0) {
                    throw new RuntimeException("Invalid line (must be <name>=<value> format) in file " + fileName +
                                               ": " + str);
                }
                result.add(new NameAndValue(str.substring(0, p).trim(),
                                            p + 1 == str.length() ? null : str.substring(p + 1).trim()));
            }
        }
        return result;
    }



    private static List<String> readFileAsStringList(String fileName, boolean nonExistsOK) {
        ArrayList<String> list = new ArrayList<String>();
        BufferedReader br = null;
        try {
            InputStream is = null;
            try {
                is = new FileInputStream(fileName);
            } catch (FileNotFoundException fne) {
                is = null;
            }

            if (is == null)
                is = ConfigUtils.class.getResourceAsStream(fileName);

            if (is == null) {
                if (nonExistsOK) {
                    return null;
                } else
                    throw new RuntimeException("Error, configuration file [" + fileName + "] not found!");
            }

            InputStreamReader isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String buf = null;
            while ((buf = br.readLine()) != null) {
                buf = buf.trim();
                if (buf.length() > 0 && '#' != buf.charAt(0)) {
                    list.add(buf);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to read file " + fileName + "! " + e);
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (Exception beE) {
                    System.err.println("Failed to close file " + fileName + "\r\n" + beE);
                }
        }
        return list;
    }


    private static void mergeConfigs(List<NameAndValue> additionalNameValues) {
        if(additionalNameValues != null) {
            Map<String, NameAndValue> map = new HashMap<String, NameAndValue>();

            //merge the list if both exists, 2nd one ovewrite.
            //if new name value in env file, then append to end
            //to keep order, put empty entry in primary file.
            for(NameAndValue nv : nameValueList) {
                map.put(nv.getName(), nv);
            }
            
            for(NameAndValue nv : additionalNameValues) {
                NameAndValue primaryNv = map.get(nv.getName());
                if(primaryNv == null) nameValueList.add(nv);
                else primaryNv.setValue(nv.getValue());
            }
            
        }

    }


    /**
     * get config value
     * @param name
     * @return
     */
    public static String getString(String name) {

        String value = getString(name, null);
        if (value == null) throw new RuntimeException("!!!!! Configuraton value not found : " + name);
        
        return value;
    }

    /**
     * get config value
     * @param name
     * @param defaultVal
     * @return
     */
    public static String getString(String name, String defaultVal) {
        if (prop == null) init(null);
        
        String value = prop.get(name);
        if (value != null && value.startsWith(EncryptionUtils.ENC_SIG)) {
            value = EncryptionUtils.decrypt(value);
        }
        
        if (value == null) value = defaultVal;
        
        return value;
    }

    
    /**
     * get value as integer
     * @param name
     * @return
     */
    public static int getInteger(String name) {
        if (prop == null) init(null);
        String strValue = getString(name);
        if (strValue == null) throw new RuntimeException("Config value " + name + " not found!");
        return Integer.parseInt(strValue);
    }
    
    /**
     * group attributes in sections, e.g. use the common prefix. list.attribute1=aaa, list.attribute2=bbb
     * then getSection("list") can get all attribute starts with list.
     * @param prefix
     * @return map of name and value.
     */
    public static Map<String, String> getSectionAsMap(String prefix) {
        if (prop == null) init(null);
        Map<String, String> result = new HashMap<String, String>();
        int len = prefix.length();
        for (String name : prop.keySet()) {
            if (name.startsWith(prefix)) {
                result.put(name.substring(len), getString(name));
            }
        }
        return result;

    }
    

    /**
     * group attributes in sections, e.g. use the common prefix. list.attribute1=aaa, list.attribute2=bbb
     * then getSection("list") can get all attribute starts with list.
     * @param prefix
     * @return list of name and value so that order is kept.
     */
    public static List<NameAndValue> getSectionAsList(String prefix) {
        if (prop == null) init(null);
        List<NameAndValue> result = new ArrayList<NameAndValue>();
        int len = prefix.length();
        for(NameAndValue nv : nameValueList) {
            String name = nv.getName();
            if (name.startsWith(prefix)) {
                result.add(new NameAndValue(name.substring(len), getString(name)));
            }
        }
        
        return result;
    }
}
