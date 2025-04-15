package meeting.frequency.secret;

import meeting.frequency.secret.model.OpenAPISecrets;
import meeting.frequency.secret.model.SlackSecrets;

public interface SecretService {

    SlackSecrets fetchSlackSecrets();
    OpenAPISecrets fetchOpenAPISecrets();
}
