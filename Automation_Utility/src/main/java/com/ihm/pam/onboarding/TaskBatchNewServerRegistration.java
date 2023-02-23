package com.ihm.pam.onboarding;

import basicpbpsdemo.Client;
import basicpbpsdemo.Client.APIException;
import basicpbpsdemo.Client.AlreadyExistException;
import basicpbpsdemo.ClientManager;

import com.ihm.pam.CommonUtils;
import com.ihm.pam.Constants;
import com.ihm.pam.monitoring.MonitoringUtils;
import com.kpmg.ihm.pam.ConfigUtils;
import com.kpmg.ihm.pam.Utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class TaskBatchNewServerRegistration {
	public TaskBatchNewServerRegistration() {
		super();
	}
	private static final Logger LOGGER = Logger.getLogger(TaskBatchNewServerRegistration.class.getName());
	private static final String OPTION_PREFIX = "onboarding.";
	private static HashSet<String> acctSmartRules = new HashSet<String>();
	private static String testResult="";

	public static void main(String[] args) {
		//System.out.println("hete");

		if (args == null || args.length != 1) {
			LOGGER.info("Please provide CSV file as input.");
			System.exit(1);
		}
		 
		//String csvFile = "C:\\Users\\rbhattacharjee4\\Desktop\\newservers.csv";
		String csvFile = args[0];
		//               csvFile = "/mnt/hgfs/newshare/newserver.csv";

		//ArrayList<String> duplicateIp = new ArrayList<String>();
		HashSet<String> platforms = new HashSet<String>();
		//HashSet<String> acctSmartRulesv = new HashSet<String>();

		
		String platform =null;
		String operatingSystem="test";

		try {
			Reader in = new FileReader(csvFile);

			acctSmartRules.clear();
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().withIgnoreEmptyLines().parse(in);
			int count = 1;
			//in.close();

			for (CSVRecord record : records) {
				String line = String.valueOf(record);
				line = line.trim();
				if(line.isEmpty())
					break;
				Client client = ClientManager.getInstance();
				//System.out.println(record.size());
				String assetID=null;
				Map<String, String> map = record.toMap();
				LOGGER.info("Processing : " + map + " " + (count++));
				String ip = map.get("ip");
				platform = map.get("platform");
				if(platform == null || platform.isEmpty())
				{
					LOGGER.severe("Platform is Invalid. Ignoring the entry");
					continue;
				}else if (platform.equalsIgnoreCase(Constants.PLATFORM_AIX) || platform.equalsIgnoreCase(Constants.PLATFORM_LINUX) || platform.equalsIgnoreCase(Constants.PLATFORM_WINDOWS) ||platform.equalsIgnoreCase(Constants.PLATFORM_ORACLE) || platform.equalsIgnoreCase( Constants.PLATFORM_MSSQL)) {
					LOGGER.severe("Platform is valid.");
				}else {
					LOGGER.severe("Platform is Invalid. Ignoring the entry");
					continue;
				}
				String name=map.get("name");
				String onboarded=map.get("onboarded");
				//System.out.println("hete");
				platforms.add(platform);
				if (onboarded != null && onboarded.equalsIgnoreCase("Yes"))
				{
					//System.out.println("hete");
					LOGGER.info("Asset already onboarded. Asset "+ip+" is being ignored");
					continue;
				}
				String oldEntry=platform+","+ip+","+name+","+",";
				String newEntry=platform+","+ip+","+name+","+"Yes"+",";
				if (!Utils.validate(ip)) {
					//throw new RuntimeException("Invalid/NULL IP in asset: " + platform + "/" + ip);
					LOGGER.severe("*************Invalid/NULL IP********* asset details "+platform + "/" + ip);
					continue;
				}
				String host = MonitoringUtils.findHostFromIp(ip);
				List list = client.searchAssetByIP(ip);
				//System.out.println(Arrays.toString(list.toArray()));
				if (list != null && list.size() == 1) {

					for (int i = 0; i < list.size(); i++) {
						if(list.get(i).toString().contains(ip)) {
							//duplicateIp.add(ip);
							String[] os=list.get(i).toString().split(",");
							String[] os2=os[8].split("=");
							if(os2[1] == null && os[2].isEmpty()) {
								operatingSystem="Empty";
							}else {
								operatingSystem=os2[1];
							}
						}
					}
				}
				if (list == null || list.size() == 0)
				{
					if(platform.equalsIgnoreCase(Constants.PLATFORM_AIX) || platform.equalsIgnoreCase(Constants.PLATFORM_LINUX) || platform.equalsIgnoreCase(Constants.PLATFORM_WINDOWS))
					{
						// new IP, doesn't exist.
						testResult="";
						String onboarded_P=onBoardPlatform(platform, ip);
						if(onboarded_P.equalsIgnoreCase("Successful"))
						{
							newEntry=newEntry+testResult;
							updateEntry(csvFile,oldEntry,newEntry);

						}else if (onboarded_P.equalsIgnoreCase("Unsuccessful")) {
							String entryyWithError=oldEntry+testResult;
							updateEntry(csvFile,oldEntry,entryyWithError);
						}else {
							throw new RuntimeException("The asset is not onboarded! ip = " + ip);
						}

					}else if (platform.equalsIgnoreCase(Constants.PLATFORM_ORACLE) || platform.equalsIgnoreCase( Constants.PLATFORM_MSSQL)) 
					{
						LOGGER.severe("Database onboarding request is recieved before the Asset is onboarded for asset "+ip);
						if(platform.equalsIgnoreCase(Constants.PLATFORM_MSSQL))
						{
							String asset_Platform=ConfigUtils.getString("DefaultPlatform." +Constants.PLATFORM_MSSQL, null);
							testResult="";
							String onboarded_P=onBoardPlatform(asset_Platform, ip);
							if(onboarded_P.equalsIgnoreCase("Successful"))
							{
								//ONBOARD THE DATABASES
								//testResult="";
								String onboardedDatabase=onBoardDatabase(platform, ip, client);
								if(onboardedDatabase.equalsIgnoreCase("Successful"))
								{
									newEntry=newEntry+testResult;
									updateEntry(csvFile,oldEntry,newEntry);

								}else {
									LOGGER.severe("No databases onBoarded for asset " + ip+" having the database platform as "+platform);
								}

							}else if (onboarded_P.equalsIgnoreCase("Unsuccessful")) {
								String entryyWithError=oldEntry+"Asset for OS "+asset_Platform+" not onboarded. "+testResult;
								updateEntry(csvFile,oldEntry,entryyWithError);
							}else {
								throw new RuntimeException("The asset is not onboarded! ip = " + ip);								
							}							
						}else if (platform.equalsIgnoreCase(Constants.PLATFORM_ORACLE)) {

							String asset_Platform=ConfigUtils.getString("DefaultPlatform." +Constants.PLATFORM_ORACLE, null);
							testResult="";
							String onboarded_P=onBoardPlatform(asset_Platform, ip);
							if(onboarded_P.equalsIgnoreCase("Successful"))
							{
								//ONBOARD THE DATABASES
								//testResult="";
								String onboardedDatabase=onBoardDatabase(platform, ip, client);
								if(onboardedDatabase.equalsIgnoreCase("Successful"))
								{
									newEntry=newEntry+testResult;
									updateEntry(csvFile,oldEntry,newEntry);

								}else {
									LOGGER.severe("No databases onBoarded for asset " + ip+" having the database platform as "+platform);
								}
							}else if (onboarded_P.equalsIgnoreCase("Unsuccessful")) {
								String entryyWithError=oldEntry+"Asset for OS "+asset_Platform+" not onboarded. "+testResult;
								updateEntry(csvFile,oldEntry,entryyWithError);
							}else {
								throw new RuntimeException("The asset is not onboarded! ip = " + ip);								
							}		

						}

					}
				}else if(list.size()<=1) {
					if(platform.equalsIgnoreCase(Constants.PLATFORM_AIX) || platform.equalsIgnoreCase(Constants.PLATFORM_LINUX) || platform.equalsIgnoreCase(Constants.PLATFORM_WINDOWS))
					{
						LOGGER.severe("Duplicate request for Asset addition for asset "+ip);
						if(operatingSystem.equalsIgnoreCase(platform) || operatingSystem.contains(platform))
						{
							LOGGER.info("Asset already onboarded. Updating the entry");
							updateEntry(csvFile,oldEntry,newEntry);
						}else if(operatingSystem.equalsIgnoreCase("test") || operatingSystem.equalsIgnoreCase("Empty")) {
							LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
							LOGGER.severe("Request for duplicate asset addition for asset "+ip);
							newEntry=platform+","+ip+","+name+","+"Yes"+",Request for duplicate asset addition for asset OS could not be determined";
							updateEntry(csvFile,oldEntry,newEntry);
							
							
						}else
						{
							LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
							LOGGER.severe("Request for new asset addition for asset "+ip+" has different OS than onboarded asset os");
							LOGGER.severe("Asset Onboarded has OS as "+operatingSystem);
							LOGGER.severe("NEW ASSET onboarding request has OS as "+platform);
							newEntry=platform+","+ip+","+name+","+"Yes"+",Request for duplicate asset addition. New request has different OS. OLD OS is"+operatingSystem;
							updateEntry(csvFile,oldEntry,newEntry);

						}
					}else if (platform.equalsIgnoreCase(Constants.PLATFORM_ORACLE) || platform.equalsIgnoreCase( Constants.PLATFORM_MSSQL)) {

						//ONBOARD THE DATABASES
						testResult="";
						String onboardedDatabase=onBoardDatabase(platform, ip, client);
						//System.out.println(onboardedDatabase+" for "+ip);
						if(onboardedDatabase.equalsIgnoreCase("Successful"))
						{
							newEntry=newEntry+testResult;
							updateEntry(csvFile,oldEntry,newEntry);

						}else {
							LOGGER.severe("No databases onBoarded for asset " + ip+" having the database platform as "+platform);
						}
					}
				}else if (list.size()>1) {
					LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					LOGGER.severe("Same IP assigned to multiple DNS : "+ip);
					LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					newEntry=platform+","+ip+","+name+","+"Yes"+",Request for duplicate asset addition. Same IP assigned to multiple DNS";
					updateEntry(csvFile,oldEntry,newEntry);
					}
			}// each record
			System.out.println("************************************");
			//acctSmartRules.forEach(System.out::println);
			//System.out.println();
			for (String acctSmartRule : acctSmartRules) {
				System.out.println("Account Smart rule "+ acctSmartRule);
				String sgId = CommonUtils.getSmartRuleId(acctSmartRule);
				LOGGER.info("Got the account management smart rule id = " + sgId + " for " + acctSmartRule
						+ ", executing it ...");
				ClientManager.getInstance().executeSmartRule(sgId);
			}
			
		/*	if (duplicateIp.size()>1) {
				LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				LOGGER.severe("DUPLICATE IP LIST:");
				for (String ips : duplicateIp) {
					LOGGER.severe(ips); 
				  	} 
				LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				
			}*/

		}catch (Throwable th) {
			LOGGER.log(Level.SEVERE, "Unexpected error!", th);
		}
	}

	private static String onBoardPlatform(String platform, String ip) throws APIException, InterruptedException {
		// TODO Auto-generated method stub
		String host = MonitoringUtils.findHostFromIp(ip);
		Map asset=null;
		String assetId=null;
		
		LOGGER.info("Creating new assert for " + platform + "/" + ip + "-" + host + "...");

		String resp =
				ClientManager.getInstance().postAsset(Constants.ASSET_GROUP, host, host, null, ip, null, null);
		asset = Utils.jsonToMap(resp);
		assetId = Utils.toIDString(asset.get("AssetID"));
		

		LOGGER.info("Created new assert for " + platform + "/" + ip);
		Map<String, String> attTypes = ClientManager.getInstance().getAttributeTypes();
		String acctTypeAttId = attTypes.get(Constants.ATT_TYPE_PLATFORM);
		Map<String, String> acctTypeAtts = ClientManager.getInstance().getAttributesForType(acctTypeAttId);
		String attValueId = acctTypeAtts.get(platform);
		if (attValueId == null) {
			LOGGER.severe("Expect attribute type " + Constants.ATT_TYPE_PLATFORM + "/" +
					platform + " exist already!");
			testResult=testResult+"Expect attribute type " + Constants.ATT_TYPE_PLATFORM + "/" +
					platform + " exist already!";
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
				LOGGER.info("System is not managed yet, wait 10 second ...");
				Thread.sleep(10000);
			} else {
				sysID = Utils.toIDString(mgdSys.get("ManagedSystemID"));
				break;
			}
		}

		if (sysID == null) {
			
			LOGGER.severe("The asset is not managed by smart rule! ip = " + ip);
			testResult=testResult+"The asset is not managed by smart rule! Manage the Asset manually";
			return "Unsuccessful";
		}

		String acctStr = ConfigUtils.getString("DefaultAccounts." + platform + ".accounts", null);
		if (acctStr == null) {
			throw new RuntimeException("DefaultAccount for " + platform +" is not configured: DefaultAccounts." + platform + ".accounts");
		}

		String acctSmartRule = ConfigUtils.getString("DefaultAccounts." + platform + ".smartrule", null);
		if (acctSmartRule == null) {
			throw new RuntimeException("smart rule for " + platform +
					" is not configured: DefaultAccounts." + platform + ".smartrule");
		}
		acctSmartRules.add(acctSmartRule);

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
							null, "false", null, null, null, null, null, null, "false", null, null,
							null, null, null, null);
			LOGGER.info("Managed account : " + respStr);

		}
		return "Successful";
	}
	

	private static String onBoardDatabase(String platform, String ip, Client client) throws APIException, InterruptedException
	{
		String returnStatement="noDB";

		String sysNetBiosName = ConfigUtils.getString(OPTION_PREFIX + "sysNetBiosName", null);
		String sysContactEmail = ConfigUtils.getString(OPTION_PREFIX + "sysContactEmail", null);
		String sysDescription = ConfigUtils.getString(OPTION_PREFIX + "sysDescription", "Onboarded by Script");
		String sysPort = ConfigUtils.getString(OPTION_PREFIX + "sysPort", null);
		String sysTimeout = ConfigUtils.getString(OPTION_PREFIX + "sysTimeout", null);
		String sysReleaseDuration = ConfigUtils.getString(OPTION_PREFIX + "sysReleaseDuration", null);
		String sysMaxReleaseDuration = ConfigUtils.getString(OPTION_PREFIX + "sysMaxReleaseDuration", null);
		String sysIsaReleaseDuration = ConfigUtils.getString(OPTION_PREFIX + "sysIsaReleaseDuration", null);
		String sysAutoManagementFlag = ConfigUtils.getString(OPTION_PREFIX + "sysAutoManagementFlag", null);
		String sysElevationCommand = ConfigUtils.getString(OPTION_PREFIX + "sysElevationCommand", null);
		String sysCheckPassword = ConfigUtils.getString(OPTION_PREFIX + "sysCheckPassword", null);

		String sysChangePasswordAfterAnyRelease = ConfigUtils.getString(OPTION_PREFIX + "sysChangePasswordAfterAnyRelease", "false");
		String sysResetPasswordOnMismatch = ConfigUtils.getString(OPTION_PREFIX + "sysResetPasswordOnMismatch", "false");
		String sysChangeFrequencyType = ConfigUtils.getString(OPTION_PREFIX + "sysChangeFrequencyType", "xdays");
		String sysChangeFrequencyDays = ConfigUtils.getString(OPTION_PREFIX + "sysChangeFrequencyDays", "30");
		String sysChangeTime = ConfigUtils.getString(OPTION_PREFIX + "sysChangeTime", "00:00");



		String acctPassword = "this is not the right password";
		String acctDescription = ConfigUtils.getString(OPTION_PREFIX + "acctDescription", "Onboarded by Script");
		String acctApiEnabled = ConfigUtils.getString(OPTION_PREFIX + "acctApiEnabled", "true");
		String acctReleaseNotificationEmail = ConfigUtils.getString(OPTION_PREFIX + "acctReleaseNotificationEmail", null);
		String acctChangeServices = ConfigUtils.getString(OPTION_PREFIX + "acctChangeServices", null);
		String acctRestartServices = ConfigUtils.getString(OPTION_PREFIX + "acctRestartServices", null);
		String acctAutoManagement = ConfigUtils.getString(OPTION_PREFIX + "acctAutoManagement", "false");
		String acctCheckPasswordFlag = ConfigUtils.getString(OPTION_PREFIX + "acctCheckPasswordFlag", "false");
		String acctResetPasswordOnMismatch = ConfigUtils.getString(OPTION_PREFIX + "acctResetPasswordOnMismatch", "false");
		String changePasswordAfterAnyRelease = ConfigUtils.getString(OPTION_PREFIX + "changePasswordAfterAnyRelease", "false");
		String acctReleaseDuration = ConfigUtils.getString(OPTION_PREFIX + "acctReleaseDuration", null);
		String acctMaxReleaseDuration = ConfigUtils.getString(OPTION_PREFIX + "acctMaxReleaseDuration", null);
		String acctIsaReleaseDuration = ConfigUtils.getString(OPTION_PREFIX + "acctIsaReleaseDuration", null);
		String acctChangePasswordAfterAnyRelease = ConfigUtils.getString(OPTION_PREFIX + "acctChangePasswordAfterAnyRelease", "true");
		String acctChangeFrequencyType = ConfigUtils.getString(OPTION_PREFIX + "acctChangeFrequencyType", "xdays");
		String acctChangeFrequencyDays = ConfigUtils.getString(OPTION_PREFIX + "acctChangeFrequencyDays", "60");
		String acctChangeTime = ConfigUtils.getString(OPTION_PREFIX + "acctChangeTime", "00:00");

		String host = MonitoringUtils.findHostFromIp(ip);
		LOGGER.info("Fetching the asset id for " + platform + "/" + ip + "-" + host + "...");
		//Map asset = null;
		String assetId = null ;
		
		List list = client.searchAssetByIP(ip);
		String[] os=list.get(0).toString().split(",");
		
		String[] iD=os[1].split("=");
		//System.out.println(iD[1]);
		String[] astid=iD[1].toString().split("\\.");
		// System.out.println(iD[1].substring(0, iD[1].length() - 2));
		LOGGER.info("Asset id is "+astid[0]);
		assetId=astid[0];
		/*try {
			String resp =ClientManager.getInstance().postAsset(Constants.ASSET_GROUP, host, host,null, ip, null, null);
			asset = Utils.jsonToMap(resp); 
			assetId =Utils.toIDString(asset.get("AssetID"));
			LOGGER.severe("!!!!!!!!!!!! Asset previously not onboarded before the database onboarding process !!!!!!!!!!!!!!!!!!!!");
			LOGGER.severe("Please check the details for asset "+ip);

		} catch (AlreadyExistException e) {
			String [] test=e.getMessage().split(":"); 
			assetId= test[1].replaceAll("\\W","");
		}*/
		LOGGER.info("Asset Id for the asset "+ip+" is "+assetId); 


		Map<String, String> attTypes = ClientManager.getInstance().getAttributeTypes();
		String acctTypeAttId = attTypes.get(Constants.ATT_TYPE_PLATFORM);
		Map<String, String> acctTypeAtts = ClientManager.getInstance().getAttributesForType(acctTypeAttId);
		String attValueId = acctTypeAtts.get(platform);
		if (attValueId == null) {
			LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			LOGGER.severe("Expect attribute type " + Constants.ATT_TYPE_PLATFORM + "/" +
					platform + " exist already!");
			LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}else {
			try {

				ClientManager.getInstance().setAttributeForAsset(assetId, attValueId);
				LOGGER.info("Asset's platform is set to " + platform + "  ASSET details:" + assetId);

				String smartGroupTitle = ConfigUtils
						.getString("monitoring.script.registerNewServer.smartRule." + platform);
				String sgId = CommonUtils.getSmartRuleId(smartGroupTitle);
				LOGGER.info("Got the smart rule id = " + sgId + " for " + smartGroupTitle + ", executing it ...");
				ClientManager.getInstance().executeSmartRule(sgId);


			} catch (APIException e) {
				// TODO: handle exception
				if(e.toString().contains("Asset Attribute exists already")) {
					LOGGER.info("Asset attribute already set for the asset "+ip);
				}
				else {
					throw e;
				}
			}			
		}
		LOGGER.info("Got attriubte id for " + platform + " = " + attValueId);
		LOGGER.info("tagging platform attribute ...");

		
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
			LOGGER.severe("The asset is not managed by smart rule! ip = " + ip);
			testResult=testResult+"The asset is not managed by smart rule!";
		}
		else {
			LOGGER.info("The Managed System ID for asset "+ip+" is "+sysID);
		}


		List platforms_1 = client.getPlatforms();
		Map<String, Map> platformIdMap = new HashMap<String, Map>();
		for (Object p : platforms_1) {
			Map m = (Map) p;
			platformIdMap.put(Utils.toIDString(m.get("PlatformID")), m);
		}

		List passwordRules = client.getPasswordRules();
		List functionalAccounts = client.getFunctionalAccounts();
		List dssKeyRules = client.getDSSKeyRules();

		Map<String, PlatformMeta> platformMap = getPlatformMeta(client);

		LOGGER.info("Checking whehter database exists on the asset " );
		List dbs = client.getAssetDatabases(assetId);
		int count=0;

		if (dbs == null || dbs.size() == 0) {
			LOGGER.severe("No database discovered!!!!!!!!!!!!!!!!!!!");
			return returnStatement;
		} else {

			LOGGER.info("Number of discovered database : " + dbs.size());
			for (Object d : dbs) {
				Map db = (Map) d;
				LOGGER.info("Onboarding database " + d);
				String dbId = Utils.toIDString(db.get("DatabaseID"));
				String dbPlatformId = Utils.toIDString(db.get("PlatformID"));
				LOGGER.info("Database ID is "+dbId);
				LOGGER.info("Database Platform ID is "+dbPlatformId);
				Map mgdDb = client.getManagedSystemByDatabaseId(dbId);
				if (mgdDb == null) {

					Map pf = platformIdMap.get(dbPlatformId);
					String pfName = (String) pf.get("Name");
					pfName = pfName.toUpperCase();
					LOGGER.info("Platform  Name is "+pfName);
					PlatformMeta dbPlatformMeta = null;
					for (String platform1 : platformMap.keySet()) {
						if (pfName.indexOf(platform1) >= 0) {
							dbPlatformMeta = platformMap.get(platform1);
							break;
						}
					}
					if (dbPlatformMeta == null) {
						throw new RuntimeException("No matched configuration in app.cfg for " + pfName);
					}

					String dbPasswordRuleId = getPasswordRuleId(pfName, dbPlatformMeta, passwordRules);
					String dbFunctionalAccountId = getFunctionalAccountId(pfName, dbPlatformMeta, functionalAccounts);

					LOGGER.info("The db Password Rule ID is "+dbPasswordRuleId);
					LOGGER.info("The db Functional Account Id is "+dbFunctionalAccountId);
					String    msg = client.postManagedDatabase(dbId, sysContactEmail, sysDescription,
							sysTimeout, dbPasswordRuleId, sysReleaseDuration, sysMaxReleaseDuration,
							sysIsaReleaseDuration, sysAutoManagementFlag, dbFunctionalAccountId, 
							sysCheckPassword, sysChangePasswordAfterAnyRelease, sysResetPasswordOnMismatch, sysChangeFrequencyType,
							sysChangeFrequencyDays, sysChangeTime);

					LOGGER.info("Reponse from managing database " + msg);

					Map mgdDbDetail = Utils.jsonToMap(msg);
					Object dbIdValue = mgdDbDetail.get("ManagedSystemID");
					String dbSystemID = dbIdValue == null ? null : Utils.toIDString(dbIdValue);
					if (dbSystemID == null) {
						throw new RuntimeException("Unexpcted error, managed database ID not found after managing system! " + msg);
					}

					LOGGER.info("Successfully onboarded database " + pfName);  
					count++;

					for (String accountName : dbPlatformMeta.defaultAccounts) {
						msg = client.postManagedAccount(dbSystemID, accountName, acctPassword, acctDescription, 
								dbPasswordRuleId, acctApiEnabled, acctReleaseNotificationEmail, acctChangeServices,
								acctRestartServices, acctReleaseDuration, acctMaxReleaseDuration, acctIsaReleaseDuration,
								acctAutoManagement, acctCheckPasswordFlag, acctResetPasswordOnMismatch, 
								changePasswordAfterAnyRelease, acctChangeFrequencyType, acctChangeFrequencyDays,
								acctChangeTime);
						LOGGER.info("Response from managing db account : " + msg);

						Map mgdAcct = Utils.jsonToMap(msg);
						if (mgdAcct.get("ManagedAccountID") != null) {
							LOGGER.info("Successfully added managed account " + accountName);
						}

					} //each account

				} else {
					LOGGER.info("Database " + dbId + " already managed, for now ignore it!!!");
					count++;
					
				}
			}//each db

			//System.out.println();
			returnStatement="Successful";

		}//database operation complete if db exists
		//System.out.println(dbs.size());
		//System.out.println(count);
		if(dbs.size()==count)
		{
			returnStatement="Successful";
		}

		return returnStatement;
	}

	private static void updateEntry(String csvFile, String oldEntry, String newEntry) throws IOException {
		Path path = Paths.get(csvFile);
		Stream <String> lines = Files.lines(path);
		List <String> replaced = lines.map(line -> line.replaceAll(oldEntry, newEntry)).collect(Collectors.toList());
		//System.out.println("test"+replaced);
		Files.write(path, replaced);
		lines.close();
		
	}

	private static class PlatformMeta {
		String functionalAccount;
		String passwordRule;
		String dssKeyRule;
		String platFormId;
		String[] defaultAccounts;
	}

	private static Map<String, PlatformMeta> getPlatformMeta(Client client) throws APIException {
		Map<String, PlatformMeta> result = new HashMap<String, PlatformMeta>();

		Map<String, String> faMap = ConfigUtils.getSectionAsMap("FunctionalAccount.");

		// System.out.println("FA MAP SIze"+faMap.size());
		for(Map.Entry<String, String> entry : faMap.entrySet()) {
			PlatformMeta pm = new PlatformMeta();
			pm.functionalAccount = entry.getValue();
			result.put(entry.getKey(), pm);    
		}


		Map<String, String> accts = ConfigUtils.getSectionAsMap("DefaultAccountsDB.");
		//  System.out.println("FA MAP SIze"+accts.size());
		if (accts.size() != faMap.size()) {
			throw new RuntimeException("Default accounts and Functional Account must both exist for each platform in app.cfg!");
		}


		for(Map.Entry<String, String> entry : accts.entrySet()) {
			String platForm = entry.getKey();
			PlatformMeta pm = result.get(platForm);
			if (pm == null) throw new RuntimeException("No functional account found in app.cfg for " + platForm);
			String strs = entry.getValue();
			pm.defaultAccounts = strs.split(",");
		}



		Map<String, String> passMap = ConfigUtils.getSectionAsMap("PasswordRule.");

		if (passMap.size() != faMap.size()) {
			throw new RuntimeException("Password rule and Functional Account must both exist for each platform in app.cfg!");
		}


		for(Map.Entry<String, String> entry : passMap.entrySet()) {
			String platForm = entry.getKey();
			PlatformMeta pm = result.get(platForm);
			if (pm == null) throw new RuntimeException("No functional account found in app.cfg for " + platForm);
			pm.passwordRule = entry.getValue();
		}


		Map<String, String> keyMap = ConfigUtils.getSectionAsMap("DSSKeyRule.");
		for(Map.Entry<String, String> entry : keyMap.entrySet()) {
			String platForm = entry.getKey();
			PlatformMeta pm = result.get(platForm);
			if (pm == null) throw new RuntimeException("No functional account found in app.cfg for " + platForm);
			pm.dssKeyRule = entry.getValue();
		}
		return result;
	}

	private static String getFunctionalAccountId(String name, PlatformMeta platformMeta, List functionalAccounts) {
		LOGGER.info(name + "'s functional acount is " + platformMeta.functionalAccount);
		String functionalAccountId = null;

		for (Object p : functionalAccounts) {
			Map fa = (Map) p;
			//                    String pid = Utils.toIDString(fa.get("PlatformID"));
			//                    if (!platformMeta.platFormId.equals(pid)) continue;
			//Make sure the alias are unique for functional account!!!!!!!!!!!!!!!!!!!!!!

			if (platformMeta.functionalAccount.equalsIgnoreCase((String) fa.get("DisplayName"))) {
				if (functionalAccountId != null) throw new RuntimeException("Multi functional accounts found with name = " + platformMeta.functionalAccount);
				functionalAccountId = Utils.toIDString(fa.get("FunctionalAccountID"));

			}
		}
		if (functionalAccountId == null) throw new RuntimeException("No functional account ID found with name = " + platformMeta.functionalAccount);
		return functionalAccountId;        
	}

	private static String getPasswordRuleId(String name, PlatformMeta platformMeta, List passwordRules) {
		LOGGER.info(name + "'s password rule is " + platformMeta.passwordRule);

		String passwordRuleId = null;


		for (Object p : passwordRules) {
			Map prule = (Map) p;
			if (platformMeta.passwordRule.equalsIgnoreCase((String) prule.get("Name"))) {
				if (passwordRuleId != null) throw new RuntimeException("Multi password rule found with name = " + platformMeta.passwordRule);
				passwordRuleId = Utils.toIDString(prule.get("PasswordRuleID"));
			}
		}


		if (passwordRuleId == null) throw new RuntimeException("No password rule found with name = " + platformMeta.passwordRule);
		LOGGER.info("Found id for password rule = " + passwordRuleId);

		return passwordRuleId;
	}


}