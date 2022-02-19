package com.unistudents.api.parser;

import com.unistudents.api.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class PANTEIONParser {
    private Exception exception;
    private String document;
    private final Logger logger = LoggerFactory.getLogger(PANTEIONParser.class);

    public Student parseInfoAndGradesPages(Document[] pages) {
        // Student initialization
        Student student = new Student();
        Info info = new Info();
        DecimalFormat df2 = new DecimalFormat("#.##");

        try {
            // Getting info
            Elements table = pages[0].select(".infoTable");

            String aem = table.select("tr").get(0).select("#ctl00_ContentData_lblCode").text();
            String[] fullName = table.select("tr").get(0).select("#ctl00_ContentData_lblName").text().split(" ");
            String firstName = fullName[1];
            String lastName = fullName[0];
            String department = table.select("tr").get(1).select("#ctl00_ContentData_lblSection").text();
            String[] termInfo = table.select("tr").get(4).select("#ctl00_ContentData_lblTermInfo").text().split("\\*");
            String semester = termInfo[1].trim().replace("Τρέχον Εξάμηνο:", "");
            String registrationYear = termInfo[0].trim().replace("Έτος εισαγωγής:", "ΕΤΟΣ ΕΙΣΑΓΩΓΗΣ ");

            info.setAem(aem);
            info.setFirstName(firstName);
            info.setLastName(lastName);
            info.setDepartmentTitle(department);
            info.setCurrentSemester(semester);
            info.setRegistrationYear(registrationYear);

            // Setting info
            student.setInfo(info);

            Progress results = new Progress();
            Semester semesterObj;
            Course courseObj;

            int totalPassedCourses = 0;
            String totalPassedCoursesDOM = pages[0].select("#ctl00_ContentData_labelLessons_DLpassed").text();
            if (!totalPassedCoursesDOM.trim().equals("-")) {
                totalPassedCourses = Integer.parseInt(totalPassedCoursesDOM);
            }
            int totalPassedCoursesWithGrade = 0;
            double totalPassedCoursesSum = 0;
            int totalEcts = 0;
            String totalEctsDOM = pages[0].select("#ctl00_ContentData_labelUnits").text();
            if (!totalEctsDOM.trim().equals("-")) {
                totalEcts = Integer.parseInt(totalEctsDOM);
            }
            double totalAverageGrade = 0;
            for (int s = 1; s < 9; s++) {
                semesterObj = new Semester();
                semesterObj.setId(s);
                int passedCourses = 0;
                double passedCoursesSum = 0;
                int ects = 0;

                Elements gradesTable;
                for (int i = 0; i < pages.length; i++) {
                    if (pages[i] != null) {
                        // Getting grades table
                        gradesTable = pages[i].select("#ctl00_ContentData_grdStudLess");
                        for (Element element : gradesTable.select("tr")) {
                            if (element.hasClass("gvRowStyle") || element.hasClass("gvAlternatingRowStyle")) {
                                if (!element.toString().contains("Δεν υπάρχουν εγγραφές")) {
                                    int courseSemester = Integer.parseInt(element.select("td").get(4).text());
                                    if (courseSemester == s) {

                                        // collect course info
                                        String examPeriod = element.select("td").get(0).text();
                                        String id = element.select("td").get(2).text();
                                        String name = element.select("td").get(3).text();
                                        String courseType = element.select("td").get(5).text();
                                        int courseEcts = Integer.parseInt(element.select("td").get(6).text());
                                        String[] grade = element.select("td").get(7).text().replace(",", ".").split(" ");
                                        double gradeToCompute;
                                        if (grade.length == 2) {
                                            grade[0] = grade[1].split(":")[1];
                                            gradeToCompute = Double.parseDouble(grade[0].replace(",00", ""));
                                            examPeriod += " ΕΠΑΝ";
                                        } else {
                                            grade[0] = (!grade[0].trim().equals("")) ? grade[0].split(":")[1] : "-";
                                            gradeToCompute = (!grade[0].equals("-")) ? Double.parseDouble(grade[0].replace(",00", "")) : -1;
                                            examPeriod = (grade[0].equals("-")) ? "-" : examPeriod;
                                        }

                                        if (gradeToCompute > 10) {
                                            grade[0] = "";
                                        }

                                        courseObj = new Course();
                                        courseObj.setId(id);
                                        courseObj.setName(name);
                                        courseObj.setType(courseType);
//                                        courseObj.setGrade(grade[0]);
//                                        courseObj.setExamPeriod(examPeriod);
                                        semesterObj.getCourses().add(courseObj);

                                        // We exclude exception courses
                                        if (gradeToCompute >= 5 && gradeToCompute <= 10 && isNAE(name, courseEcts)) {
                                            // Calculate passed courses & ects for each semester
                                            passedCourses++;
                                            passedCoursesSum += gradeToCompute;
                                            ects += courseEcts;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                totalPassedCoursesSum += passedCoursesSum;
                totalPassedCoursesWithGrade += passedCourses;

                double gradeAverage = 0;
                if (passedCourses != 0) {
                    gradeAverage = passedCoursesSum / passedCourses;
                }

                semesterObj.setPassedCourses(passedCourses);
                semesterObj.setDisplayAverageGrade((passedCourses != 0) ? df2.format(gradeAverage) : "-");
                semesterObj.setDisplayEcts(String.valueOf(ects));
                if (semesterObj.getCourses().size() > 0)
                    results.getSemesters().add(semesterObj);
            }

            if (totalPassedCoursesWithGrade != 0)
                totalAverageGrade = totalPassedCoursesSum / totalPassedCoursesWithGrade;

            results.setDisplayPassedCourses(String.valueOf(totalPassedCourses));
            results.setDisplayAverageGrade((totalPassedCoursesWithGrade != 0) ? df2.format(totalAverageGrade) : "-");
            results.setDisplayEcts(String.valueOf(totalEcts));

            // Setting grades
            student.setProgress(results);
            return student;
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            setException(e);
            StringBuilder documents = new StringBuilder();
            for (Document page : pages) {
                if (page != null) {
                    documents.append(page.outerHtml());
                    documents.append("\n\n========\n");
                }
            }
            setDocument(documents.toString());
            return null;
        }
    }

    private boolean isNAE(String name, int ects) {
        return !(name.contains("ΦΡΟΝΤΙΣΤΗΡΙΟ") || ects == 0);
    }

    private void setDocument(String document) {
        this.document = document;
    }

    public String getDocument() {
        return this.document;
    }

    private void setException(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
