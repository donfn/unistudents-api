package com.unistudents.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Course {
    private String id; // hashed value of displayCode+name
    private String displayCode;
    private String name;
    private String type;
    private String stream;  // ομάδα μαθήματος
    private String instructor;
    private int ects;
    private String displayEcts;
    private int credits;  // Διδακτικές Μονάδες
    private String displayCredits;
    private double weight;  // βαρύτητα
    private boolean isExempted;  // μάθημα απαλλαγής
    private boolean isCalculated;  // προσμετράται στο πτυχίο
    private ExamGrade latestExamGrade;
    private List<ExamGrade> examGradeHistory;  // latestExamGrade excluded
    private List<Course> subCourses; // εργαστήριο, θεωρία κλπ
}
