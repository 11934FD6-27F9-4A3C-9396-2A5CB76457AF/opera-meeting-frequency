package omegapoint.opera.operationaljournal.api.model.response;

import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;
import omegapoint.opera.operationaljournal.domain.model.table.Rerun;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class RerunMessageTest {

    @Test
    public void list_of_reruns_to_RerunMessage() {
        List<Rerun> domainReruns = List.of(
                new Rerun[]{
                        new Rerun(
                                UUID.randomUUID(),
                                2,
                                new WebhookStep("update_cinode_team"),
                                ZonedDateTime.now(),
                                "QUEUE_NAME",
                                new BlobReference("containerName", "pa/th")
                        ),
                        new Rerun(
                                UUID.randomUUID(),
                                2,
                                new WebhookStep("update_cinode_team"),
                                ZonedDateTime.now(),
                                "QUEUE_NAME",
                                new BlobReference("containerName", "pa/th")
                        )});

        assertDoesNotThrow(() -> RerunMessage.fromDomain(domainReruns));
    }
}
