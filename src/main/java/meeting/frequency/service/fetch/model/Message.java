package meeting.frequency.service.fetch.model;

import java.util.List;

public record Message(String name,
                      String office,
                      List<String> rawMessages) {
}
