package com.unistudents.api.parser;

import com.unistudents.api.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class TEIWESTParser {
    private Exception exception;
    private String document;
    private final String PRE_LOG;
    private final Logger logger = LoggerFactory.getLogger(TEIWESTParser.class);

    public TEIWESTParser(String university) {
        this.PRE_LOG = "[" + university + ".TEIWEST]";
    }

    private Info parseInfoPage(Document infoPage) {
        Info info = new Info();

        try {

            String aem = infoPage.select("#ctl00_FooterPane_DepartmentUser").text();
            aem = aem.substring(aem.indexOf("AM:") + "AM:".length(), aem.indexOf("Τμήμα:")).trim();
            String firstName = infoPage.select("#ctl00_MainPane_Content_StudentInfoFormLayout_FirstNameLabel").text();
            String lastName = infoPage.select("#ctl00_MainPane_Content_StudentInfoFormLayout_LastNameLabel").text();
            String department = infoPage.select("#ctl00_MainPane_Content_StudentInfoFormLayout_DepartmentLabel").text();
            String registrationYear = infoPage.select("#ctl00_MainPane_Content_StudentInfoFormLayout_ProgramStudyLabel").text();
            String semester = infoPage.select("#ctl00_MainPane_Content_StudentInfoFormLayout_Semester").text();

            info.setAem(aem);
            info.setFirstName(firstName);
            info.setLastName(lastName);
            info.setDepartmentTitle(department);
            info.setRegistrationYear(registrationYear);
            info.setCurrentSemester(semester);

            return info;
        } catch (Exception e) {
            logger.error(this.PRE_LOG + " Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(infoPage.outerHtml());
            return null;
        }
    }

    private Progress parseGradesPage(Document gradePage) {
        Progress progress = initGrades();
        ArrayList<Semester> semesters = initSemesters();
        DecimalFormat df2 = new DecimalFormat("#.##");

        try {
            Element table = gradePage.select("#ctl00_MainPane_Content_DegreeGridView_DXMainTable").first();
            Elements rows = table.select("tr");

            double totalGradeSum = 0;
            int totalPassedCourses = 0;

            double semesterGradeSum = 0;
            int semesterPassedCourses = 0;
            Semester semester = null;
            for (Element row : rows) {
                if (row.hasClass("dxgvGroupRow_Metropolis")) {
                    String s = row.text();
                    String semesterId = s.substring(s.indexOf("ΕΞΑΜ: ") + "ΕΞΑΜ: ".length(), s.indexOf("(")).trim();
                    semester = semesters.get(parseSemesterId(semesterId) - 1);
                    semesterGradeSum = 0;
                    semesterPassedCourses = 0;
                } else if (row.hasClass("dxgvDataRow_Metropolis")) {
                    Elements els = row.select("td");
                    Course course = new Course();
                    course.setName(els.get(3).text());
                    course.setType(els.get(5).text());
//                    course.setExamPeriod(els.get(1).text());
                    course.setId(course.getName().replace(" ", "").trim() +
                            course.getType().replace(" ", "").trim());

                    String grade = els.get(6).text().replace(",", ".").replace(".00", "");
//                    course.setGrade(grade);

                    double gradeToCompute = Double.parseDouble(grade);
                    if (gradeToCompute >= 5) {
                        semesterGradeSum += gradeToCompute;
                        semesterPassedCourses++;

                        totalGradeSum += gradeToCompute;
                        totalPassedCourses++;
                    }

                    if (semester == null) return null;
                    semester.setPassedCourses(semesterPassedCourses);
                    semester.getCourses().add(course);

                    if (semesterPassedCourses > 0) {
                        double averageGrade = (double) Math.round((semesterGradeSum / semesterPassedCourses) * 100) / 100;
                        semester.setDisplayAverageGrade(df2.format(averageGrade));
                    } else {
                        semester.setDisplayAverageGrade("-");
                    }
                }
            }


            clearSemesters(semesters);
            progress.setSemesters(semesters);
            progress.setDisplayPassedCourses(String.valueOf(totalPassedCourses));
            if (totalPassedCourses > 0) {
                double averageGrade = (double) Math.round((totalGradeSum / totalPassedCourses) * 100) / 100;
                progress.setDisplayAverageGrade(df2.format(averageGrade));
            } else {
                progress.setDisplayAverageGrade("-");
            }
        } catch (Exception e) {
            logger.error(this.PRE_LOG + " Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(gradePage.outerHtml());
            return null;
        }

        return progress;
    }

    private ArrayList<Semester> clearSemesters(ArrayList<Semester> semesters) {
        Iterator<Semester> iterator = semesters.iterator();
        while (iterator.hasNext()) {
            Semester semester = (Semester) iterator.next();
            if (semester.getCourses().isEmpty()) {
                iterator.remove();
            }
        }

        return semesters;
    }

    private ArrayList<Semester> initSemesters() {
        Semester[] semesters = new Semester[12];
        for (int i = 1; i <= 12; i++) {
            semesters[i - 1] = new Semester();
            semesters[i - 1].setId(i);
            semesters[i - 1].setPassedCourses(0);
            semesters[i - 1].setDisplayAverageGrade("-");
            semesters[i - 1].setDisplayEcts("-");
            semesters[i - 1].setCourses(new ArrayList<>());
        }
        return new ArrayList<>(Arrays.asList(semesters));
    }

    private Progress initGrades() {
        Progress progress = new Progress();
        progress.setDisplayAverageGrade("-");
        progress.setDisplayEcts("-");
        progress.setDisplayPassedCourses("0");
        progress.setSemesters(new ArrayList<>());
        return progress;
    }

    public Student parseInfoAndGradesDocuments(Document infoPage, Document gradesPage) {
        Student student = new Student();

        try {
            Info info = parseInfoPage(infoPage);
            Progress progress = parseGradesPage(gradesPage);

            if (info == null || progress == null) {
                return null;
            }

            student.setInfo(info);
            student.setProgress(progress);

            return student;
        } catch (Exception e) {
            logger.error(this.PRE_LOG + " Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(infoPage.outerHtml() + "\n\n\n======\n\n\n" + gradesPage.outerHtml());
            return null;
        }
    }

    private int parseSemesterId(String semesterString) {
        switch (semesterString) {
            case "Α":
            case "A":
                return 1;
            case "Β":
            case "B":
                return 2;
            case "Γ":
                return 3;
            case "Δ":
                return 4;
            case "Ε":
            case "E":
                return 5;
            case "Ζ":
                return 6;
            case "ΣΤ":
                return 7;
            case "Η":
                return 8;
            case "Θ":
                return 9;
            case "Ι":
            case "I":
                return 10;
            case "Κ":
            case "K":
                return 11;
            case "Λ":
                return 12;
            default:
                try {
                    return Integer.parseInt(semesterString);
                } catch (NumberFormatException e) {
                    return 1;
                }
        }
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}
