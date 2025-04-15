package meeting.frequency.service.integration.slack.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessagesItem(@JsonProperty("text") String text,
						   @JsonProperty("type") String type,
						   @JsonProperty("user") String user,
						   @JsonProperty("ts") String ts,
						   @JsonProperty("blocks") List<BlocksItem> blocks) {
}