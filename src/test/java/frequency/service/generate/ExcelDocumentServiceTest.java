package frequency.service.generate;

import meeting.frequency.service.generate.ExcelClient;
import meeting.frequency.service.generate.ExcelDocumentService;
import meeting.frequency.service.process.model.MeetingFrequency;
import org.apache.commons.collections4.map.LinkedMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExcelDocumentServiceTest {

    private final ExcelClient excelClient = mock(ExcelClient.class);
    private final ExcelDocumentService service = new ExcelDocumentService(excelClient);
    
    @Test
    public void should_handle_success() throws IOException {
        List<MeetingFrequency> meetingFrequencyList = givenMeetingFrequency();

        service.generateDocument(meetingFrequencyList);


        verify(excelClient).generateDocument(eq(meetingFrequencyList), eq(expectedMapOfficeToMeetings()));
    }

    private Map<String, Integer> expectedMapOfficeToMeetings() {

        LinkedMap<String, Integer> map = new LinkedMap<>();
        map.put("Stockholm", 5);
        map.put("Oslo", 1);
        return map;
    }

    private List<MeetingFrequency> givenMeetingFrequency() {

        return List.of(
                new MeetingFrequency("Test1", 2, List.of(), "Stockholm"),
                new MeetingFrequency("Test2", 1, List.of(), "Oslo"),
                new MeetingFrequency("Test3", 3, List.of(), "Stockholm")
        );
    }


}