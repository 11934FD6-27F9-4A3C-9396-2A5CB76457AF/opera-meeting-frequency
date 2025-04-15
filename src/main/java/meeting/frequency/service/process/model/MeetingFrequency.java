package meeting.frequency.service.process.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeetingFrequency(String name,
                               int meetings,
                               List<String> companies,
                               String office) {

}