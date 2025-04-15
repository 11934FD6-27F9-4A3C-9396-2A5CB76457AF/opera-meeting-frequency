package meeting.frequency.service.integration.slack.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackHistoryResponse(boolean ok,
								  List<MessagesItem> messages,
								  boolean hasMore,
								  String error){

}