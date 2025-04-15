package meeting.frequency.service.upload;

import meeting.frequency.secret.SecretService;
import meeting.frequency.service.integration.slack.SlackHttpClient;

import java.io.File;

public class SlackUploadService implements UploadService{

    private final SlackHttpClient slackHttpClient;

    public SlackUploadService(final SecretService secretService) {

        this.slackHttpClient = new SlackHttpClient(secretService);
    }
    public SlackUploadService(final SlackHttpClient slackHttpClient) {this.slackHttpClient = slackHttpClient;}

    @Override
    public boolean upload(final File file) {

        return slackHttpClient.uploadFile(file);
    }
}
