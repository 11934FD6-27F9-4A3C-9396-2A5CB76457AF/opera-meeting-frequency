package omegapoint.opera.operationaljournal.api.model.response;

import java.util.UUID;

public record Rerun(UUID runId,
                    int attempt,
                    String webhookStep,
                    String originTimestamp,
                    String queueName,
                    String blobContainerName,
                    String blobPath) {

    public static Rerun fromDomain(omegapoint.opera.operationaljournal.domain.model.table.Rerun rerun) {
        return new Rerun(
                rerun.runId(),
                rerun.attempt(),
                rerun.webhookStep().value,
                rerun.originTimestamp().toString(),
                rerun.queueName(),
                rerun.blobReference().containerName,
                rerun.blobReference().path);
    }
}
