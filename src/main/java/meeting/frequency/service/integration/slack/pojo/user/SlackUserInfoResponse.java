package meeting.frequency.service.integration.slack.pojo.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackUserInfoResponse(boolean ok, User user, String error){ ;

}