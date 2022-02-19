package com.unistudents.api.parser;

import com.unistudents.api.common.Integration;
import com.unistudents.api.common.Services;
import com.unistudents.api.model.Student;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public abstract class Parser {
    String PRE_LOG;
    Exception exception;
    String document;

    public abstract List<Integration> getIntegrations();
    public abstract Student parseStudent(Map<String, Object> scrapedData, Integration integration);

    public String getLogFile() {
        return new Services().uploadLogFile(exception, document, PRE_LOG);
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    void init(Integration integration) {
        PRE_LOG = integration.getUniversity() + (integration.getSystem() == null ? "" : "." + integration.getSystem());
    }
}
