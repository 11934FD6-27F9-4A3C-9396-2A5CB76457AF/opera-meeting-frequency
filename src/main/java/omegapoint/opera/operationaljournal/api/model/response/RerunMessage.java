package omegapoint.opera.operationaljournal.api.model.response;

import java.util.List;
import java.util.stream.Stream;

public record RerunMessage(List<Rerun> reruns) {
    public static RerunMessage fromDomain(Stream<omegapoint.opera.operationaljournal.domain.model.table.Rerun> domainObjects) {
        return new RerunMessage(domainObjects
                .map(Rerun::fromDomain)
                .toList());
    }

    public static RerunMessage fromDomain(List<omegapoint.opera.operationaljournal.domain.model.table.Rerun> domainReruns) {
        return new RerunMessage(domainReruns
                .stream()
                .map(Rerun::fromDomain)
                .toList());
    }
}
