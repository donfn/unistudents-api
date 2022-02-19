package com.unistudents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Progress {
    private int passedCourses;
    private String displayPassedCourses;
    private int failedCourses;
    private String displayFailedCourses;
    private double averageGrade;
    private String displayAverageGrade;
    private double weightedAverageGrade;
    private String displayWeightedAverageGrade;
    private int ects;
    private String displayEcts;
    private int credits;
    private String displayCredits;
    private ArrayList<Semester> semesters;
}
