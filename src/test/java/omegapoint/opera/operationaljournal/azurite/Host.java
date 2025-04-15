package omegapoint.opera.operationaljournal.azurite;

import java.util.Objects;

public class Host {
    private static final String LOCALHOST = "localhost";
    private static final String LOCAL_LOOPBACK = "127.0.0.1";
    public final String value;

    private Host(final String value) {
        this.value = value;
    }

    public static Host fromDockerIP(final String ip) {

        // Docker returns the IP as 'localhost' (not an IP...) when running on developer machines.
        // In Azure DevOps, where the IP is an actual IP, a difference in behavior is observed in Azurite.
        // To reach consistent behavior, we replace 'localhost' with '127.0.0.1' in such cases.
        // Investigate further, especially with respect to the actual behavior of Azure Blob Storage, if any issues arise in the future.
        if (ip.equals(LOCALHOST)) {
            System.out.printf("Detected that the testcontainer for Azurite is bound to '%s'. Replacing the connection string with '%s' before passing it to the Azure SDK.%n", LOCALHOST, LOCAL_LOOPBACK);
            return new Host(LOCAL_LOOPBACK);
        } else {
            return new Host(ip);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Host host = (Host) o;
        return Objects.equals(value, host.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
