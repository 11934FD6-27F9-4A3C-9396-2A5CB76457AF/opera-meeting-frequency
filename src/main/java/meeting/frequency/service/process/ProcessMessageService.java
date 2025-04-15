package meeting.frequency.service.process;

import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.process.model.MeetingFrequency;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface ProcessMessageService {

    List<MeetingFrequency> process(final List<Message> messages) throws IOException, URISyntaxException;
}
