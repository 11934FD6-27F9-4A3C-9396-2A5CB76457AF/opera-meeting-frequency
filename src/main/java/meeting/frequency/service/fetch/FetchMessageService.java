package meeting.frequency.service.fetch;

import meeting.frequency.service.fetch.model.Message;

import java.net.URISyntaxException;
import java.util.List;

public interface FetchMessageService {

    List<Message> fetchMessages() throws URISyntaxException;
}
