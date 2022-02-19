package com.unistudents.api.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.unistudents.api.model.LoginRequest;
import com.unistudents.api.service.MockService;
import com.unistudents.api.service.StudentService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@EnableCaching
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private MockService mockService;

    @RequestMapping(value = {"/image"}, produces = MediaType.IMAGE_JPEG_VALUE, method = RequestMethod.POST)
    public @ResponseBody byte[] getImage(
            @RequestParam("url") String url,
            @RequestBody JsonNode jsonNode) {

        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .cookie("ASP.NET_SessionId", jsonNode.get("cookie").asText())
                    .followRedirects(false)
                    .execute();

            return response.bodyAsBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = {"/student/{university}", "/student/{university}/{system}"}, method = RequestMethod.POST)
    public ResponseEntity getStudent(
            @PathVariable("university") String university,
            @PathVariable(required = false) String system,
            @RequestBody LoginRequest loginRequest) {
        return studentService.getStudent(loginRequest, university.toUpperCase(), system != null ? system.toUpperCase() : null);
    }

    @RequestMapping(value = {"/mock/student/{university}", "/mock/student/{university}/{system}"}, method = RequestMethod.POST)
    public ResponseEntity getStudentMock(
            @PathVariable(required = false) String university,
            @PathVariable(required = false) String system) {
        return mockService.getStudent(university != null ? university.toUpperCase() : null, system != null ? system.toUpperCase() : null);
    }
}
