package meeting.frequency.service.integration.slack.pojo.upload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackUploadCompleteResponse(boolean ok) {

}
