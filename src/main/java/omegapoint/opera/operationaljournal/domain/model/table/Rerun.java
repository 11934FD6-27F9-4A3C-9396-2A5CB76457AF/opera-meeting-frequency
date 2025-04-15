package omegapoint.opera.operationaljournal.domain.model.table;

import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;

import java.time.ZonedDateTime;
import java.util.UUID;

public record Rerun(UUID runId,
                    int attempt,
                    WebhookStep webhookStep,
                    ZonedDateTime originTimestamp,
                    String queueName,
                    BlobReference blobReference) {
}
