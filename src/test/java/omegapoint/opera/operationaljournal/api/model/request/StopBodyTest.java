package omegapoint.opera.operationaljournal.api.model.request;

import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StopBodyTest {

    @Test
    void deserialize_good() {
        final String body = """
                {
                  "attempt": 7,
                  "timestamp": "2024-05-30T00:01:29.217036900Z[Etc/UTC]",
                  "message": "No Cinode user found for this Sage employee",
                  "status": "ERROR"
                }
                """;
        StopBody stopBody = StopBody.deserialize(body).get();

        assertEquals(7, stopBody.attempt);
        assertEquals("2024-05-30T00:01:29.217036900Z[Etc/UTC]" , stopBody.timestamp.toString());
        assertEquals("No Cinode user found for this Sage employee", stopBody.message);
        assertEquals("ERROR", stopBody.status);
    }

    @Test
    void deserialize_missing_field() {
        final String body = """
                {
                  "attempt": 7,
                  "message": "No Cinode user found for this Sage employee",
                  "status": "ERROR"
                }
                """;
        Either<RejectMessage, StopBody> stopBody = StopBody.deserialize(body);

        assertTrue(stopBody.isLeft());
        assertTrue(stopBody.getLeft().message().contains("Some field is null"));
    }

}