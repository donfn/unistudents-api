package com.unistudents.api.scraper;

import com.unistudents.api.common.Integration;
import com.unistudents.api.common.Services;
import com.unistudents.api.common.StringHelper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CardisoftScraper extends Scraper {
    public enum Docs {
        INFO_PAGE,
        GRADES_PAGE
    }

    private final Logger logger = LoggerFactory.getLogger(CardisoftScraper.class);

    @Override
    public List<Integration> getIntegrations() {
        return Arrays.asList(
            Integration.IHU_TEITHE,
            Integration.IHU_CM,
            Integration.IHU_TEIEMT,
            Integration.UOP_MAIN,
            Integration.UOP_TEIPEL,
            Integration.UNIPI,
            Integration.UOWM,
            Integration.ASPETE
        );
    }

    @Override
    Map<String, Object> getScrapedData(String username, String password) {
        Map<String, Object> scrapedData = new HashMap<>();
        username = username.trim();
        password = password.trim();

        //
        // Request Login Html Page
        //

        Connection.Response response = null;
        String loginPage = "";
        String[] keyValue;

        try {
            response = getResponse(USER_AGENT);

            // check for connection errors
            if (response == null) return null;

            loginPage = String.valueOf(response.parse());
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }

        // get hashed key, value. if exists
        keyValue = getKeyValue(loginPage);

        // store data to pass
        HashMap<String, String> data = new HashMap<>();
        data.put((SYSTEM != null) ? (SYSTEM.equals("CM") ? "userName1" : "userName") : "userName", username);
        data.put("pwd", password);
        data.put("submit1", "%C5%DF%F3%EF%E4%EF%F2");
        data.put("loginTrue", "login");
        if (keyValue != null)
            if (keyValue.length == 2)
                if (!keyValue[0].isEmpty() && !keyValue[1].isEmpty())
                    data.put(keyValue[0], keyValue[1]);

        // store session cookies
        Map<String, String> cookies = new HashMap<>();
        for (Map.Entry<String, String> entry : response.cookies().entrySet()) {
            if (entry.getKey().startsWith("ASPSESSIONID") || entry.getKey().startsWith("HASH_ASPSESSIONID")) {
                cookies.put(entry.getKey(), entry.getValue());
            }
        }

        //
        // Try to Login
        //

        try {
            response = Jsoup.connect(URL + "/login.asp")
                    .data(data)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Cache-Control", "max-age=0")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Origin", URL)
                    .header("Referer", URL + "/login.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(response.cookies())
                    .method(Connection.Method.POST)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return null;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return null;
        }

        // returned document from login response
        Document returnedDoc = null;
        boolean authorized = false;
        try {
            returnedDoc = response.parse();
            authorized = authorizationCheck(returnedDoc);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }

        //
        // if is not authorized return
        //
        if (!authorized) {
            this.authorized = false;
            return null;
        }
        else {
            this.authorized = true;
        }

        // if server is not responding
        if (returnedDoc.toString().contains("An operation error occurred. (AuthPublisherObject)") ||
                returnedDoc.toString().contains("The LDAP server is unavailable. (AuthPublisherObject)") ||
                returnedDoc.toString().contains("Συνέβη σφάλμα. H ενέργεια αυτή προκάλεσε σφάλμα συστήματος. Παρακαλούμε προσπαθήστε αργότερα.")) {
            connected = false;
            return null;
        }

        if (!response.url().toString().contains("studentMain.asp")) {
            logger.error("[" + PRE_LOG + "] Error: Invalid response URL {}", response.url().toString());
            return null;
        }

        // set student info page
        scrapedData.put(Docs.INFO_PAGE.toString(), returnedDoc);
        // StringHelper.write("src/test/resources/cardisoft/info.html", returnedDoc.outerHtml());

        // add cookies
        for (Map.Entry<String, String> entry : response.cookies().entrySet()) {
            cookies.put(entry.getKey(), entry.getValue());
        }

        //
        // Request Grades Page
        //

        try {
            response = Jsoup.connect(URL + "/stud_CResults.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Referer", URL + "/studentMain.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(cookies)
                    .method(Connection.Method.GET)
                    .execute();
            returnedDoc = response.parse();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return null;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return null;
        }

        // set grades page
        scrapedData.put(Docs.GRADES_PAGE.toString(), returnedDoc);
        // StringHelper.write("src/test/resources/cardisoft/grades.html", returnedDoc.outerHtml());
        setSession(cookies);
        return scrapedData;
    }

    @Override
    Map<String, Object> getScrapedData(Map<String, String> session) {
        Map<String, Object> scrapedData = new HashMap<>();
        Connection.Response response;

        //
        // Request Info Page
        //

        try {
            response = Jsoup.connect(URL + "/studentMain.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(session)
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return null;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return null;
        }

        // set info page
        try {
            if (response.statusCode() != 200) return null;
            Document infoPage = response.parse();
            scrapedData.put(Docs.INFO_PAGE.toString(), infoPage);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }

        //
        // Request Grades Page
        //

        try {
            response = Jsoup.connect(URL + "/stud_CResults.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", DOMAIN)
                    .header("Referer", URL + "/studentMain.asp")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", USER_AGENT)
                    .cookies(session)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
            return null;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return null;
        }

        // set grades page
        try {
            scrapedData.put(Docs.GRADES_PAGE.toString(), response.parse());
            setSession(session);
            return scrapedData;
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
            return null;
        }
    }

    private Connection.Response getResponse(String userAgent) {
        try {
            return Jsoup.connect(URL + "/login.asp")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9,el;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Host", DOMAIN)
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("User-Agent", userAgent)
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException | UnknownHostException | HttpStatusException | ConnectException connException) {
            connected = false;
            logger.warn("[" + PRE_LOG + "] Warning: {}", connException.getMessage(), connException);
        } catch (IOException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
        return null;
    }

    private String[] getKeyValue(String loginPage) {
        if (SYSTEM != null) {
            if (SYSTEM.equals("TEITHE") || SYSTEM.equals("CM")) {
                return new String[0];
            }
        }

        if (loginPage.contains("eval([]")) {
            return getObfuscatedTypeTwo(loginPage);
        } else {
            return getObfuscatedTypeOne(loginPage);
        }
    }

    private String[] getObfuscatedTypeOne(String loginPage) {
        String[] keyValue = new String[2];

        try {
            int keyIndex = loginPage.indexOf("], '");
            int valueIndex = loginPage.lastIndexOf("], '");

            if (keyIndex != -1 && keyIndex != valueIndex) {
                int lastKeyIndex = getLastCharIndex(loginPage, keyIndex, ')');
                int lastValueIndex = getLastCharIndex(loginPage, valueIndex, ')');

                keyValue[0] = decode(loginPage.substring(keyIndex + 4, lastKeyIndex - 1) );
                keyValue[1] = decode(loginPage.substring(valueIndex + 4, lastValueIndex - 1) );
            } else if (keyIndex != -1) {
                keyIndex = loginPage.indexOf("'input[name=");

                int lastKeyIndex = getLastCharIndex(loginPage, keyIndex, ')');
                int lastValueIndex = getLastCharIndex(loginPage, valueIndex, ')');

                keyValue[0] = decode(loginPage.substring(keyIndex + 12, lastKeyIndex - 2) );
                keyValue[1] = decode(loginPage.substring(valueIndex + 4, lastValueIndex - 1) );
            } else {
                keyIndex = loginPage.indexOf("name=\"\\");
                valueIndex = loginPage.indexOf("value=\"\\");

                int lastKeyIndex = getLastCharIndex(loginPage, keyIndex, '"');
                int lastValueIndex = getLastCharIndex(loginPage, valueIndex, '"');

                keyValue[0] = decode(loginPage.substring(keyIndex + 6, lastKeyIndex) );
                keyValue[1] = decode(loginPage.substring(valueIndex + 7, lastValueIndex) );
            }
            return keyValue;
        } catch (Exception e) {
            logger.error("getObfuscatedTypeOne error", e);
            return null;
        }
    }

    private String[] getObfuscatedTypeTwo(String loginPage) {
        String obfuscatedString = loginPage.substring(loginPage.indexOf("eval(") + 5, loginPage.indexOf("());</script>") + 2);
        return Services.jsUnFuck(obfuscatedString);
    }

    private String decode(String hash) {
        hash = hash.replace("'", "").replace("+", "").replace("\\x", "").trim();
        byte[] decodedHash = new byte[0];
        try {
            decodedHash = Hex.decodeHex(hash.toCharArray());
        } catch (DecoderException e) {
            logger.error("[" + PRE_LOG + "] Error: {}", e.getMessage(), e);
        }
        return new String(decodedHash);
    }

    private int getLastCharIndex(String content, int index, char c) {
        int i = index + 40;
        while (true) {
            Character character = content.charAt(i);
            if (character.equals(c)) {
                return i;
            }
            i++;
        }
    }

    private boolean authorizationCheck(Document document) {

        String html = document.toString();

        return !(html.contains("Λάθος όνομα χρήστη ή κωδικού πρόσβασης") ||
                html.contains("Λάθος όνομα χρήστη") ||
                html.contains("Ο χρήστης δεν έχει πρόσβαση στην εφαρμογή"));
    }
}
