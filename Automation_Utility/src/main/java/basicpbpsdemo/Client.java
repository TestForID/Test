package basicpbpsdemo;

import com.kpmg.ihm.pam.Utils;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import javax.net.ssl.HttpsURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

//// Script Version: 1.1
//// Modified: 18-Nov-2016

/**
 * Sample BeyondTrust Password Safe (v6.4.4+) API v3 Client
 *
 * This Class can be used to make calls to the Password Safe v3 Restful API.
 *
 * This class relies on the correct truststore settings to allow secure connections to be made with the Password Safe.
 *
 * The Password Safe CA Certificate can be exported
 * either using the BeyondInsight Configuration tool for a Software installation
 * or via the UVM console for an Appliance. Please consult the product documentation for details.
 *
 * Once exported the CA certificate should be imported into a Java Keystore using a command similar to:
 * <pre>
 * keytool -importcert -keystore cacerts.jks -file PS_SERVER_NAME_eEye_EMS_CA.cer -alias eEye_EMS_ca
 * </pre>
 * <pre>
 * System.setProperty("javax.net.ssl.trustStorePassword ","Password1");
 * System.setProperty("javax.net.ssl.trustStore","/path/to/cacerts.jks");
 * </pre>
 *
 * When Client Certificates are required the following steps must be performed for the API to use a trusted certificate
 * for securing the communication with the Password Safe.
 *
 * The Client Certificate and Private Key can be exported from Password Safe as a PKCS12 file
 * either using the BeyondInsight Configuration tool for a Software installation
 * or via the UVM console for an Appliance. Please consult the product documentation for details.
 *
 * Use the following code to load the Client Certificate and Private Key
 * 
 * <pre>
 * System.setProperty("javax.net.ssl.keyStorePassword","Password1");
 * System.setProperty("javax.net.ssl.keyStoreType","pkcs12");
 * System.setProperty("javax.net.ssl.keyStore","/path/to/eEyeEMSClient.pfx");
 * </pre>
 *
 * To avoid dependencies on 3rd party JSON libraries this class returns the JSON
 * response as a String. Where required, the responses should be parsed using
 * your JSON Parser of choice.
 */
public class Client {

    private static final String URL_PATH = "/api/public/v3/";

    private static final String ERR_400 = "Poorly formatted request. Some value in the request is not properly formatted";
    private static final String ERR_401 = "API Authentication failed";
    private static final String ERR_404 = "Could not find the requested entity";
    private static final String ERR_409 = "Conflicting request exists. Another user has already requested a password for the specified account within the next durationMinutes window";


    private static final String ERR_4031 = "User does not have permission to request a password or account is not valid for the system";
    private static final String ERR_4032 = "Requestor Only API or account. Only Requestors can access this API or account.";
    private static final String ERR_4033 = "Approver Only API or account. Only Approvers can access this API or account.";
    private static final String ERR_4034 = "Request is not yet approved";

    private final URL base_url;

    private Map<String, String> headers;

    /**
     * Creates a new Password Safe API Client connecting to the given host
     *
     * @param host Password Safe host name or IP Address
     * @param port Password Safe Port number
     * @throws Client.APIException
     */
    public Client(String host, int port) throws Client.APIException {
        this(host, port, "BeyondTrust");
    }

    /**
     * Creates a new Password Safe API Client connecting to the given host
     *
     * @param host Password Safe host name or IP Address
     * @throws Client.APIException
     */
    public Client(String host) throws Client.APIException {
        this(host, 443, "BeyondTrust");
    }

    /**
     * Creates a new Password Safe API Client connecting to the given host with custom virtual directory and port
     *
     * @param host Password Safe host name or IP Address
     * @param port Password Safe Port number
     * @param dir Beyond Insight Virtual Directory
     * @throws Client.APIException
     */
    public Client(String host, int port, String dir) throws Client.APIException {
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        try {
            base_url = new URL("HTTPS", host, port, "/" + dir + URL_PATH);
            System.out.println("URL: " + base_url.toString());

            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

            headers = new HashMap<>();
        } catch (MalformedURLException me) {
            throw new Client.APIException("Invalid Password Safe URL", me);
        }
    }

    /*
     * Make a HTTPS connection to the Password Safe Restful API
     */
    private HttpsURLConnection connect(String path) throws Client.APIException {

        try {
            HttpsURLConnection conn;
            URL url;

            url = new URL(base_url, path);

            conn = (HttpsURLConnection) url.openConnection();

            // Add custom headers to the request
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            return conn;
        } catch (MalformedURLException me) {
            throw new Client.APIException("Invalid Password Safe URL", me);
        } catch (IOException ie) {
            throw new Client.APIException("Failed to open connection to Password Safe", ie);
        }
    }

    /*
     * Handle the response from the Password Safe API
     */
    private String getResponse(HttpsURLConnection conn) throws Client.APIException {
        try {
            String rv;
            InputStream is;
            int status = conn.getResponseCode();

            if (status >= 200 && status < 300) {
                is = conn.getInputStream();
                rv = readString(is);
            } else {
                switch (status) {
                    case 400:
                        throw new Client.APIException("Bad Request " + status + ". " + readString(conn.getErrorStream()));
                    case 401:
                        throw new Client.APIException("Unauthorized " + status + ". " +  ERR_401);
                    case 404:
                        throw new Client.APIException("Not Found " + status + ". " +  readString(conn.getErrorStream()));
                    case 409:
                        throw new Client.APIException("Conflict " + status + ". " +  ERR_409);
                    case 403:
                        String err_code;

                        is = conn.getErrorStream();

                        // For 403 Forbidden responses the API sends a reason code in the response body
                        err_code = readString(is);

                        if (null != err_code) {
                            switch (err_code) {
                                case "4031":
                                    throw new Client.APIException("Forbidden " + err_code +  ". " + ERR_4031);
                                case "4032":
                                    throw new Client.APIException("Forbidden " + err_code +  ". " + ERR_4032);
                                case "4033":
                                    throw new Client.APIException("Forbidden " + err_code +  ". " + ERR_4033);
                                case "4034":
                                    throw new Client.APIException("Forbidden " + err_code +  ". " + ERR_4034);
                                default:
                                    throw new Client.APIException("Forbidden " + err_code);
                            }
                        }
                    default:
                        throw new Client.APIException("Error " + status +  ". " + conn.getResponseMessage());
                }
            }

            return rv;
        } catch (IOException ie) {
            throw new Client.APIException("Failed to receive response from Password Safe", ie);
        }
    }
    
    /*
     * Handle the response from the Password Safe API
     */
    private Image getResponseImage(HttpsURLConnection conn) throws Client.APIException {
        try {
            Image rv;
            InputStream is;
            int status = conn.getResponseCode();

            if (status >= 200 && status < 300) {
                is = conn.getInputStream();

                rv = ImageIO.read(is);
            } else {
                switch (status) {
                    case 400:
                        throw new Client.APIException(ERR_400);
                    case 401:
                        throw new Client.APIException(ERR_401);
                    case 404:
                        throw new Client.APIException(ERR_404);
                    case 409:
                        throw new Client.APIException(ERR_409);
                    case 403:
                        String err_code;

                        is = conn.getErrorStream();

                        // For 403 Forbidden responses the API sends a reason code in the response body
                        err_code = readString(is);

                        if (null != err_code) {
                            switch (err_code) {
                                case "4031":
                                    throw new Client.APIException(ERR_4031);
                                case "4032":
                                    throw new Client.APIException(ERR_4032);
                                case "4033":
                                    throw new Client.APIException(ERR_4033);
                                case "4034":
                                    throw new Client.APIException(ERR_4034);
                                default:
                                    throw new Client.APIException(err_code);
                            }
                        }
                    default:
                        throw new Client.APIException(conn.getResponseMessage());
                }
            }

            return rv;
        } catch (IOException ie) {
            throw new Client.APIException("Failed to receive response from Password Safe", ie);
        }
    }

    /*
     * Perform a HTTPS GET to the Password Safe Restful API
     */
    protected String get(String path) throws Client.APIException {
        HttpsURLConnection conn = connect(path);
        String rv = getResponse(conn);
        return rv;
    }

    /*
     * Perform a HTTPS GET to the Password Safe Restful API
     */
    protected Image getImage(String path) throws Client.APIException {
        HttpsURLConnection conn = connect(path);
        Image rv = getResponseImage(conn);
        return rv;
    }

    /*
     * Perform a HTTPS POST to the Password Safe Restful API
     */
    protected String post(String path, String body) throws Client.APIException {
        try {
            HttpsURLConnection conn = connect(path);

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");

            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());

            String rv = getResponse(conn);
            return rv;
        } catch (IOException ie) {
            throw new Client.APIException("Failed to post data to Password Safe", ie);
        }
    }


    protected String delete(String path, String body) throws Client.APIException {
        try {
            HttpsURLConnection conn = connect(path);

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");

            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());

            String rv = getResponse(conn);
            return rv;
        } catch (IOException ie) {
            throw new Client.APIException("Failed to post data to Password Safe", ie);
        }
    }



    /*
     * Perform a HTTPS PUT to the Password Safe Restful API
     */
    protected String put(String path, String body) throws Client.APIException {
        try {
            HttpsURLConnection conn = connect(path);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");

            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());

            String rv = getResponse(conn);
            return rv;
        } catch (IOException ie) {
            throw new Client.APIException("Failed to put data to Password Safe", ie);
        }
    }

    /*
     * Read InputStream and build a String
     */
    private String readString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            String line = br.readLine();
            if (line != null) {
                sb.append(line);
            }

            while ((line = br.readLine()) != null) {
                sb.append("\n").append(line);
            }
        }

        return sb.toString();
    }

    /**
     * Sign application in to API.
	 * API: POST <base>/Auth/SignAppIn
     *
     * @param runas A username. The commands will run in the context of the specified user.
     * @param key Application API key
     * @return JSON user object on success
     * <pre>
     * {
     *   UserId: int,
     *   SID: string,
     *   EmailAddress: string,
     *   IsAdministrator: boolean,
     *   UserName: string
     *   Name: string
     * }
     * </pre>
     * @throws Client.APIException
     */
    public String signAppIn(String runas, String key) throws Client.APIException {
        String response;
        String auth = String.format("PS-Auth key=%s; runas=%s;", key, runas);

        // Add the Authorization header to our list of custom headers
        // This is sent on all API calls.
        headers.put("Authorization", auth);

        response = post("Auth/SignAppIn", "{}");

        if (response == null) {
            throw new APIException("API Authentication Failed");
        } else if (response.contains("UserId") == false) {
            throw new APIException(response);
        }

        return response;
    }
	
    /**
     * Sign Out of the API.
     * @throws Client.APIException
     */
    public String signOut() throws Client.APIException {
        return post("Auth/SignOut", "{}");
    }

    /**
     * Get a list of managed accounts this user has access to
     *
     * @return Managed Accounts list
     * <pre>
     * [
     *   {
     *     SystemId: int,
     *     SystemName: string,
     *     DomainName: string,
     *     AccountId: int,
     *     AccountName: string,
     *     MaximumReleaseDuration: int,
     *     IsIsaAccount: Boolean,
     *     RecordSessionForConnectionFlag: Boolean,
     *     ShowRdp: Boolean
     *     ShowSsh: boolean
     *   },
     *   ...
     * ]
     * </pre>
     * @throws Client.APIException
     */
    public String getManagedAccountsList() throws Client.APIException {
        return get("ManagedAccounts");
    }

    /**
     * Get a managed account this user has access to by system name and account name
     *
     * @return Managed Account
     * <pre>
     *   {
     *     SystemId: int,
     *     SystemName: string,
     *     DomainName: string,
     *     AccountId: int,
     *     AccountName: string,
     *     MaximumReleaseDuration: int,
     *     IsIsaAccount: Boolean,
     *     RecordSessionForConnectionFlag: Boolean,
     *     ShowRdp: Boolean
     *     ShowSsh: boolean
     *   }
     * </pre>
     * @throws Client.APIException
     */
    public String getManagedAccountByName(String systemName, String accountName) throws Client.APIException {
        try {
            String path = String.format("ManagedAccounts?systemName=%s&accountName=%s", URLEncoder.encode(systemName, "UTF-8"), accountName);
            return get(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getManagedDBAccountByName(String systemName, String accountName) throws Client.APIException {
        String path = String.format("ManagedAccounts?systemName=%s&accountName=%s&type=database", systemName, accountName);
        return get(path);
    }


    /**
     * Get a list of all password requests awaiting authorization for this user
     *
     * @return Array of password requests
     * <pre>
     * [
     *   {
     *     RequestId: int
     *     SystemName: string,
     *     RequestReleaseDate: date-formatted string,
     *     ApprovedDate: date-formatted string,
     *     ExpiresDate: date-formatted string
     *   },
     *   ...
     * ]
     * </pre>
     * @throws Client.APIException
     */
    public String getPendingRequests() throws Client.APIException {
        return get("Requests?status=pending");
    }

    /**
     * Get a list of all password requests authorized for this user
     *
     * @return Array of password requests
     * <pre>
     * [
     *    {
     *      RequestId: int
     *      SystemName: string,
     *      RequestReleaseDate: date-formatted string,
     *      ApprovedDate: date-formatted string,
     *      ExpiresDate: date-formatted string
     *    },
     *    ...
     * ]
     * </pre>
     * @throws Client.APIException
     */
    public String getActiveRequests() throws Client.APIException {
        return get("Requests?status=active");
    }

    /**
     * Create a new Password Request
     *
     * @param accountId id of the managed account
     * @param systemId id of the managed system/asset
     * @param duration duration in minutes for the password request
     * @param reason reason for the password request
     * @return id of the password request
     * <pre>
     * ￼requestId: int
     * </pre>
     * @throws Client.APIException
     */
    public String immediatePasswordRequest(int accountId, int systemId, int duration, String reason) throws Client.APIException {
        String body = String.format("{\"AccountId\":%d,\"SystemId\":%d,\"DurationMinutes\":%d,\"Reason\":%s}", accountId, systemId, duration, jsonString(reason));

        return post("Requests", body);
    }

    /**
     * Retrieve the clear text password for the given request id
     *
     * @param requestId Password request id
     * @return clear text password
     * <pre>
     * ￼Password: string
     * </pre>
     * @throws Client.APIException
     */
    public String retrievePassword(int requestId) throws Client.APIException {
        String path = String.format("Credentials/%d", requestId);

        return get(path);
    }

    public String retrieveDSSKey(int requestId) throws Client.APIException {
        String path = String.format("Credentials/%d?type=dsskey", requestId);

        return get(path);
    }


    /**
     * Checks-in/releases the request identified by this request id
     *
     * @param requestId Request id
     * @param reason Reason request is being checked-in
     * @throws Client.APIException
     */
    public void checkinRequest(int requestId, String reason) throws Client.APIException {
        String path = String.format("Requests/%d/Checkin", requestId);
        String body = String.format("{\"reason\":%s}", jsonString(reason));

        put(path, body);
    }

    /**
     * Set the password for a managed account
     *
     * @param accountId ID of the account to manage
     * @param password password to set for the account
     * @return JSON response from Password Safe
     * @throws Client.APIException
     */
    public String setManagedAccountPassword(int accountId, String password) throws Client.APIException {
        String body = String.format("{\"AccountId\":%d,\"Password\":%s}", accountId, password);

        return post("SetManagedAccountPassword", body);
    }

    /**
     * Gets all Workgroups available to the user
     * <pre>
     * {
     *   OrganizationID: string,
     *   ID: int,
     *   Name: string
     * }
     * </pre>
     *
     * @return Array of Workgroups
     * @throws Client.APIException
     */
    public String getWorkgroups() throws Client.APIException {
        return get("Workgroups");
    }

    public String getUserGroups() throws Client.APIException {
        return get ("UserGroups");
    }
    
    public String getAssets(String workGroupName) throws APIException
    {
        return get("Workgroups/" + workGroupName.replace(" ", "%20") + "/Assets");
    }


    public List getUserGroupSmartGroupRoles(String groupId, String smartGroupId) throws APIException {
        String str = get("UserGroups/" + groupId + "/SmartRules/" + smartGroupId + "/Roles");
        return Utils.jsonToList(str);
    }

    public List getSmartRules() throws Client.APIException {
        String str = get("SmartRules");
        return Utils.jsonToList(str);
    }
    
    
    public List getPasswordRules() throws Client.APIException {
        String str = get("PasswordRules");
        return Utils.jsonToList(str);
    }

    public List getDSSKeyRules() throws Client.APIException {
        String str = get("DSSKeyRules");
        return Utils.jsonToList(str);
    }

    public List getDatabases() throws Client.APIException {
        String str = get("Databases");
        return Utils.jsonToList(str);
    }
    
    public List getAssetDatabases(String assetId) throws Client.APIException {
        String str = get("Assets/" + assetId + "/Databases");
        return Utils.jsonToList(str);
    }

    
    public List getSmartRuleAsset(String id) throws APIException {
        String str = get("SmartRules/" + id + "/Assets");
        return Utils.jsonToList(str);
    }
    
    public List getSmartRuleManagedAccounts(String id) throws APIException {
        String str = get("SmartRules/" + id + "/ManagedAccounts");
        return Utils.jsonToList(str);
    }


    public Map getSmartRuleByTitle(String title) throws APIException {
        try {
            String str = get("SmartRules?title=" + URLEncoder.encode(title, "UTF-8"));
            return Utils.jsonToMap(str);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public List getOS() throws APIException {
        String str = get("OperatingSystems");
        return Utils.jsonToList(str);
    }
    
    public List getFunctionalAccounts() throws APIException {
        String str = get("FunctionalAccounts");
        return Utils.jsonToList(str);
    }
    
    
    public List getPlatforms() throws APIException {
        String str = get("Platforms");
        return Utils.jsonToList(str);
    }
    
    public Map getAssetById(String id) throws APIException {
        String str = get("Assets/" + id);
        return Utils.jsonToMap(str);        
    }
    
    public Map getManagedSystemByAssetId(String id) throws APIException {
        try {
            String str = get("Assets/" + id + "/ManagedSystems");
            return Utils.jsonToMap(str);
        } catch (APIException e) {
            String msg = e.getMessage();
            if (msg != null && msg.indexOf("Managed System not found") > 0) {
                return null;                
            } else {
                throw e;
            }
        }
    }

    public Map getManagedSystemByDatabaseId(String id) throws APIException {
        try {
            String str = get("Databases/" + id + "/ManagedSystems");
            return Utils.jsonToMap(str);
        } catch (APIException e) {
            String msg = e.getMessage();
            if (msg != null && msg.indexOf("Managed System not found") > 0) {
                return null;                
            } else {
                throw e;
            }
        }
    }
                               

    public Map getSmartRule(String id) throws Client.APIException {
        String str = get("SmartRules/" + id);
        return Utils.jsonToMap(str);
    }

    public List getPermissions() throws Client.APIException {
        String perms = get("Permissions");
        return Utils.jsonToList(perms);
    }

    public List getAccessPolicies() throws Client.APIException {
        String perms = get("AccessPolicies");
        return Utils.jsonToList(perms);
    }

    public List getRoles() throws Client.APIException {
        String str = get("Roles");
        return Utils.jsonToList(str);
    }


    public List getAllAssets() throws Client.APIException {
        String grpStr = getWorkgroups();

        List groups = Utils.jsonToList(grpStr);
        List result = new ArrayList<>();
        
        for (Object o : groups) {
            String grpName = (String) ((Map) o).get("Name");
            String assetsStr = getAssets(grpName);


            List assets = Utils.jsonToList(assetsStr);
            result.addAll(assets);
        }

        return result;

//            for(Object a : assets) {            
//                System.out.println(((Map) a).get("IPAddress"));
//                System.out.println(a);
//            }
            
            
//            if ("BEYONDTRUST WORKGROUP".equals(grpName)) {
                //add several IP
        //            String status = client.postAsset(grpName, null, null, null, "92.168.163.166", null, "Linux");
        //            System.out.println("Status for add new asset:" + status);
 //           }
            
       // }

    }


    public List getAssetsAttributes(String assetId) throws APIException
    {
        String resp = get("Assets/" + assetId + "/Attributes");
        return Utils.jsonToList(resp);
    }
    
    public String deleteAssetAttribute(String assetId, String attributeId) throws APIException {
        String resp = delete("Assets/" + assetId + "/Attributes/" + attributeId, "{}");
        return resp;
    }

    public String deleteAssetAttributes(String assetId) throws APIException {
        String resp = delete("Assets/" + assetId + "/Attributes", "{}");
        return resp;
    }
    
    
    public Map getManagedAccountByID(String acctId) throws APIException
    {
        String resp = get("ManagedAccounts/" + acctId);
        return Utils.jsonToMap(resp);
    }
    
    public Map setAttributeForAsset(String assetId, String attId) throws APIException {
        String body = "{}";

        String resp = post("Assets/" + assetId + "/Attributes/" + attId, body);
        return Utils.jsonToMap(resp);
    }


    public Map<String, String> getAttributesForType(String attributeTypeID) throws APIException {
        Map<String, String> result = new HashMap<String, String>();
        
        String resp = get("AttributeTypes/" + attributeTypeID + "/Attributes");
        
        List list = Utils.jsonToList(resp);
        
        for (Object o : list) {
            Map m = (Map) o;
            result.put((String) m.get("ShortName"), String.valueOf(((Number) m.get("AttributeID")).intValue()));
        }
        return result;
        
    }
    
    public String createAttributeForType(String attributeTypeID, String name) throws APIException {
        /*
        { ParentAttributeID :
        int, // can be null
        ShortName : string,
        LongName : string,
        Description : string,
        ValueInt : int // can be null
        }
         */
        String body = "{\"ShortName\":\"" + name + "\",\"LongName\":\"" + name + "\",\"Description\":\"" + name + "\"}";
        String resp = post("AttributeTypes/" + attributeTypeID + "/Attributes", body);
        Map m = Utils.jsonToMap(resp);
        return String.valueOf(((Number) m.get("AttributeID")).intValue());
    }

    public String deleteAttributeType(String typeID) throws APIException {
        return delete("AttributeTypes/" + typeID, "{}");
    }
    

    public Map<String, String> getAttributeTypes() throws APIException
    {
        Map<String, String> result = new HashMap<String, String>();
        
        String resp = get("AttributeTypes");
        
        List list = Utils.jsonToList(resp);
        
        for (Object o : list) {
            Map m = (Map) o;
            result.put((String) m.get("Name"), String.valueOf(((Number) m.get("AttributeTypeID")).intValue()));
        }
        return result;
        
    }
    

    public String createAttributeType(String name) throws APIException {
        String body = "{\"Name\":\"" + name + "\"}";
        String resp = post("AttributeTypes", body);
        Map m = Utils.jsonToMap(resp);
        return String.valueOf(((Number) m.get("AttributeTypeID")).intValue());
    }

    
    public String getAsset(String workGroupName, String assetName) throws APIException
    {
        return get("Workgroups/" + workGroupName.replace(" ", "%20") + "/Assets/" + assetName.replace(" ", "%20"));
    }
    
    public String getPlatforms2() throws Client.APIException
    {
        return get("Platforms");
    }
    
    
    
    public String getManagedSystem(String assetID) throws Client.APIException
    {
        return get("Assets/" + assetID + "/ManagedSystems");
    }

    public String getAllManagedSystems() throws Client.APIException
    {
        return get("ManagedSystems");
    }

    
    public List getManagedAccounts(String sysID) throws Client.APIException
    {
        String resp = get("ManagedSystems/" + sysID + "/ManagedAccounts");
        return Utils.jsonToList(resp);
    }
    
    /**
     * Gets a listing of account aliases for which User can request a password
     *
     * @return Array of account aliases
     * <pre>
     * [
     * {
     *   AliasID: int,
     *   AliasName: string,
     *   SystemID: int,
     *   SystemName: string,
     *   AccountID: int,
     *   AccountName: string,
     *   DomainName: string,
     *   InstanceName: string
     * },
     * ...
     * ]
     * </pre>
     * @throws Client.APIException
     */
    public String getAliases() throws Client.APIException {
        return get("Aliases");
    }

    /**
     * Requests an account password for an Alias
     *
     * @param aliasId id of the account alias
     * @param duration duration in minutes for the password request
     * @param reason reason for the password request
     * @return id of the password request
     * <pre>
     * requestId: int
     * </pre>
     * @throws Client.APIException
     */
    public String immediatePasswordRequestByAlias(int aliasId, int duration, String reason) throws Client.APIException {
        String path = String.format("Aliases/%d/Requests", aliasId);
        String body = String.format("{\"durationMinutes\":%d,\"reason\":%s}", duration, jsonString(reason));

        return post(path, body);
    }

    public String immediateChangeAccountCredential(String managedAccountID) throws Client.APIException {
        return post("ManagedAccounts/" + managedAccountID + "/Credentials/Change", "{\"Queue\":\"true\"}");
    }


    

    /**
     * Retrieve the clear text password for the given request id
     *
     * @param requestId Password request id
     * @param aliasId Account Alias id
     * @return clear text password
     * <pre>
     * Password: String
     * </pre>
     * @throws Client.APIException
     */
    public String retrievePasswordForAlias(int aliasId, int requestId) throws Client.APIException {
        String path = String.format("Aliases/%d/Credentials/%d", aliasId, requestId);

        return get(path);
    }

    /**
     * Search the keystroke events for the given condition
     * The search term can be any full text search term 
     *
     * @param condition Full text search term
     * @return JSON array containing the list of matching keystroke events
     * @throws Client.APIException
     */
    public String searchKeystrokes(String condition) throws Client.APIException {
        String path;
        String response = null;
        
        try {
            path = String.format("Keystrokes/search/%s", URLEncoder.encode(condition, "UTF-8").replace("+", "%20"));
            response = get(path);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

        return response;
    }

    /**
     * Get a list of all recorded sessions 
     *
     * @return JSON array containing the list of all recorded sessions
     * @throws Client.APIException
     */
    public String getSessions() throws Client.APIException {
        return get("Sessions");
    }

    /**
     * Get details for a given recorded sessions 
     *
     * @param sessionId ID of the session to lookup
     * @return JSON object containing the details of the recorded session
     * @throws Client.APIException
     */
    public String getSession(int sessionId) throws Client.APIException {
        String path = String.format("Sessions/%d", sessionId);

        return get(path);
    }

    /**
     * Creates a replay session 
     *
     * @param id The UUID for the session, this is the Token value from the Session object
     * @param key The recording key used to encrypt the log, this is the RecordKey value from the Session object
     * @param protocol The protocol (currently SSH or RDP), this can be derived from the Protocol value from the Session object (0 = RDP 1 = SSH)
     * @return JSON object containing the details of the replay session
     * @throws Client.APIException
     */
    public String postReplay(String id, String key, String protocol) throws Client.APIException {
        String body = String.format("{\"id\":%s, \"record_key\":%s, \"protocol\":%s, \"headless\":true}", jsonString(id), jsonString(key), jsonString(protocol));
        
        return post("pbsm/replay", body);
    }

    /**
     * Returns details of the Replay session 
     *
     * @param id The UUID for the session, this is the id value from the Replay Session object returned by postReplay
     * @return JSON object containing the details of the replay session
     * @throws Client.APIException
     */
    public String getReplay(String id) throws Client.APIException {
        String path = String.format("pbsm/replay/%s", id);
        
        return get(path);
    }

    /**
     * Controls the Replay session 
     *
     * @param id The UUID for the session, this is the id value from the Replay Session object returned by postReplay
     * @param speed the speed of the session as a % (-1 to ignore)
     * @param offset the offset of the session from the start in ms (-1 to ignore)
     * @param next skip the replay session until the output changes by the given % (-1 to ignore)
     * @return JSON object containing the details of the replay session
     * @throws Client.APIException
     */
    public String putReplay(String id, int speed, int offset, int next) throws Client.APIException {
        String path = String.format("pbsm/replay/%s", id);
        StringBuilder body = new StringBuilder("{");

        if (speed >= 0) {
            body.append("\"speed\":").append(Integer.toString(speed));
        }

        if (offset >= 0) {
            if (body.length() > 1) body.append(",");
            body.append("\"offset\":").append(Integer.toString(offset));
        }
        
        if (next >= 0) {
            if (body.length() > 1) body.append(",");
            body.append("\"next\":").append(Integer.toString(next));
        }
        
        body.append("}");
        
        return put(path, body.toString());
    }
    
    public String postWorkgroup(String organizationID, String name) throws APIException
    {
        String body = formatMessageBody(new String[]{"OrganizationId", "Name"}, 
                new String[]{jsonString(organizationID), jsonString(name)});
        return post("Workgroups", body);
    }
    
    
    public List searchAssetByIP(String ip) throws APIException {
        String body = formatMessageBody(new String[]{"IPAddress"}, 
                new String[]{jsonString(ip)});
        String resp = post("Assets/Search", body);
        return Utils.jsonToList(resp);
    }
    
    
    public String postAsset(String workgroupName, String assetName, String dnsName, 
            String domainName, String ipAddress, String macAddress, String assetType) throws APIException
    {
        String body = formatMessageBody(new String[]{"AssetName", "DnsName", "DomainName", "IPAddress", "MacAddress", "AssetType"},
            new String[] {jsonString(assetName), jsonString(dnsName), jsonString(domainName), jsonString(ipAddress), 
            jsonString(macAddress), jsonString(assetType)});
        try {
            return post("Workgroups/" + workgroupName.replace(" ", "%20") + "/Assets", body);
        } catch (APIException e) {
            if (e.getMessage().indexOf(" already exists") >= 0) {
                throw new AlreadyExistException(e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    public String postManagedSystem(String assetID, String platformID, String netBiosName, String contactEmail, String description,
            String port, String timeout, String passwordRuleID, String dssKeyRuleID, String releaseDuration, String maxReleaseDuration,
            String isaReleaseDuration, String autoManagementFlag, String functionalAccountID, String elevationCommand, 
            String checkPassword, String changePasswordAfterAnyRelease, String resetPasswordOnMismatch, String changeFrequencyType,
            String changeFrequencyDays, String changeTime) throws APIException
    {
        String body = formatMessageBody(new String[]{"PlatformID", "NetBiosName", "ContactEmail", "Description",
            "Port", "Timeout", "PasswordRuleID", "DSSKeyRuleID", "ReleaseDuration", "MaxReleaseDuration", "ISAReleaseDuration",
            "AutoManagementFlag", "FunctionalAccountID", "ElevationCommand", "CheckPasswordFlag", "ChangePasswordAfterAnyReleaseFlag",
            "ResetPasswordOnMismatchFlag", "ChangeFrequencyType", "ChangeFrequencyDays", "ChangeTime"},
                new String[]{platformID, jsonString(netBiosName), jsonString(contactEmail), jsonString(description),
                    port, timeout, passwordRuleID, dssKeyRuleID, releaseDuration, maxReleaseDuration, isaReleaseDuration,
                    autoManagementFlag, functionalAccountID, jsonString(elevationCommand), checkPassword, changePasswordAfterAnyRelease,
                    resetPasswordOnMismatch, jsonString(changeFrequencyType), changeFrequencyDays, jsonString(changeTime)});
        try {
            return post("Assets/" + assetID + "/ManagedSystems", body);
        } catch (APIException e) {
            if (e.getMessage().indexOf(" already exists") >= 0) {
                throw new AlreadyExistException(e.getMessage());
            } else {
                throw e;
            }
        }

    }
    
    
    
    public String postManagedDatabase(String dbID, String contactEmail, String description,
            String timeout, String passwordRuleID, String releaseDuration, String maxReleaseDuration,
            String isaReleaseDuration, String autoManagementFlag, String functionalAccountID, 
            String checkPassword, String changePasswordAfterAnyRelease, String resetPasswordOnMismatch, String changeFrequencyType,
            String changeFrequencyDays, String changeTime) throws APIException
    {
        String body = formatMessageBody(new String[]{"ContactEmail", "Description",
             "Timeout", "PasswordRuleID", "ReleaseDuration", "MaxReleaseDuration", "ISAReleaseDuration",
            "AutoManagementFlag", "FunctionalAccountID", "CheckPasswordFlag", "ChangePasswordAfterAnyReleaseFlag",
            "ResetPasswordOnMismatchFlag", "ChangeFrequencyType", "ChangeFrequencyDays", "ChangeTime"},
                new String[]{jsonString(contactEmail), jsonString(description),
                    timeout, passwordRuleID, releaseDuration, maxReleaseDuration, isaReleaseDuration,
                    autoManagementFlag, functionalAccountID, checkPassword, changePasswordAfterAnyRelease,
                    resetPasswordOnMismatch, jsonString(changeFrequencyType), changeFrequencyDays, jsonString(changeTime)});
        return post("Databases/" + dbID + "/ManagedSystems", body);
    }    
    
    public String postManagedSystem(String assetID, Map atts) throws APIException {
        return post("Assets/" + assetID + "/ManagedSystems", Utils.toJson(atts));
    }


    public String deleteManagedSystem(String assetID) throws APIException {
        return delete("ManagedSystems/" + assetID, "{}");
    }

    public String deleteManagedAccount(String acctID) throws APIException {
        return delete("ManagedAccounts/" + acctID, "{}");
    }

    public String deleteAsset(String assetID) throws APIException {
        return delete("Assets/" + assetID, "{}");
    }
    
    
    public String postManagedAccount(String systemID, String accountName, String password, String description, 
            String passwordRuleID, String apiEnabled, String releaseNotificationEmail, String changeServices,
            String restartServices, String releaseDuration, String maxReleaseDuration, String isaReleaseDuration,
            String autoManagement, String checkPasswordFlag, String resetPasswordOnMismatch, 
            String changePasswordAfterAnyRelease, String changeFrequencyType, String changeFrequencyDays,
            String changeTime) throws APIException
    {
        String body = formatMessageBody(new String[]{"AccountName", "Password", "Description", "PasswordRuleID", "ApiEnabled",
                    "ReleaseNotificationEmail", "ChangeServicesFlag", "RestartServicesFlag", "ReleaseDuration", "MaxReleaseDuration",
                    "ISAReleaseDuration", "AutoManagementFlag", "CheckPasswordFlag", "ResetPasswordOnMismatchFlag", 
                    "ChangePasswordAfterAnyReleaseFlag", "ChangeFrequencyType", "ChangeFrequencyDays", "ChangeTime"},
            new String[]{jsonString(accountName), jsonString(password), jsonString(description), passwordRuleID, apiEnabled,
                    jsonString(releaseNotificationEmail), changeServices, restartServices, releaseDuration, maxReleaseDuration,
                    isaReleaseDuration, autoManagement, checkPasswordFlag, resetPasswordOnMismatch, changePasswordAfterAnyRelease,
                    jsonString(changeFrequencyType), changeFrequencyDays, jsonString(changeTime)});
        try {
            return post("ManagedSystems/" + systemID + "/ManagedAccounts", body);
        } catch (APIException e) {
            if (e.getMessage().indexOf(" already exists") >= 0) {
                throw new AlreadyExistException(e.getMessage());
            } else {
                throw e;
            }
        }
    }
    


    public String postManagedAccountWithKey(String systemID, String accountName, String password, String dssKey, String description, 
            String passwordRuleID, String apiEnabled, String releaseNotificationEmail, String changeServices,
            String restartServices, String releaseDuration, String maxReleaseDuration, String isaReleaseDuration,
            String autoManagement, String checkPasswordFlag, String resetPasswordOnMismatch, 
            String changePasswordAfterAnyRelease, String changeFrequencyType, String changeFrequencyDays,
            String changeTime) throws APIException
    {
        /*         String body = formatMessageBody(new String[]{"AccountName", "Password", "PrivateKey", "Passphrase", "DSSAutoManagementFlag", "Description", "PasswordRuleID", "ApiEnabled",
                    "ReleaseNotificationEmail", "ChangeServicesFlag", "RestartServicesFlag", "ReleaseDuration", "MaxReleaseDuration",
                    "ISAReleaseDuration", "AutoManagementFlag", "CheckPasswordFlag", "ResetPasswordOnMismatchFlag", 
                    "ChangePasswordAfterAnyReleaseFlag", "ChangeFrequencyType", "ChangeFrequencyDays", "ChangeTime"},
            new String[]{jsonString(accountName), jsonString(password), jsonString(dssKey), "12345", "true", jsonString(description), passwordRuleID, apiEnabled,
                    jsonString(releaseNotificationEmail), changeServices, restartServices, releaseDuration, maxReleaseDuration,
                    isaReleaseDuration, autoManagement, checkPasswordFlag, resetPasswordOnMismatch, changePasswordAfterAnyRelease,
                    jsonString(changeFrequencyType), changeFrequencyDays, jsonString(changeTime)}); */
        
        String body = formatMessageBody(new String[]{"AccountName", "Password", "DSSAutoManagementFlag", "Description", "PasswordRuleID", "ApiEnabled",
                    "ReleaseNotificationEmail", "ChangeServicesFlag", "RestartServicesFlag", "ReleaseDuration", "MaxReleaseDuration",
                    "ISAReleaseDuration", "AutoManagementFlag", "CheckPasswordFlag", "ResetPasswordOnMismatchFlag", 
                    "ChangePasswordAfterAnyReleaseFlag", "ChangeFrequencyType", "ChangeFrequencyDays", "ChangeTime"},
            new String[]{jsonString(accountName), jsonString(password), "true", jsonString(description), passwordRuleID, apiEnabled,
                    jsonString(releaseNotificationEmail), changeServices, restartServices, releaseDuration, maxReleaseDuration,
                    isaReleaseDuration, autoManagement, checkPasswordFlag, resetPasswordOnMismatch, changePasswordAfterAnyRelease,
                    jsonString(changeFrequencyType), changeFrequencyDays, jsonString(changeTime)});

        
        try {
            return post("ManagedSystems/" + systemID + "/ManagedAccounts", body);
        } catch (APIException e) {
            if (e.getMessage().indexOf(" already exists") >= 0) {
                throw new AlreadyExistException(e.getMessage());
            } else {
                throw e;
            }
        }
    }
    



    public String addAccountAttribute(String managedAccountID, String attributeID) throws APIException {
        return post("ManagedAccounts/" + managedAccountID + "/Attributes/" + attributeID, "{}");
    }
    
    public String deleteAccountAttribute(String managedAccountID, String attributeID) throws APIException {
        return delete("ManagedAccounts/" + managedAccountID + "/Attributes/" + attributeID, "{}");
    }

    
    public String addSyncedAccount(String parent, String child) throws APIException {
        return post("ManagedAccounts/" + parent + "/SyncedAccounts/" + child, "{}");
    }


    public String deleteSyncedAccount(String parent, String child) throws APIException {
        return delete("ManagedAccounts/" + parent + "/SyncedAccounts/" + child, "{}");
    }
    
    public String deleteAllSyncedAccounts(String parent) throws APIException {
        return delete("ManagedAccounts/" + parent + "/SyncedAccounts", "{}");
    }

    public List getSyncedAccounts(String parent) throws APIException {
        String str = get("ManagedAccounts/" + parent + "/SyncedAccounts");
        return Utils.jsonToList(str);
    }    
    
    public String executeSmartRule(String ruleId) throws APIException {
        return post("SmartRules/"+ ruleId + "/Process", "{}");
    }
    
    
    public String postSmartRule(String accountID, String title) throws APIException
    {
        String body = formatMessageBody(new String[]{"AccountId", "Title"}, 
                new String[]{accountID, jsonString(title)});
        
        return post("SmartRules/FilterSingleAccount", body);
    }
    
    public String postUserGroupsBI(String groupName, String description) throws APIException
    {
        String body = formatMessageBody(new String[]{"groupType", "groupName", "description"},
        new String[]{"\"BeyondInsight\"", jsonString(groupName), jsonString(description)});
        
        return post("Usergroups", body);
    }
    
    public String postUserGroupsLDAP(String groupName, String groupDistinguishedName,
            String hostName, String bindUser, String bindPassword, String port, String useSSL,
            String membershipAttribute, String accountAtrribute, String permissions, String smartRuleAccess) throws APIException
    {
        String body = formatMessageBody(new String[]{"groupType", "groupName", "groupDistinguishedName", "hostName", "bindUser",
                "bindPassword", "port", "useSSL", "membershipAttribute", "accountAttribute", "Permissions", "SmartRuleAccess"},
                new String[]{"\"LdapDirectory\"", jsonString(groupName), jsonString(groupDistinguishedName), jsonString(hostName),
                        jsonString(bindUser), jsonString(bindPassword), port, useSSL, jsonString(membershipAttribute),
                        jsonString(accountAtrribute), permissions, smartRuleAccess});

        return post("Usergroups", body);
    }
    
    public String putCredentials(String accountId, String password, String publicKey, String privateKey, 
            String passphrase, String updateSystem) throws APIException
    {
        String body = formatMessageBody(new String[]{"Password", "PublicKey", "PrivateKey", "Passphrase", "UpdateSystem"}, 
                new String[]{jsonString(password), jsonString(publicKey), jsonString(privateKey), 
                    jsonString(passphrase), updateSystem});

        return put("ManagedAccounts/" + accountId + "/Credentials", body);
    }

    
    
    /**
     * Takes a list of keys and values and returns a message body that's formatted in the way it will
     * be posted.
     * @param keys The keys
     * @param values The values
     * @return JSON formatted String
     */
    public String formatMessageBody(String[] keys, String[] values)
    {
        StringBuilder sb = new StringBuilder("{");

        for (int i=0; i < keys.length; i++)
        {
            sb.append("\"").append(keys[i]).append("\":").append(values[i]).append(", ");
        }
        sb.delete(sb.length()-2, sb.length());
        sb.append("}");
        
        return sb.toString();
    }
    
    
    /**
     * Returns an image of the RDP Replay session 
     *
     * @param id The UUID for the session, this is the id value from the Replay Session object returned by postReplay
     * @return Image object containing the output of the replay session
     * @throws Client.APIException
     */
    public Image getReplayImage(String id, int scale) throws Client.APIException {
        String path = String.format("pbsm/replay/%s?jpeg=%d", id, scale);
        
        return getImage(path);
    }

    /**
     * Returns the text of the SSH Replay session 
     *
     * @param id The UUID for the session, this is the id value from the Replay Session object returned by postReplay
     * @return String object containing the output of the replay session
     * @throws Client.APIException
     */
    public String getReplayScreen(String id) throws Client.APIException {
        String path = String.format("pbsm/replay/%s?screen=1&html=1&scrollback=0", id);
        
        return get(path);
    }

    /**
     * Simple exception class to wrap Password Safe API errors
     */
    public class APIException extends java.lang.Exception {

        public APIException(String reason) {
            super(reason);
        }

        public APIException(String reason, Throwable throwable) {
            super(reason, throwable);
        }
    }

    public class AlreadyExistException extends APIException {

        public AlreadyExistException(String reason) {
            super(reason);
        }

        public AlreadyExistException(String reason, Throwable throwable) {
            super(reason, throwable);
        }
    }



    /**
     * Escape String value to make a safe JSON String
     *
     * @param str String value to be encoded
     * @return Encoded String safe for use in JSON
     */
    private static String jsonString(String str) {
        StringBuilder sb = new StringBuilder();

        sb.append('"');

        if (str != null) {
            int len = str.length();

            for (int i = 0; i < len; i += 1) {
                char c = str.charAt(i);

                if (c < 0x20 || c > 0x7e) {
                    sb.append("\\u").append(String.format("%04x", (int) c));
                } else {
                    switch (c) {
                        case '"':
                        case '/':
                        case '\\':
                            sb.append('\\');
                            sb.append(c);
                            break;
                        case '\b':
                            sb.append("\\b");
                            break;
                        case '\f':
                            sb.append("\\f");
                            break;
                        case '\n':
                            sb.append("\\n");
                            break;
                        case '\r':
                            sb.append("\\r");
                            break;
                        case '\t':
                            sb.append("\\t");
                            break;
                        default:
                            sb.append(c);
                    }
                }
            }
        }

        sb.append('"');

        return sb.toString();
    }
}
