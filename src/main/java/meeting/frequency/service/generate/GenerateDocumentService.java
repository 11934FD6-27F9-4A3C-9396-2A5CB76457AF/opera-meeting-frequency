package meeting.frequency.service.generate;

import meeting.frequency.service.process.model.MeetingFrequency;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface GenerateDocumentService {


    File generateDocument(final List<MeetingFrequency> meetingFrequencyList) throws IOException, URISyntaxException;
}
