package omegapoint.opera.operationaljournal.azurite;

import java.util.Objects;

public class Port {
    public final String value;

    public Port(final String value) {
        this.value = value;
    }

    public static Port fromInteger(int port) {
        return new Port(Integer.toString(port));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Port port = (Port) o;
        return Objects.equals(value, port.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
