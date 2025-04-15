package omegapoint.opera.logging;

import com.microsoft.azure.functions.HttpStatus;
import io.vavr.control.Either;

public sealed interface ResultMessage
        permits RejectMessage, SuccessMessage {

    String message();
    HttpStatus status();
    public static <L> Either<L, SuccessMessage> right(String message) {
        return Either.right(SuccessMessage.of(message));
    }
    public static <R> Either<RejectMessage, R> left(String message, HttpStatus httpStatus) {
        return Either.left(RejectMessage.of(message, httpStatus));
    }
}
