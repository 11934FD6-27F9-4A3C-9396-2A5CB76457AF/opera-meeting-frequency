package meeting.frequency.secret;

import meeting.frequency.secret.model.OpenAPISecrets;
import meeting.frequency.secret.model.SlackSecrets;

public class SecretServiceImpl implements SecretService{

    private final static String SLACK_READ_CHANNEL = "SlackChannelIdRead";
    private final static String SLACK_UPLOAD_CHANNEL = "SlackChannelIdUpload";
    private final static String SLACK_BOT_TOKEN = "SlackBotToken";
    private final static String OPENAI_API = "OpenAIKey";

    @Override
    public SlackSecrets fetchSlackSecrets() {

        return new SlackSecrets(
                System.getenv(SLACK_BOT_TOKEN),
                System.getenv(SLACK_READ_CHANNEL),
                System.getenv(SLACK_UPLOAD_CHANNEL)
        );
    }

    @Override
    public OpenAPISecrets fetchOpenAPISecrets() {

        return new OpenAPISecrets(System.getenv(OPENAI_API));
    }
}
