package omegapoint.opera.operationaljournal.infrastructure.model;

import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RerunTest {

    @Test
    public void toDomain() {
        Rerun rerun = new Rerun(
                "3ab6445d-6529-404a-b3c0-ae69ea92ccad",
                3,
                "update_cinode_team",
                "2024-05-27 12:12:48.1655491",
                "orchestrator-team-update-to-cinode",
                "webhook-processed",
                "sage-team-update/2024/05/27/a9f0b5b7-29f5-450e-bc0b-ccc3faa16c01.json"
        );
        omegapoint.opera.operationaljournal.domain.model.table.Rerun actual = rerun.toDomain();


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS");
        omegapoint.opera.operationaljournal.domain.model.table.Rerun expected = new omegapoint.opera.operationaljournal.domain.model.table.Rerun(
                UUID.fromString("3ab6445d-6529-404a-b3c0-ae69ea92ccad"),
                3,
                new WebhookStep("update_cinode_team"),
                ZonedDateTime.parse("2024-05-27T10:12:48.165549100Z"),
                "orchestrator-team-update-to-cinode",
                new BlobReference("webhook-processed", "sage-team-update/2024/05/27/a9f0b5b7-29f5-450e-bc0b-ccc3faa16c01.json")
        );

        assertEquals(expected, actual);
    }
}