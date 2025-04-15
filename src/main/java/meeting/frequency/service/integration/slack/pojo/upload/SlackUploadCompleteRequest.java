package meeting.frequency.service.integration.slack.pojo.upload;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SlackUploadCompleteRequest(List<UploadFile> files, @JsonProperty("channel_id")String channel_id) {}
