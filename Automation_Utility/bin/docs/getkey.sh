#!/bin/sh

###############################################################################
# Script Output Color formatting
###############################################################################
red=$(tput setaf 1)
green=$(tput setaf 2)
yellow=$(tput setaf 3)
NORMAL=$(tput sgr0)
###############################################################################
# Loading the properties file
###############################################################################
propFileLoc="$(pwd)/script.properties"
if [ -z "$propFileLoc" ]; then echo "${red}Missing Properties file ${NORMAL}" 
	exit 1
elif [ -r "$propFileLoc" ]; then echo "${green}Using properties file :: $propFileLoc ${NORMAL}" 
else
	echo "${red}Error accessing properties file, $propFileLoc ${NORMAL}"
	echo "${red}Check the file path and ensure read permissions are granted. ${NORMAL}"
	exit 1
fi

###############################################################################
# Fetching the properties file as key value
###############################################################################
getProp(){
grep "$1" "$propFileLoc" | cut -d'=' -f2
}

###############################################################################
# Fetching the necessary system and script parameters
###############################################################################
#Following parameters are fetched from the accompanying properties file
baseURL=$(getProp 'pbps.server.api.uri')
echo "Base URL for invoking REST API :: $baseURL"
contentType=$(getProp 'pbps.api.content.type')
dssAuth=$(getProp 'pbps.api.account.dssauth')

###############################################################################
# Initiate API Sign-in to the server
###############################################################################
signin(){
#Sign in
echo "1. Initiating Sign in"
connoper=$(curl -i -v -k -c pbpscookie.txt --request POST --url "${baseURL}/Auth/SignAppin" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);" -H "$contentType" -d "" 2>&1 /dev/null)
signinstat=$(echo "$connoper" | grep ^HTTP | cut -d " " -f 2)
}

###############################################################################
# Logout of the Server
###############################################################################
signout(){
#Sign out	
echo "6. Signing out"
connclose=$(curl --silent -i -k -b pbpscookie.txt POST --url "${baseURL}/Auth/Signout" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);" -H "$contentType" -d "" 2>&1 /dev/null)
signoutstat=$(echo "$connclose" | grep ^HTTP | cut -d " " -f 2)
	[ "$signoutstat" -eq 200 ] && echo "${green}Session closed successfully ${NORMAL}" && [ -e pbpscookie.txt ] && rm -f pbpscookie.txt || echo "${red}Session is still active, Error Code: $signoutstat${NORMAL}" && [ -e pbpscookie.txt ] && rm -f pbpscookie.txt
}

###############################################################################
# Get the Managed Account on Managed Server
###############################################################################
getManagedAccounts(){
systemName=$(getProp pbps.api.managed.server)
accountName=$(getProp pbps.api.managed.account)
echo "2. Get Managed Account $accountName on $systemName"
# Import Asset
getManagedAccounts=$(curl --silent -i -k -b pbpscookie.txt --request GET --url "${baseURL}/ManagedAccounts?systemName=$systemName&accountName=$accountName" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);")
getMngdAcctResponse=$(echo "$getManagedAccounts" | grep ^HTTP | cut -d " " -f 2)
}

###############################################################################
# Create Request
###############################################################################
requests(){
echo "3. Creating Request"
#Check Managed Account
requests=$(curl --silent -i -k -b pbpscookie.txt --request POST --url "${baseURL}/Requests" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);" -H "$contentType" -d "{\"ConflictOption\":\"reuse\",\"SystemID\":$systemID,\"AccountID\":$accountID,\"DurationMinutes\":1}")
	requestsResponse=$(echo "$requests" | grep ^HTTP | cut -d " " -f 2)
}

###############################################################################
# Retreive the credentials
###############################################################################
getCredentials(){
echo "4. Get Credentials"
#Check Managed Account
if [ $dssAuth -eq 1 ]; then echo "${green}Authn mechanism: DSS Key${NORMAL}"
        pass=$(curl --silent -i -k -b pbpscookie.txt --request GET --url "${baseURL}/Credentials/${requestID}?type=passphrase" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);" -H "$contentType" -d "")
        passResponse=$(echo "$pass" | grep ^HTTP | cut -d " " -f 2)
        if [ "$passResponse" -ne 200 ]; then
           echo "${red}Failed to get passphrase! $pass ${NORMAL}"
           signout
           exit
        fi
        passphrase=$(echo "$pass" | tail -n1|tr -d '\"')
        echo $passphrase > pass.txt
	getCredentials=$(curl --silent -i -k -b pbpscookie.txt --request GET --url "${baseURL}/Credentials/${requestID}?type=dsskey" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);" -H "$contentType" -d "")
        echo "$getCredentials"
elif [ $dssAuth -eq 0 ]; then echo "${green}Authn mechanism: Password${NORMAL}"
	getCredentials=$(curl --silent -i -k -b pbpscookie.txt --request GET --url "${baseURL}/Credentials/${requestID}" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);" -H "$contentType" -d "")
else echo "${red}Verify the pbps.api.account.dssauth property${NORMAL}"
fi
getCredentialsResponse=$(echo "$getCredentials" | grep ^HTTP | cut -d " " -f 2) 
}

###############################################################################
# Checkin Account credentials
###############################################################################
checkinCredentials(){
echo "5. Checkin the request for Managed User"
#Checkin credentials
checkinCredentials=$(curl --silent -i -k -b pbpscookie.txt --request PUT --url "${baseURL}/Requests/${requestID}/Checkin" -H "Authorization: PS-Auth key=$(getProp pbps.auth.key); runas=$(getProp pbps.auth.user);" -H "$contentType" -d "{}")
checkinCredentialsResponse=$(echo "$checkinCredentials" | grep ^HTTP | cut -d " " -f 2)
}
###############################################################################
# The workflow of Password Check-out
# 1. Login to Password Safe
# 2. Get the Managed Account on the Managged Server
# 3. Create a request in order to check out the password
# 4. Get the credentials
# 5. Checkin in the credentials
# 6. Sign out of Password Safe API
###############################################################################
echo "Connecting to Password Safe"
signin
if [ -z "$signinstat" ];then echo "${red}Failed to Connect. No response from server ${NORMAL}"
 elif [ "$signinstat" -eq 200 ];then
    echo "${green}Connected ${NORMAL}"
	getManagedAccounts
	if [ "$getMngdAcctResponse" -eq 200 ]; then 
	systemID=$(echo "$getManagedAccounts" |awk -F, 'NR>1{print $1}' RS='SystemId":')
	accountID=$(echo "$getManagedAccounts" |awk -F, 'NR>1{print $1}' RS='"AccountId":')
	fi
	requests
	if [ "$requestsResponse" -eq 201 ]; then
	requestID=$(echo "$requests"|tail -n1| awk '{print $1}' | cut -f1 -d'"')
	echo "${green}Request ID :: $requestID${NORMAL}"
	elif [ "$requestsResponse" -eq 403 ]; then
		echo "${red}User does not have permissions to request the indicated account or the account does not have API access enabled${NORMAL}"
		signout
		exit
	else
		echo "${red}Conflicting request exists. This user or another user has already requested a password for the specified account within the next <durationMinutes> window${NORMAL}"
		signout
		exit
	fi

        ppkFile=$(getProp 'ppk.out')
        pemFile=$(getProp 'pem.out')

	getCredentials
	if [ "$getCredentialsResponse" -eq 200 ]; then
	accountCredentials=$(echo "$getCredentials"|tail -n1|tr -d '\"')
	    if [ $dssAuth -eq 1 ]; then
               if test -f whiteboard.key; then
                   existWhite=`cat whiteboard.key`
                   echo -e $accountCredentials > tmp.key
                   keystr=`cat tmp.key`
                   if [ "$existWhite" == "$keystr" ]; then
                       echo "${red}No key change, exit${NORMAL}"
                       checkinCredentials
                       signout
                       exit
                   fi
               fi
	       echo -e $accountCredentials > whiteboard.key
               if test -f private_key.ppk; then
                   cp private_key.ppk out.ppk
               fi
               if test -f private_key.pem; then
                   cp private_key.pem out.pem
               fi
	       echo "Private Key(Non-OpenSSH format) for managed account $accountName on server $systemName is :"
		   echo "${green} $(cat whiteboard.key)${NORMAL}"
		   echo "Converting SSH Key to OpenSSH format"
		   #chmod 600 whiteboard.key
		   puttygen whiteboard.key -O private -o private_key.ppk --old-passphrase pass.txt
		   puttygen whiteboard.key -O private-openssh -o private_key.pem -P -C nopass --old-passphrase pass.txt --new-passphrase empty
		   echo "Converted to PPK format"
		   echo "${green} $(cat private_key.ppk)${NORMAL}"
                   if [ -z "$pemFile" ]
                   then
                      echo "No PEM format in config file, skip it"
                   else
                      if test -f out.pem; then
                          cp out.pem "$pemFile"
                      else 
                          cp private_key.pem "$pemFile"
                      fi
                      echo "Copied key to $pemFile"
                   fi
                   if [ -z "$ppkFile" ]
                   then
                      echo "No PPK format in config file, skip it"
                   else
                      if test -f out.ppk; then
                          cp out.ppk "$ppkFile"
                      else
                          cp private_key.ppk "$ppkFile"
                      fi
                      echo "Copied key to $ppkFile"
                   fi

		else
			echo "Password for managed account $accountName on $systemName :${green}$accountCredentials${NORMAL}"
	    fi
	elif [ "$getCredentialsResponse" -eq 403 ]; then
		echo "${red}User does not have permissions to request credentials${NORMAL}"
	else echo "${red}Error retrieving credentials, check if the account is configured for Password/DSS key based authentication${NORMAL}"
	signout
	exit
	fi
	checkinCredentials
	if [ "$checkinCredentialsResponse" -eq 204 ]; then echo "${green}Credential check-in successful${NORMAL}"
	else 
		echo "${red}User does not have permissions to release the indicated request or the associated account${NORMAL}"
	fi
	signout
else echo "${red}Failed to Connect, Error Code: $signinstat ${NORMAL}"
fi
