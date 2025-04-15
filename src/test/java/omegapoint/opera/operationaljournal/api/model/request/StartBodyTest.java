package omegapoint.opera.operationaljournal.api.model.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import omegapoint.opera.operationaljournal.config.JacksonConfig;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartBodyTest {

    ZonedDateTime originTime;
    ZonedDateTime now;
    UUID parentRunId;
    UUID runId;
    StartBody example;

    @BeforeEach
    void setUp() {
        originTime = ZonedDateTime.now();
        now = ZonedDateTime.now();
        parentRunId = UUID.randomUUID();
        runId = UUID.randomUUID();
        example = new StartBody(
                UUID.randomUUID(),
                new BlobReference("webhook-inbox", "/sage/request.json"),
                "sage_team_notifications",
                originTime.toString(),
                now.toString(),
                "sage",
                null,
                new Checkpoint("team-queue", "QUEUE"),
                "LIVE_UPDATE",
                null);
    }

    @Test
    void serialize() throws JsonProcessingException {
        final String serialized = JacksonConfig.objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(example);
        System.out.println(serialized);
    }

    @Test
    void deserialize_good() {
        final var now = ZonedDateTime.now();
        System.out.println(now);

        final var startBody = """
                {
                  "parentRunId" : "f17a4df2-52dd-4b8b-be96-875688d1ca40",
                  "blobReference" : {
                    "containerName" : "webhook-inbox",
                    "path" : "/sage/request.json"
                  },
                  "flow" : "sage_team_notifications",
                  "originTimestamp" : "2024-03-18T15:49:43.855853+01:00[Europe/Stockholm]",
                  "runStartTime" : "%s",
                  "entityId" : "sage",
                  "checkpoint" : {
                    "checkpointPath" : "team-queue",
                    "checkpointType" : "QUEUE"
                  },
                  "operationType" : "LIVE_UPDATE"
                }
                """.formatted(now.toString());
        var result = StartBody.deserialize(startBody);
        result
                .peek(System.out::println)
                .peekLeft(error -> System.err.println(error.message()));

        assertTrue(result.isRight());
        var body = result.get();
        assertEquals(now, body.runStartTime);
    }

    @Test
    void deserialize_good_with_message() {
        final var now = ZonedDateTime.now();
        System.out.println(now);

        final var startBody = """
                {
                  "parentRunId" : "f17a4df2-52dd-4b8b-be96-875688d1ca40",
                  "blobReference" : {
                    "containerName" : "webhook-inbox",
                    "path" : "/sage/request.json"
                  },
                  "flow" : "sage_team_notifications",
                  "originTimestamp" : "2024-03-18T15:49:43.855853+01:00[Europe/Stockholm]",
                  "runStartTime" : "%s",
                  "entityId" : "sage",
                  "checkpoint" : {
                    "checkpointPath" : "team-queue",
                    "checkpointType" : "QUEUE"
                  },
                  "operationType" : "LIVE_UPDATE",
                  "message" : "Test message"
                }
                """.formatted(now.toString());
        var result = StartBody.deserialize(startBody);
        result
                .peek(System.out::println)
                .peekLeft(error -> System.err.println(error.message()));

        assertTrue(result.isRight());
        var body = result.get();
        assertEquals(now, body.runStartTime);
        assertEquals("Test message", body.message);
    }

    @Test
    void deserialize_local() {
        final var now = LocalDateTime.now();
        System.out.println(now);

        final var startBody = """
                                {
                  "parentRunId" : "f17a4df2-52dd-4b8b-be96-875688d1ca40",
                  "blobReference" : {
                    "containerName" : "webhook-inbox",
                    "path" : "/sage/request.json"
                  },
                  "flow" : "sage_team_notifications",
                  "originTimestamp" : "2024-03-18T15:49:43.855853+01:00[Europe/Stockholm]",
                  "runStartTime" : "%s",
                  "entityId" : "sage",
                  "checkpoint" : {
                    "checkpointPath" : "team-queue",
                    "checkpointType" : "QUEUE"
                  },
                  "operationType" : "LIVE_UPDATE"
                }
                """.formatted(now);
        var result = StartBody.deserialize(startBody);
        result
                .peek(System.out::println)
                .peekLeft(error -> System.err.println(error.message()));

        assertTrue(result.isLeft());
        var error = result.getLeft();

        assertThat(error.message(), containsString("Use java.time.Instant or java.time.ZonedDateTime."));
    }
}