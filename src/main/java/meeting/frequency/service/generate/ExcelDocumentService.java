package meeting.frequency.service.generate;

import meeting.frequency.service.process.model.MeetingFrequency;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelDocumentService implements GenerateDocumentService {

    private final ExcelClient excelClient;

    public ExcelDocumentService(final ExcelClient excelClient) {this.excelClient = excelClient;}

    public ExcelDocumentService() {
        this.excelClient = new ExcelClient();
    }

    @Override
    public File generateDocument(final List<MeetingFrequency> meetingFrequencyList) throws IOException {


        final Map<String, Integer> officeToTotalMeetings = getOfficeToTotalMeetings(meetingFrequencyList);

        return excelClient.generateDocument(meetingFrequencyList, officeToTotalMeetings);
    }

    private Map<String, Integer> getOfficeToTotalMeetings(final List<MeetingFrequency> meetingFrequencyList) {

        return meetingFrequencyList.stream()
                .collect(Collectors.groupingBy(MeetingFrequency::office))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()
                        .stream().mapToInt(MeetingFrequency::meetings).sum()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, //Ignore
                        LinkedHashMap::new
                ));
    }
}
