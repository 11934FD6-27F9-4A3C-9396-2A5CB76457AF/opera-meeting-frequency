package omegapoint.opera.operationaljournal.api.model.response;

import lombok.NonNull;

import java.util.Optional;
import java.util.UUID;

public record RootRunId(String runId) {

    public static RootRunId fromDomain(@NonNull Optional<UUID> rootRunId) {
        return rootRunId.map(runId -> new RootRunId(runId.toString()))
                .orElse(new RootRunId(null));
    }
}
