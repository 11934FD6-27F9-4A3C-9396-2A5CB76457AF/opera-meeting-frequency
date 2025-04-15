package meeting.frequency.service.integration.slack.pojo.upload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackUploadStartResponse(boolean ok, String upload_url, String file_id, String error) {

}
