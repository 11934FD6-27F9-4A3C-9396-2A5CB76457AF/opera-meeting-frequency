package omegapoint.opera.operationaljournal.api.model.request;

import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import org.junit.jupiter.api.Test;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RestartBodyTest {

    @Test
    void deserialize_good() {
        final var now = ZonedDateTime.now();
        RestartBody expected = new RestartBody(now.toString(), 2, null);
        Either<RejectMessage, RestartBody> actual = RestartBody.deserialize("""
                {
                    "timestamp": "%s",
                    "attempt": 2
                }
                """.formatted(now));

        assertTrue(actual
                .peekLeft(System.err::println)
                .isRight());
        assertEquals(expected, actual.get());
    }

    @Test
    void deserialize_missing_attempt() {
        final var now = ZonedDateTime.now();

        Either<RejectMessage, RestartBody> actual = RestartBody.deserialize("""
                {
                    "timestamp": "%s"
                }
                """.formatted(now));

        assertTrue(actual
                .peek(System.err::println)
                .isLeft());
    }

    @Test
    void deserialize_first_attempt() {
        final var now = ZonedDateTime.now();

        Either<RejectMessage, RestartBody> actual = RestartBody.deserialize("""
                {
                    "timestamp": "%s",
                    "attempt": 1
                }
                """.formatted(now));

        assertTrue(actual
                .peek(System.err::println)
                .isLeft());
    }

    @Test
    void deserialize_null_attempt() {
        final var now = ZonedDateTime.now();

        Either<RejectMessage, RestartBody> actual = RestartBody.deserialize("""
                {
                    "timestamp": "%s",
                    "attempt": null
                }
                """.formatted(now));

        assertTrue(actual
                .peek(System.err::println)
                .isLeft());
    }

    @Test
    void deserialize_negative_attempt() {
        final var now = ZonedDateTime.now();

        Either<RejectMessage, RestartBody> actual = RestartBody.deserialize("""
                {
                    "timestamp": "%s",
                    "attempt": -1
                }
                """.formatted(now));

        assertTrue(actual
                .peek(System.err::println)
                .isLeft());
    }
}