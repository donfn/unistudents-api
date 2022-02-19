package com.unistudents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Info {
    private String aem;
    private String firstName;
    private String lastName;
    private String username;
    private String departmentId;
    private String departmentTitle;
    private String specialtyId;
    private String specialtyTitle;
    private String registrationYear;
    private String programTitle;
    private String currentSemester;
    private String studentStatus;
    private Degree degree;
    private Map<String, String> extra;
}
