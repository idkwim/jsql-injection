package com.jsql.util;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.spnego.SpnegoHttpURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.jsql.exception.PreparationException;

public class ConnectionUtil {
    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = Logger.getLogger(ConnectionUtil.class);
    
    /**
     * Url entered by user.
     */
    public static String initialUrl;
    
    /**
     * GET, POST, HEADER (State/Strategy pattern).
     */
    public static String method;
    
    /**
     * Get data submitted by user.
     */
    public static String getData = "";
    
    /**
     * Post data submitted by user.
     */
    public static String postData = "";
    
    /**
     * Header data submitted by user.
     */
    public static String headerData = "";
    
    public static String httpProtocol = "POST";
    
    public static void check() throws PreparationException {
     // Test the HTTP connection
        HttpURLConnection connection = null;
        try {
            LOGGER.info("Starting new injection");
            LOGGER.trace("Connection test...");

            if (AuthenticationUtil.enableKerberos) {
                String a = 
                        Pattern
                        .compile("(?s)\\{.*")
                        .matcher(StringUtils.join(Files.readAllLines(Paths.get(AuthenticationUtil.kerberosLoginConf), Charset.defaultCharset()), ""))
                        .replaceAll("")
                        .trim();
                
                SpnegoHttpURLConnection spnego = new SpnegoHttpURLConnection(a);
                connection = spnego.connect(new URL(ConnectionUtil.initialUrl));
            } else {
                connection = (HttpURLConnection) new URL(ConnectionUtil.initialUrl).openConnection();
            }
                
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDefaultUseCaches(false);
            HttpURLConnection.setFollowRedirects(ConfigurationUtil.followRedirection);
            
            // Add headers if exists (Authorization:Basic, etc)
            for (String header: ConnectionUtil.headerData.split("\\\\r\\\\n")) {
                Matcher regexSearch = Pattern.compile("(?s)(.*):(.*)").matcher(header);
                if (regexSearch.find()) {
                    String keyHeader = regexSearch.group(1).trim();
                    String valueHeader = regexSearch.group(2).trim();
                    try {
                        if ("Cookie".equalsIgnoreCase(keyHeader)) {
                            connection.addRequestProperty(keyHeader, valueHeader);
                        } else {
                            connection.addRequestProperty(keyHeader, URLDecoder.decode(valueHeader, "UTF-8"));
                        }
                    } catch (UnsupportedEncodingException e) {
                        LOGGER.warn("Unsupported header encoding " + e.getMessage(), e);
                    }
                }
            }

            StringUtil.sendMessageHeader(connection, ConnectionUtil.initialUrl);
            
            // Disable caching of authentication like Kerberos
            connection.disconnect();
        } catch (Exception e) {
            throw new PreparationException("Connection problem: " + e.getMessage());
        }
    }
    
}
