package basicpbpsdemo;

import basicpbpsdemo.Client.APIException;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import com.kpmg.ihm.pam.ConfigUtils;

import java.security.cert.X509Certificate;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * Client session manager, refresh session every 10 min in current access environment.
 */
public class ClientManager {
    public ClientManager() {
        super();
    }


    private static Client instance = null;
    private static boolean initialized = false;
    private static final Logger LOGGER = Logger.getLogger(ClientManager.class.getName());
    private static long lastInstaneTime = 0;

    public static synchronized Client getInstance() {

        long now = System.currentTimeMillis();
        
        if (instance == null || (now - lastInstaneTime) > 600000) { //10 min to create a new connection
            instance = getNewInstance();
            lastInstaneTime = now;  
        }
        return instance;
    }


    private static void init() {
        if (initialized)
            return;

        try { //TODO: set trust later

            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        initialized = true;

    }

    private static synchronized Client getNewInstance() {
        init();
        // BeyondInsight server hostname
        String host = ConfigUtils.getString("PasswordSafeHost"); // We need url for API_Local User_Host_ID

        String user = ConfigUtils.getString("PasswordSafeUser"); // Pass API_Local User

        LOGGER.info("Establishing connection to " + host + " with user = " + user);

        // BeyondInsight API Key
        String apiKey = ConfigUtils.getString("PasswordSafeApiKey");

        try {
            Client client = new Client(host);
            // Sign In
            String jsonUserObjectStr = client.signAppIn(user, apiKey);
            JsonObject joUser = Json.parse(jsonUserObjectStr).asObject();
            LOGGER.info("Signed in as " + joUser.getString("UserName", ""));

            instance = client;

        } catch (APIException apie) {
            LOGGER.log(Level.SEVERE, "Error during API initialization!", apie);
            throw new RuntimeException(apie);
        }

        return instance;
    }


    public static Client createClient(String host, String user, String apiKey) {
        init();

        LOGGER.info("Establishing connection to " + host + " with user = " + user);

        // BeyondInsight API Key

        try {
            Client client = new Client(host);
            // Sign In
            String jsonUserObjectStr = client.signAppIn(user, apiKey);
            JsonObject joUser = Json.parse(jsonUserObjectStr).asObject();
            LOGGER.info("Signed in as " + joUser.getString("UserName", "") + " on " + host);
            return client;

        } catch (APIException apie) {
            LOGGER.log(Level.SEVERE, "Error during API initialization!", apie);
            throw new RuntimeException(apie);
        }
        
    }





}
