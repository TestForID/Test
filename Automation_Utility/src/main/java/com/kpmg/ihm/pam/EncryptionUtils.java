
package com.kpmg.ihm.pam;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.util.Arrays;


import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import javax.xml.bind.DatatypeConverter;

import com.kpmg.ihm.pam.ConfigUtils;


/**
 * Encryption utilities.  AES
 */
public class EncryptionUtils {
    private EncryptionUtils() {
        super();
    }
    public static final String ENC_SIG = "{AES}";
    private static SecureRandom random = new SecureRandom(Long.toString(System.currentTimeMillis()).getBytes());
    private static final Logger LOGGER = Logger.getLogger(EncryptionUtils.class.getName());
    private static final String ALG = "AES/CBC/PKCS5Padding";
    private static final String SEED_KEY = "system.key";
    
    private static String systemKey = null;
    
    public static void init() {
        
        String seed = ConfigUtils.getString(SEED_KEY);
        if (seed == null) {
            LOGGER.log(Level.SEVERE,"No system key configured!");
            throw new RuntimeException(SEED_KEY + " is not found in config!");
        }
        systemKey = getPBKey(seed);
    }
    
    

    
    public static String generateKey() {
        byte[] ivBytes = getRandom16Bytes();
        return DatatypeConverter.printBase64Binary(ivBytes);
    }

    public static String encrypt(String input) {
        if (systemKey == null) init();
        return encrypt(systemKey, input);
    }
    
    
    public static String decrypt(String encryptedStr) {
        if (systemKey == null) init();
        return decrypt(systemKey, encryptedStr);
    }
    
    
    public static String encrypt(String keyStr, String input) {
        try {
            byte[] randomBytes = DatatypeConverter.parseBase64Binary(keyStr);
            SecretKeySpec key = new SecretKeySpec(randomBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALG);
            
            byte[] ivBytes = getRandom16Bytes();
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encrypted = cipher.doFinal(input.getBytes());
            StringBuilder sb = new StringBuilder(ENC_SIG).append(DatatypeConverter.printBase64Binary(ivBytes))
                .append(",").append(DatatypeConverter.printBase64Binary(encrypted));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);        
        }
    }
    
    public static String decrypt(String keyStr, String encryptedStr) {
        try {
            if (!encryptedStr.startsWith(ENC_SIG)) throw new RuntimeException("No encrypted by this provider!");
            String based64Result = encryptedStr.substring(ENC_SIG.length());
            byte[] randomBytes = DatatypeConverter.parseBase64Binary(keyStr);
            SecretKeySpec key = new SecretKeySpec(randomBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALG);

            int pos = based64Result.indexOf(',');
            byte[] ivBytes = DatatypeConverter.parseBase64Binary(based64Result.substring(0, pos).trim());
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decrypted = cipher.doFinal(DatatypeConverter.parseBase64Binary(based64Result.substring(pos + 1).trim()));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static byte[] getRandom16Bytes() {
        byte[] key = null;
        try {
            String salt = Long.toHexString(random.nextLong());
            key = (salt + System.currentTimeMillis()).getBytes();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return key;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static String getPBKey(String password) {
        byte[] key = null;
        try {
            key = (password).getBytes();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return DatatypeConverter.printBase64Binary(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        ConfigUtils.init(null);
        init();
        if (args == null || args.length != 1) {
            System.out.println("Argument missing!  Please provide text to be encrypted.");
            return;
        }
        System.out.println(EncryptionUtils.encrypt(args[0]));
    }
    
}
