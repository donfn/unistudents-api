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

public class ICARUSParser {
    private Exception exception;
    private String document;
    private final Logger logger = LoggerFactory.getLogger(ICARUSParser.class);

    public Student parseInfoAndGradesPages(Document infoAndGradePage) {
        DecimalFormat df2 = new DecimalFormat("#.##");
        Student student = new Student();
        Progress progress = initGrades();
        Info info = new Info();

        // get some information
        try {
            String[] fullName = infoAndGradePage.select("#header_login u").text().split(" ");
            info.setFirstName(fullName[0]);
            info.setLastName(fullName[fullName.length - 1]);
            String[] moreInfo = infoAndGradePage.select("#wrapper #content #tabs-1 #stylized > h2").text().split(" ");
            info.setAem(moreInfo[2]);
            info.setRegistrationYear(moreInfo[moreInfo.length - 1]);
            info.setDepartmentTitle("Μηχανικών Πληροφοριακών και Επικοινωνιακών Συστημάτων");

            double totalSum = 0;
            int totalPassedCourses = 0;
            double[] semesterSum = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int[] semesterPassedCourses = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            ArrayList<Semester> semesters = initSemesters();
            Elements els = infoAndGradePage.select("#analytic_grades tbody tr");
            for (Element el : els) {
                Elements courseInfo = el.select("td");
                Course course = new Course();
                course.setId(courseInfo.get(1).text().trim());
                course.setName(courseInfo.get(2).text().trim());
//                course.setGrade(courseInfo.get(3).text().trim());
//                course.setExamPeriod(courseInfo.get(6).text().trim());
                String semesterId = courseInfo.get(4).text().trim();
                String status = courseInfo.get(7).text().trim();
                if (status.equals("Δε δόθηκε")) {
//                    course.setGrade("-");
//                    course.setExamPeriod("-");
                }

                boolean found = false;
                int semesterIndex = Integer.parseInt(semesterId) - 1;
                Semester semester = semesters.get(semesterIndex);
                for (int c = 0; c < semester.getCourses().size(); c++) {
                    Course semCourse = semester.getCourses().get(c);
                    if (semCourse.getId().equals(course.getId())) {
                        if (status.contains("Επιτυχία")) {
//                            if (semCourse.getGrade().equals("-")) {
//                                semester.getCourses().remove(semCourse);
//                                break;
//                            }
//                            if (Double.parseDouble(semCourse.getGrade()) < 5) {
//                                semester.getCourses().remove(semCourse);
//                                break;
//                            }
                        }
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    semester.getCourses().add(course);
                    if (status.equals("Επιτυχία")) {
//                        double grade = Double.parseDouble(course.getGrade());
//                        semesterSum[semesterIndex] += grade;
//                        semesterPassedCourses[semesterIndex]++;
//                        totalSum += grade;
                    }
                }
            }

            ArrayList<Semester> semestersToAdd = new ArrayList<>();
            for (int s = 0; s < semesters.size(); s++) {
                Semester semester = semesters.get(s);
                int passedCourses = semesterPassedCourses[s];
                totalPassedCourses += passedCourses;
                semester.setPassedCourses(passedCourses);
                semester.setDisplayAverageGrade((passedCourses == 0) ? "-" : df2.format(semesterSum[s] / passedCourses));
                if (semester.getCourses().size() > 0)
                    semestersToAdd.add(semester);
            }

            info.setCurrentSemester(String.valueOf(semestersToAdd.size()));
            progress.setSemesters(semestersToAdd);
            progress.setDisplayAverageGrade((totalPassedCourses == 0) ? "-" : df2.format(totalSum / totalPassedCourses));
            progress.setDisplayPassedCourses(String.valueOf(totalPassedCourses));

            student.setInfo(info);
            student.setProgress(progress);
            return student;
        } catch (Exception e) {
            logger.error("[AEGEAN.ICARUS] Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(infoAndGradePage.outerHtml());
            return null;
        }
    }

    private Progress initGrades() {
        Progress progress = new Progress();
        progress.setDisplayAverageGrade("-");
        progress.setDisplayEcts("-");
        progress.setDisplayPassedCourses("0");
        progress.setSemesters(new ArrayList<>());
        return progress;
    }

    private ArrayList<Semester> initSemesters() {
        Semester[] semesters = new Semester[12];
        for (int i = 1; i <= 12; i++) {
            semesters[i-1] = new Semester();
            semesters[i-1].setId(i);
            semesters[i-1].setPassedCourses(0);
            semesters[i-1].setDisplayAverageGrade("-");
            semesters[i-1].setCourses(new ArrayList<>());
        }
        return new ArrayList<>(Arrays.asList(semesters));
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
