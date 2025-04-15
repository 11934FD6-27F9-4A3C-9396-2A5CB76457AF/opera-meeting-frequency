package omegapoint.opera.operationaljournal.infrastructure.model.journal;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

public class WebhookStep {
    public final String value;

    public WebhookStep(final String webhookStep) {
        notNull(webhookStep);
        isTrue(webhookStep.length()<200, "length should be less than 200 characters");
        this.value = webhookStep;
    }
}
