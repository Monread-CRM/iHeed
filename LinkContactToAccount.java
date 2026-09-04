void automation.Link_Contact_to_Account(Int contactId)
{
notificationEmail = "darragh.mckernan@monread.com";
contact = zoho.crm.getRecordById("Contacts",contactId);
if(contact == null || contact.get("id") == null)
{
	info "The function could not retrieve the Contact ID";
	sendmail
	[
		from :zoho.loginuserid
		to :notificationEmail
		subject :"Link Contact to Account"
		message :"The function could not retrieve the Contact ID: " + contactId
	]
	return;
}
clinicName = ifnull(contact.get("Clinc"),"").toString().trim();
if(clinicName == "")
{
	info "Contact ID has no value in the Clinic field";
	sendmail
	[
		from :zoho.loginuserid
		to :notificationEmail
		subject :"Link Contact to Account - Not Found"
		message :"Contact ID" + contactId + " has no value in the Clinic field"
	]
	return;
}
info "Contact ID: " + contactId;
info "Clinic Name: " + clinicName;
accountSearchResults = zoho.crm.searchRecords("Accounts","(Account_Name:equals:" + clinicName + ")",1,200);
info accountSearchResults;
exactMatches = List();
for each  candidateAccount in accountSearchResults
{
	candidateName = ifnull(candidateAccount.get("Account_Name"),"").toString().trim();
	if(candidateName.equalsIgnoreCase(clinicName))
	{
		exactMatches.add(candidateAccount);
	}
}
matchCount = exactMatches.size();
info "Total Matches: " + matchCount;
if(matchCount == 0)
{
	//possible matches comparison
	cleanClinicName = clinicName.toLowerCase().replaceAll("[^a-z0-9 ]"," ",false).trim();
	clinicWords = cleanClinicName.toList(" ");
	clinicWords = clinicWords.sort(true);
	clinicWordKey = "";
	for each  clinicWord in clinicWords
	{
		if(clinicWord != "")
		{
			clinicWordKey = clinicWordKey + clinicWord + " ";
		}
	}
	possibleMatches = List();
	for each  pageNumber in {1,2,3,4,5,6,7,8,9,10}
	{
		accountsPage = zoho.crm.getRecords("Accounts",pageNumber,200);
		for each  candidateAccount in accountsPage
		{
			candidateName = ifnull(candidateAccount.get("Account_Name"),"").toString().trim();
			cleanCandidateName = candidateName.toLowerCase().replaceAll("[^a-z0-9 ]"," ",false).trim();
			candidateWords = cleanCandidateName.toList(" ");
			candidateWords = candidateWords.sort(true);
			candidateWordKey = "";
			for each  candidateWord in candidateWords
			{
				if(candidateWord != "")
				{
					candidateWordKey = candidateWordKey + candidateWord + " ";
				}
			}
			if(candidateWordKey == clinicWordKey)
			{
				possibleMatches.add(candidateAccount);
			}
		}
		if(accountsPage.size() < 200)
		{
			break;
		}
	}
	info "Clinic word key: " + clinicWordKey;
	info "Possible word-order matches: " + possibleMatches.size();
	for each  possibleAccount in possibleMatches
	{
		info "Possible Account: " + possibleAccount.get("Account_Name") + " | ID: " + possibleAccount.get("id");
	}
	if(possibleMatches.size() > 0)
	{
		possibleMatchList = "";
		for each  possibleAccount in possibleMatches
		{
			possibleMatchList = possibleMatchList + "Account Name: " + possibleAccount.get("Account_Name") + " | Account ID: " + possibleAccount.get("id") + " <br> ";
		}
		sendmail
		[
			from :zoho.loginuserid
			to :notificationEmail
			subject :"Link Contact to Account - Multiple Possible Matches"
			message :"Contact ID " + contactId + " has the Clinic '" + clinicName + "'. <br> No exact match was found but the listed accounts might potentially match: <br>  <br> " + possibleMatchList + " <br>  <br> Manual review is required. No Account was linked."
		]
		return;
	}
	sendmail
	[
		from :zoho.loginuserid
		to :notificationEmail
		subject :"Link Contact to Account - No matches found"
		message :"Contact ID " + contactId + " has Clinic '" + clinicName + "' but no exact or similar Account name match was found."
	]
	return;
}
if(matchCount > 1)
{
	matchList = "";
	info "Multiple Account Names match Clinic Name";
	for each  matchingAccount in exactMatches
	{
		matchList = matchList + "Account Name: " + matchingAccount.get("Account_Name") + " and Account ID: " + matchingAccount.get("id") + " <br> ";
	}
	sendmail
	[
		from :zoho.loginuserid
		to :notificationEmail
		subject :"Link Contact to Account - Multiple exact matches"
		message :"Contact ID " + contactId + " has Clinic '" + clinicName + "'. <br>  <br> Multiple account names match exactly: " + matchList + " - needs manual review."
	]
	return;
}
if(matchCount == 1)
{
	selectedAccount = exactMatches.get(0);
	selectedAccountId = selectedAccount.get("id");
	accountLookup = Map();
	accountLookup.put("id",toNumber(selectedAccountId));
	contactUpdate = Map();
	contactUpdate.put("Account_Name",accountLookup);
	updateResponse = zoho.crm.updateRecord("Contacts",contactId,contactUpdate);
	info updateResponse;
	for each  pageNumber in {1,2,3,4,5,6,7,8,9,10}
	{
		dealsPage = zoho.crm.searchRecords("Deals","(Contact_Name:equals:" + contactId + ")",pageNumber,200);
		for each  deal in dealsPage
		{
			dealUpdate = Map();
			dealUpdate.put("Account_Name",accountLookup);
			dealUpdateResponse = zoho.crm.updateRecord("Deals",toNumber(deal.get("id")),dealUpdate);
			info dealUpdateResponse;
		}
		if(dealsPage.size() < 200)
		{
			//we can only grab 200 records at a time so if its less that this we have found all we needed
			break;
		}
	}
	/*sendmail
	[
		from :zoho.loginuserid
		to :notificationEmail
		subject :"Link Contact to Account - Exact Match Found"
		message :"One exact match, setting the current account as a Contact in the Account"
	]*/
}
}
