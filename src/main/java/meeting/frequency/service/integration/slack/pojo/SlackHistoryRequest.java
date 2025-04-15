package meeting.frequency.service.integration.slack.pojo;

public record SlackHistoryRequest(String channel, String oldest, String latest, int limit) {

}
