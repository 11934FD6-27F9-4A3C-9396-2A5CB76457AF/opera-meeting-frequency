package omegapoint.opera.logging;

import com.microsoft.azure.functions.HttpStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@EqualsAndHashCode
@Getter @Accessors(fluent = true)
public final class SuccessMessage implements ResultMessage {

    private final String message;

    private SuccessMessage(final String message) {
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.OK;
    }

    public static SuccessMessage of(final String value) {
        return new SuccessMessage(value);
    }
}
