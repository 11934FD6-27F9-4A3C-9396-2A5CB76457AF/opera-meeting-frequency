package omegapoint.opera.operationaljournal.config;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import lombok.extern.slf4j.Slf4j;
import omegapoint.opera.operationaljournal.infrastructure.SlackService;

import static org.apache.commons.lang3.Validate.notNull;

@Slf4j
public class SlackServiceConfig {

    public static SlackService slackService(){
        final String token = notNull(System.getenv("SlackAlertBotToken"), "SlackAlertBotToken is null");
        final String channelId = notNull(System.getenv("SlackAlertChannelId"), "SlackAlertChannelId is null");

        Slack slack = Slack.getInstance();
        MethodsClient methods = slack.methods(token);
        return new SlackService(methods, channelId);
    }
}
