package omegapoint.opera.logging;

import com.microsoft.azure.functions.HttpStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@EqualsAndHashCode
@Getter @Accessors(fluent = true)
public final class RejectMessage implements ResultMessage {

    private final String message;
    private final HttpStatus status;

    private RejectMessage(final String message,
                          final HttpStatus status) {
        this.message = message;
        this.status = status;
    }

    @Deprecated
    public static RejectMessage of(final String value) {
        return of(value, null);
    }
    public static RejectMessage of(final String value,
                                   final HttpStatus status) {
        return new RejectMessage(value, status);
    }
    public static RejectMessage of400(final String value) {
        return of(value, HttpStatus.BAD_REQUEST);
    }
    public static RejectMessage of404(final String value) {
        return of(value, HttpStatus.NOT_FOUND);
    }
    public static RejectMessage of409(final String value) {
        return of(value, HttpStatus.CONFLICT);
    }
    public static RejectMessage of500(final String value) {
        return of(value, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public HttpStatus status() {
        return this.status;
    }
}
