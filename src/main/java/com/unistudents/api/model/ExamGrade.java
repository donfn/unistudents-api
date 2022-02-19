package com.unistudents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExamGrade {
    double grade;
    boolean isPassed;
    String displayGrade;  // [1,0 - 10,0], [PASS - FAIL], [ΑΠΑΛΛΑΓΗ]
    String examPeriod;
    String academicYear;
    String displayPeriod;
}