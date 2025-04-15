package omegapoint.opera.operationaljournal.infrastructure.model.journal;

import static org.apache.commons.lang3.Validate.isTrue;

public class Message {
    public final String value;

    public Message(final String message) {
        if (message != null) {
            isTrue(message.length()<400, "length should be less than 400 characters");
        }
        this.value = message;
    }
}
