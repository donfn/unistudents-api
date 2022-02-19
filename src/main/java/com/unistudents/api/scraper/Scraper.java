package com.unistudents.api.scraper;

import com.unistudents.api.common.Integration;
import com.unistudents.api.common.UserAgentGenerator;
import com.unistudents.api.model.LoginRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public abstract class Scraper {
    final String USER_AGENT = UserAgentGenerator.generate();
    String UNIVERSITY;
    String SYSTEM;
    String DOMAIN;
    String URL;
    String PRE_LOG;

    boolean connected = true;
    boolean authorized = true;
    Map<String, String> session;

    public abstract List<Integration> getIntegrations();
    abstract Map<String, Object> getScrapedData(String username, String password);
    abstract Map<String, Object> getScrapedData(Map<String, String> session);

    public Map<String, Object> getScrapedData(LoginRequest loginRequest, Integration integration) {
        init(integration);

        if (loginRequest.getSession() == null || loginRequest.getSession().isEmpty()) {
            return getScrapedData(loginRequest.getUsername(), loginRequest.getPassword());
        } else {
            Map<String, Object> scrapedData = getScrapedData(loginRequest.getSession());
            if (scrapedData == null || scrapedData.isEmpty()) {
                return getScrapedData(loginRequest.getUsername(), loginRequest.getPassword());
            } else {
                return scrapedData;
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public Map<String, String> getSession() {
        return session;
    }

    void setSession(Map<String, String> session) {
        this.session = session;
    }

    private void init(Integration integration) {
        UNIVERSITY = integration.getUniversity();
        SYSTEM = integration.getSystem();
        DOMAIN = integration.getDomain();
        URL = ((integration.isSSL()) ? "https://" : "http://") + DOMAIN + integration.getPathURL();
        PRE_LOG = UNIVERSITY + (SYSTEM == null ? "" : "." + SYSTEM);
    }
}
