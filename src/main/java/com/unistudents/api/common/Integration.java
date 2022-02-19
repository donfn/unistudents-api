package com.unistudents.api.common;

import java.util.Objects;
import java.util.stream.Stream;

public enum Integration {
    AUEB("AUEB", null, "e-grammateia.aueb.gr", "/unistudent", true),
    IHU_TEITHE("IHU", "TEITHE", "pithia.teithe.gr", "/unistudent", false),
    IHU_CM("IHU", "CM", "egram.cm.ihu.gr", "/unistudent", true),
    IHU_TEIEMT("IHU", "TEIEMT", "e-secretariat.teiemt.gr", "/unistudent", true),
    UOP_MAIN("UOP", "MAIN", "e-secretary.uop.gr", "/UniStudent", true),
    UOP_TEIPEL("UOP", "TEIPEL", "www.webgram.teikal.gr", "/unistudent", false),
    UNIPI("UNIPI", null, "students.unipi.gr", "", true),
    UOWM("UOWM", null, "students.uowm.gr", "", true),
    ASPETE("ASPETE", null, "studentweb.aspete.gr", "", true),
    AUA_ILYDA("AUA", "ILYDA", "unistudent.aua.gr", null, true),
    UOI("UOI", null, "classweb.uoi.gr", null, true),
    UNIWA("UNIWA", null, "services.uniwa.gr", null, true),
    UOC("UOC", null, "eduportal.cict.uoc.gr", null, true),
    IONIO("IONIO", null, "dias.ionio.gr", null, true);

    private final String university;
    private final String system;
    private final String domain;
    private final String pathURL;
    private final boolean SSL;

    Integration(String university, String system, String domain, String pathURL, boolean SSL) {
        this.university = university;
        this.system = system;
        this.domain = domain;
        this.pathURL = pathURL;
        this.SSL = SSL;
    }

    public String getUniversity() {
        return university;
    }

    public String getSystem() {
        return system;
    }

    public String getDomain() {
        return domain;
    }

    public String getPathURL() {
        return pathURL;
    }

    public boolean isSSL() {
        return SSL;
    }

    public static Integration getIntegration(String university, String system) {
        return Stream.of(Integration.values())
                .filter(i -> Objects.equals(i.getUniversity(), university) && Objects.equals(i.getSystem(), system))
                .findFirst()
                .orElse(null);
    }
}
