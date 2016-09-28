/**
 * paged.java
 * 5 July 2001
 * Sample JNDI application that performs a paged search.
 *
 */

import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;
import sun.nio.cs.UTF_32;
import sun.text.normalizer.UTF16;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;

import static java.lang.String.*;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.*;


class Paged {

    public static void main(String[] args) throws NamingException, IOException {
        ArrayList<String> list = new ArrayList<String>();
        FileInputStream in = null;
        try {
            in = new FileInputStream("WhilteList.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                list.add(line);
            }
        } catch (IOException x) {
            System.err.println(x);
        } finally {
            if (in != null) in.close();
        }

        System.out.println(list.size());
        ArrayList<Fishing> reportItem = new ArrayList<>();
        Fishing currentReportItem = null;
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "CN=,OU=Users,DC=contoso,DC=com");
        env.put(Context.SECURITY_CREDENTIALS, "password");
        env.put(Context.PROVIDER_URL, "ldap://office.contoso.com");
        env.put(Context.REFERRAL, "follow");

// We want to use a connection pool to
// improve connection reuse and performance.
        env.put("com.sun.jndi.ldap.connect.pool", "true");

// The maximum time in milliseconds we are going
// to wait for a pooled connection.
        env.put("com.sun.jndi.ldap.connect.timeout", "300000");

        //Next, we need to specify how we are going to search the directory with a SearchControls object, from where we want to start the search with a search base string, and how we want to filter results.

        SearchControls searchCtls = new SearchControls();
// We start our search from the search base.
// Be careful! The string needs to be escaped!
        String searchBase = "OU=Offices,DC=office,DC=lamoda,DC=ru";

// There are different search scopes:
// - OBJECT_SCOPE, to search the named object
// - ONELEVEL_SCOPE, to search in only one level of the tree
// - SUBTREE_SCOPE, to search the entire subtree
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

// We only want groups, so we filter the search for
// objectClass=group. We use wildcards to find all groups with the
// string "<group>" in its name. If we want to find users, we can
// use: "(&(objectClass=person)(cn=*<username>*))".
        String searchFilter = "(&(objectCategory=person)(mail=*)(objectClass=user))";

// We want all results.
        searchCtls.setCountLimit(0);

// We want to wait to get all results.
        searchCtls.setTimeLimit(0);

// Active Directory limits our results, so we need multiple
// requests to retrieve all results. The cookie is used to
// save the current position.
        byte[] cookie = null;
        LdapContext ctx = new InitialLdapContext(env, null);

// We want 500 results per request.
        ctx.setRequestControls(
                new Control[] {
                        new PagedResultsControl(1000, Control.CRITICAL)
                });

// We only want to retrieve the "distinguishedName" attribute.
// You can specify other attributes/properties if you want here.
        //String returnedAtts[] = { "distinguishedName", "mail" };
        //searchCtls.setReturningAttributes(returnedAtts);

// The request loop starts here.
        do {
            // Start the search with our configuration.
            NamingEnumeration<SearchResult> answer = ctx.search(
                    searchBase, searchFilter, searchCtls);

            // Loop through the search results.
            while (answer.hasMoreElements()) {
                long pwdSetDate;
                String pwdpwd;
                Date date = new Date();
                Date datecurrent = new Date();
                SearchResult sr = answer.next();
                Attributes attr = sr.getAttributes();
                //Attribute a = attr.get("distinguishedName");
                System.out.println(attr.get("name"));
                pwdpwd = String.valueOf(attr.get("pwdLastSet"));
                pwdpwd = pwdpwd.replaceAll("\\D+", "");
                System.out.println(pwdpwd);
                //System.out.println(attr.get("sAMAccountname"));
                //System.out.println(attr.get("pwdLastSet"));
                //pwdpwd = valueOf(attr.get("pwdLastSet"));
                pwdSetDate = Long.parseLong(pwdpwd);
                //pwdSetDate = Long.parseLong(String.valueOf(attr.get("pwdLastSet")));
                Date pwdSet = new Date(pwdSetDate/10000-11644473600000L);
                DateFormat mydate = new SimpleDateFormat("yyyyMMdd");
                mydate.format(date);

                System.out.println(mydate.format(date));
                long diff = datecurrent.getTime() - pwdSet.getTime();
                System.out.println("Difference between  " + date + " and "+ datecurrent+" is "
                        + (diff / (1000 * 60 * 60 * 24)) + " days.");
                System.out.println(attr.get("mail"));
                System.out.println(attr.get("userAccountControl"));
                // Print our wanted attribute value.
                //System.out.println((String) a.get());
                //System.out.println(sr.g("mail"));
                currentReportItem = new Fishing();
                currentReportItem.setName((String.valueOf(attr.get("name"))).substring(5));
                currentReportItem.setEmail((String.valueOf(attr.get("mail"))).substring(6));
                currentReportItem.setDaysAfterChangePwd(Math.toIntExact(diff / (1000 * 60 * 60 * 24)));
                currentReportItem.setUac(Integer.parseInt((String.valueOf(attr.get("userAccountControl"))).substring(20)));
                if (currentReportItem.getUac() == 512 && currentReportItem.getDaysAfterChangePwd() == 60 && !list.contains((String.valueOf(attr.get("mail"))).substring(6))) {
                    reportItem.add(currentReportItem);
                }

            }

            // Find the cookie in our response and save it.
            Control[] controls = ctx.getResponseControls();
            if (controls != null) {
                for (int i = 0; i < controls.length; i++) {
                    if (controls[i] instanceof
                            PagedResultsResponseControl) {
                        PagedResultsResponseControl prrc =
                                (PagedResultsResponseControl) controls[i];
                        cookie = prrc.getCookie();
                    }
                }
            }

            // Use the cookie to configure our new request
            // to start from the saved position in the cookie.
            ctx.setRequestControls(new Control[] {
                    new PagedResultsControl(1000,
                            cookie,
                            Control.CRITICAL) });
        } while (cookie != null);

        // We are done, so close the Context object.
        ctx.close();


        System.out.println(list.size());
        for (Fishing aBook : reportItem) {
            //System.out.println(aBook);
            System.out.println(aBook.getEmail());
            //Recipient's email ID needs to be mentioned.
            String to = "pfishingadmin@contoso.com";

            // Sender's email ID needs to be mentioned
            String from = "helpdesk@contoso.com <helpdesk@contoso92.com>";

            // Assuming you are sending email from localhost
            String host = "IP_SMTP_HOST";

            // Get system properties
            Properties properties = System.getProperties();

            // Setup mail server
            properties.setProperty("mail.smtp.host", host);

            // Get the default Session object.
            Session session = Session.getDefaultInstance(properties);

            try {
                // Create a default MimeMessage object.
                MimeMessage message = new MimeMessage(session);

                // Set From: header field of the header.
                message.setFrom(new InternetAddress(from));
                message.setHeader("Content-type", "text/html; charset=utf-8");

                // Set To: header field of the header.
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

                // Set Subject: header field
                message.setSubject("Your Network password will expire soon, please change your password.", "utf-8");

                // Now set the actual message
                String ruMessage = "Просьба произвести плановое изменение пароля для вашей учетной записи. В противном случае вы потеряете доступ к сети Lamoda. " +
                        "Пользователи других систем (не Windows) могут изменить пароль, зайдя по ссылке <a href=http://pwd.contoso92.com>https://pwd.contoso.com</a> " +
                        "Если вы не можете самостоятельно поменять пароль, обратитесь на hd@contoso.com.";
                String engMessage ="<br><br>Hello. Please, change your password for you account," +
                        " because its expired. Otherwise you will lose your access to the network & services of Lamoda company. Users of not Windows systems (like Mac OS, " +
                        "*nix, and other) go to url <a href=http://pwd.contoso92.com>https://pwd.contoso.com</a> If you are unable to change your password, please contact hd@contoso.com";
                //byte ptext[] = ruMessage.getBytes(UTF_16);
                //String formattedMessage = new String(ptext, UTF_8);
                //System.out.println(ruMessage);
                message.setContent(ruMessage+engMessage, "text/html; charset=utf-8");

                // Send message
                Transport.send(message);
                System.out.println("Sent message successfully....");
            }catch (MessagingException mex) {
                mex.printStackTrace();
            }





        }
        String msgToAdmin ="";
        for (Fishing aBook : reportItem) {
            msgToAdmin+=aBook.toString() + "\n<br>";
        }
        String to = "admin_1@contoso.com";

        // Sender's email ID needs to be mentioned
        String from = "Fishing";

        // Assuming you are sending email from localhost
        String host = "IP_SMTP";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));
            message.setHeader("Content-type", "text/html; charset=utf-8");
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("admin_2@contoso.com"));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject("Daily fishing list", "utf-8");

           
            message.setContent(msgToAdmin, "text/html; charset=utf-8");

            // Send message
            Transport.send(message);
            System.out.println("Sent message to admin successfully....");
        }catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

}
