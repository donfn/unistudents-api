import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.Integration;
import com.unistudents.api.model.Student;
import com.unistudents.api.parser.CardisoftParser;
import com.unistudents.api.scraper.CardisoftScraper;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;
import util.RequestContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestCardisoftParser {

    private static final String INFO_HTML = "src/test/resources/cardisoft/info.html";
    private static final String GRADES_HTML = "src/test/resources/cardisoft/grades.html";
    private static final String STUDENT_JSON = "src/test/resources/cardisoft/student.json";

    @Test
    public void testParser() throws IOException {
        Integration integration = Integration.UNIPI;

        Document infoPage = RequestContent.readDocument(INFO_HTML);
        Document gradesPage = RequestContent.readDocument(GRADES_HTML);

        Map<String, Object> scrapedData = new HashMap<String, Object>(){{
            put(CardisoftScraper.Docs.INFO_PAGE.toString(), infoPage);
            put(CardisoftScraper.Docs.GRADES_PAGE.toString(), gradesPage);
        }};

        CardisoftParser parser = new CardisoftParser();
        Student student = parser.parseStudent(scrapedData, integration);

        ObjectMapper objectMapper = new ObjectMapper();
        String expectedStudent = RequestContent.readFile(STUDENT_JSON);

        Assert.assertEquals(
                expectedStudent,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(student));
    }
}
