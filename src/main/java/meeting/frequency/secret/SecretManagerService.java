package meeting.frequency.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import meeting.frequency.secret.model.OpenAPISecrets;
import meeting.frequency.secret.model.SlackSecrets;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SecretManagerService implements SecretService{

    private final static String SLACK_CREDENTIALS_PATH = "slack.credentials";
    private final static String OPENAI_CREDENTIALS_PATH = "openai.credentials";

    private final SecretsManagerClient client;
    private final ObjectMapper objectMapper;

    public SecretManagerService(){
        this.client = SecretsManagerClient.builder()
                .region(Region.EU_NORTH_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SlackSecrets fetchSlackSecrets(){

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(SLACK_CREDENTIALS_PATH)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);

            return objectMapper.readValue(response.secretString(), SlackSecrets.class);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not fetch slack secrets");
        }
    }

    @Override
    public OpenAPISecrets fetchOpenAPISecrets(){

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(OPENAI_CREDENTIALS_PATH)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);

            return objectMapper.readValue(response.secretString(), OpenAPISecrets.class);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not fetch openAI secrets");
        }
    }

}
