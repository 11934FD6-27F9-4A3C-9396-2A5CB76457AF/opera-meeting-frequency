package meeting.frequency.service.integration.slack.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record ElementsItem(@JsonProperty("elements") List<ElementsItem> elements, @JsonProperty("type") String type,
						   @JsonProperty("text") String text) {

}