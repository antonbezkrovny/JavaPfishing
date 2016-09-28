# Java Pfishing

+ Connecting to Active Directory
+ Grap all users from  
searchBase = "OU=Offices,DC=office,DC=contoso,DC=com";
+ With filter
searchFilter = "(&(objectCategory=person)(mail=*)(objectClass=user))";
+ Filter PwdLastSet to 30 days from today
+ Send fake mail to users
+ Send report to admin
