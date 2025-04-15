package meeting.frequency.service.integration.slack.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BlocksItem(@JsonProperty("elements") List<ElementsItem> elements, @JsonProperty("type") String type,
						 @JsonProperty("block_id") String blockId) {

}