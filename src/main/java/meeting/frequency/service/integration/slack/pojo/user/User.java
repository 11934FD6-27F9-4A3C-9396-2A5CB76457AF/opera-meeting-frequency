package meeting.frequency.service.integration.slack.pojo.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(String id, @JsonProperty("real_name") String realName){ ;

	public static User EMPTY_USER(){
		return new User("", "");
	};
}