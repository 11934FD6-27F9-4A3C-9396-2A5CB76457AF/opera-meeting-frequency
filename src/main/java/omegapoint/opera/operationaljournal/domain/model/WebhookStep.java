package omegapoint.opera.operationaljournal.domain.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class WebhookStep {

    public final String value;

    public WebhookStep(final String value) {
        this.value = value;
    }
}
