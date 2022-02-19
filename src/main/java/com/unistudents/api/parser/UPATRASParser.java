package com.unistudents.api.parser;

import com.unistudents.api.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class UPATRASParser {
    private Exception exception;
    private String document;
    private final Logger logger = LoggerFactory.getLogger(UPATRASParser.class);

    public Student parseInfoAndGradesPage(Document infoAndGradesPage) {
        Student student = new Student();
        DecimalFormat df2 = new DecimalFormat("#.##");

        try {
            /*
             *  Scrape aem, name, year,
             */
            Info info = new Info();
            info.setAem(infoAndGradesPage.select("span:containsOwn(Αρ. Μητρώου)").first().parent().parent().parent().parent().children().last().text());

            String[] name = infoAndGradesPage.select("span:containsOwn(Ονοματεπώνυμο)").first().parent().parent().parent().parent().children().last().text().split(";")[0].split(",");
            info.setFirstName(name[0].trim());
            info.setLastName(name[1].trim());

            String[] year = infoAndGradesPage.select("span:containsOwn(Κατάσταση)").first().parent().parent().parent().parent().children().last().text().split(";");
            info.setRegistrationYear(year[year.length - 1].trim());


            /*
             *  Scrape some units
             */
            String ects = infoAndGradesPage.select("span:containsOwn(Σύνολο Ολοκλ. ΔΜ)").first().parent().parent().parent().children().last().child(0).attributes().get("value").replace(",00", "").replace("0000", "0");


            /*
             *  Scrape semester's courses
             */
            Progress progress = initGrades();
            progress.setDisplayEcts(ects);

            int totalPassedCourses = 0;
            int totalPassedCoursesWithoutGrades = 0;
            int[] semesterPassedCourses = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            double[] semesterGradesSum = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            Element table = infoAndGradesPage.select("tbody[id$=-contentTBody]").first();
            Elements rows = table.getElementsByAttribute("rr");
            for (Element row : rows) {
                Elements columns = row.getElementsByTag("td");
                if (columns.size() <= 1) continue;
                Course course = new Course();
                course.setId(columns.get(2).text());
                course.setName(columns.get(3).text());
                String grade = columns.get(4).text().replace(",", ".").replace("NS", "-");
//                course.setGrade(grade.equals("") ? "-" : grade);
                course.setType(columns.get(1).text());

                String col7 = columns.get(7).text();
                String gradeType = col7.trim().split(" ")[0];
                String examMonth = (col7.contains("(Επαναληπτικές)"))
                        ? "Επαναληπτικές"
                        : columns.get(6).text().trim().split(" ")[0];

                String examPeriod = examMonth + " " + columns.get(5).text() + " | " + (gradeType.equals("") ? "-" : gradeType);
//                course.setExamPeriod(examPeriod);

//                if (course.getGrade().equals("P")) {
//                    course.setGrade("");
//                    course.setExamPeriod("ΑΠΑΛΛΑΓΗ");
//                }

                // check for duplicates
                int courseSemester = Integer.parseInt(columns.get(0).text()) - 1;
                if (progress.getSemesters().get(courseSemester).getCourses().contains(course)) {
                    for (Course semCourse: progress.getSemesters().get(courseSemester).getCourses()) {
//                        if (semCourse.getId().equals(course.getId()) && semCourse.getGrade().equals("-") && !course.getGrade().equals("-")) {
//                            semCourse.setExamPeriod(course.getExamPeriod());
//                            semCourse.setGrade(course.getGrade());
//                            break;
//                        }
                    }
                    continue;
                }

                // add course to semester
                progress.getSemesters().get(courseSemester).getCourses().add(course);

                // calculate passed courses & avg grade
                if (columns.get(8).text().contains("Επιτυχία")) {
//                    if (course.getExamPeriod().equals("ΑΠΑΛΛΑΓΗ")) {
//                        totalPassedCoursesWithoutGrades++;
//                    } else {
//                        totalPassedCourses++;
//                        semesterPassedCourses[courseSemester]++;
//                        semesterGradesSum[courseSemester] += Double.parseDouble(course.getGrade());
//                    }
                }

                // set department
                if (info.getDepartmentTitle() == null) {
                    info.setDepartmentTitle(columns.get(22).text());
                }
            }

            // keep only semesters with courses
            ArrayList<Semester> semesters = new ArrayList<>();
            for (int index = 0; index < progress.getSemesters().size(); index++) {
                Semester semester = progress.getSemesters().get(index);
                if (!semester.getCourses().isEmpty()) {
                    int passedCourses = semesterPassedCourses[index];
                    double avg = semesterGradesSum[index];
                    semester.setPassedCourses(passedCourses);
                    semester.setDisplayAverageGrade((passedCourses == 0) ? "-" : df2.format(avg / passedCourses));
                    semesters.add(semester);
                }
            }
            progress.setSemesters(semesters);
            progress.setDisplayPassedCourses(String.valueOf(totalPassedCourses + totalPassedCoursesWithoutGrades));
            info.setCurrentSemester(String.valueOf(semesters.size()));

            /*
             * Scrape gpa
             */
            Elements lastTableRowData = infoAndGradesPage.select("tbody[id$=-contentTBody]").last().children().last().children();
            if (lastTableRowData.size() < 3) {
                progress.setDisplayAverageGrade("-");
            } else {
                progress.setDisplayAverageGrade(lastTableRowData.get(2).text().replace(",", "."));
            }

            student.setInfo(info);
            student.setProgress(progress);
            return student;
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
            setException(e);
            setDocument(infoAndGradesPage.outerHtml());
            return null;
        }
    }

    private Progress initGrades() {
        Progress progress = new Progress();

        ArrayList<Semester> semesters = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Semester semester = new Semester();
            semester.setId(i+1);
            semester.setDisplayAverageGrade("-");
            semester.setCourses(new ArrayList<>());
            semesters.add(semester);
        }

        progress.setSemesters(semesters);
        progress.setDisplayEcts("0");
        progress.setDisplayPassedCourses("0");
        progress.setDisplayAverageGrade("-");

        return progress;
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
