package com.ihm.pam;

public interface Constants {
    
    String ASSET_GROUP = "BEYONDTRUST WORKGROUP";
    
    String ATT_TYPE_APPLICATION = "Application";
    
    String ATT_TYPE_PLATFORM = "Platform";
    String PLATFORM_WINDOWS = "Windows";
    String PLATFORM_LINUX = "Linux";
    String PLATFORM_AIX = "AIX";
    String PLATFORM_ORACLE = "Oracle";
    String PLATFORM_MSSQL = "MS SQL Server";
    String[] PLATFORMS = {Constants.PLATFORM_WINDOWS, Constants.PLATFORM_LINUX, Constants.PLATFORM_AIX, Constants.PLATFORM_ORACLE, Constants.PLATFORM_MSSQL};

    
    String ATT_TYPE_ACCOUNT_TYPE = "AccountType";
    String ACCOUNT_TYPE_APP_NAMED = "App_Named";
    String ACCOUNT_TYPE_APP_NAMED_VAULT = "App_Named_Vault";
    String ACCOUNT_TYPE_PLATFORM_NAMED = "Platform_Named";
    String ACCOUNT_TYPE_PLATFORM_NAMED_VAULT = "Platform_Named_Vault";
    String ACCOUNT_TYPE_APP_SHARED = "App_Shared";
    String ACCOUNT_TYPE_APP_SHARED_VAULT = "App_Shared_Vault";
    String ACCOUNT_TYPE_PLATFORM_SHARED = "Platform_Shared";
    String ACCOUNT_TYPE_PLATFORM_SHARED_VAULT = "Platform_Shared_Vault";
    String ACCOUNT_TYPE_DEFAULT = "Default";
    String ACCOUNT_TYPE_DEFAULT_VAULT = "Default_Vault";
    String ACCOUNT_TYPE_APP_SERVICE = "App_Service";
    String ACCOUNT_TYPE_APP_SERVICE_VAULT = "App_Service_Vault";
    String ACCOUNT_TYPE_PLATFORM_SERVICE = "Platform_Service";  //remove
    String ACCOUNT_TYPE_PLATFORM_SERVICE_VAULT = "Platform_Service_Vault";  //remove
    String ACCOUNT_TYPE_VENDOR = "Vendor";  //for app only
    String ACCOUNT_TYPE_VENDOR_VAULT = "Vendor_Vault";
    
    String[] ACCOUNT_TYPES = {ACCOUNT_TYPE_APP_NAMED, ACCOUNT_TYPE_APP_NAMED_VAULT, ACCOUNT_TYPE_PLATFORM_NAMED,
                              ACCOUNT_TYPE_PLATFORM_NAMED_VAULT, ACCOUNT_TYPE_APP_SHARED,
                              ACCOUNT_TYPE_APP_SHARED_VAULT, ACCOUNT_TYPE_PLATFORM_SHARED, ACCOUNT_TYPE_PLATFORM_SHARED_VAULT,
                              ACCOUNT_TYPE_DEFAULT, ACCOUNT_TYPE_DEFAULT_VAULT, ACCOUNT_TYPE_PLATFORM_SERVICE, ACCOUNT_TYPE_PLATFORM_SERVICE_VAULT,
                              ACCOUNT_TYPE_APP_SERVICE, ACCOUNT_TYPE_APP_SERVICE_VAULT,
                              ACCOUNT_TYPE_VENDOR, ACCOUNT_TYPE_VENDOR_VAULT};
                            
    
}
