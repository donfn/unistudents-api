package com.unistudents.api.parser;

import com.unistudents.api.model.Course;
import com.unistudents.api.model.Progress;
import com.unistudents.api.model.Semester;
import com.unistudents.api.model.Student;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class ECEParser {
    private Exception exception;
    private String document;
    private final Logger logger = LoggerFactory.getLogger(ECEParser.class);

    public Student parseGradeDocument(Document gradeDocument) {
        Student student = new Student();
        Progress progress = initGrades();
        ArrayList<Semester> semesters = initSemesters();

        try {
            Elements elSemesters = gradeDocument.select(".card-body .courses > h5");
            Elements elList = gradeDocument.select(".card-body .courses > .courses-list");

            for (int s = 0; s < elSemesters.size(); s++) {
                String semesterId = elSemesters.get(s).text().split(" ")[1];
                int semesterIndex = Integer.parseInt(semesterId);

                Elements elCourses = elList.get(s).select("table > tbody > tr");
                for (Element elCourse: elCourses) {
                    Elements elCourseFields = elCourse.select("td");
                    Course course = new Course();
                    course.setId(elCourseFields.get(0).text());
                    course.setName(elCourseFields.get(1).text());

                    String normalGrade = elCourseFields.get(2).text().trim();
                    if (!normalGrade.contains("–")) {
                        normalGrade = normalGrade.split(" ")[0];
                        if (normalGrade.contains("Προβιβάστηκε")) normalGrade = "P";
                        if (normalGrade.contains("Απέτυχε")) normalGrade = "F";
//                        course.setGrade(normalGrade);
//                        course.setExamPeriod("Κανονική " + elCourseFields.get(2).select("span").first().attributes().get("title").split(" ")[2].split("-")[0]);
                    }

                    String septGrade = elCourseFields.get(3).text().trim();
                    if (!septGrade.contains("–")) {
                        septGrade = septGrade.split(" ")[0];
                        if (septGrade.contains("Προβιβάστηκε")) septGrade = "P";
                        if (septGrade.contains("Απέτυχε")) septGrade = "F";
//                        course.setGrade(septGrade);
//                        course.setExamPeriod("Επαναληπτική " + elCourseFields.get(3).select("span").first().attributes().get("title").split(" ")[2].split("-")[0]);
                    }

                    String extraGrade = elCourseFields.get(4).text().trim();
                    if (!extraGrade.contains("–")) {
                        extraGrade = extraGrade.split(" ")[0];
                        if (extraGrade.contains("Προβιβάστηκε")) extraGrade = "P";
                        if (extraGrade.contains("Απέτυχε")) extraGrade = "F";
//                        course.setGrade(extraGrade);
//                        course.setExamPeriod("Επιπλέον " + elCourseFields.get(4).select("span").first().attributes().get("title").split(" ")[2].split("-")[0]);
                    }

//                    if (course.getGrade() == null)
//                        course.setGrade("-");

                    semesters.get(semesterIndex - 1).getCourses().add(course);
                }
            }

            ArrayList<Semester> semestersToAdd = new ArrayList<>();
            for (Semester semester: semesters) {
                if (semester.getCourses().size() > 0)
                    semestersToAdd.add(semester);
            }

            progress.setSemesters(semestersToAdd);
            student.setProgress(progress);
            return student;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[NTUA.ECE] Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(gradeDocument.outerHtml());
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
