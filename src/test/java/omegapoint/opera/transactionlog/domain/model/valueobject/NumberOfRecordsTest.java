package omegapoint.opera.transactionlog.domain.model.valueobject;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonParseException;

import static org.junit.jupiter.api.Assertions.*;

class NumberOfRecordsTest {

    @Test
    void GoodCase() {
        assertEquals(5, new NumberOfRecords(5).value);
    }

    @Test
    void givenZero_ShouldAllow() {
        assertEquals(0, new NumberOfRecords(0).value);
    }

    @Test
    void givenNegative_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new NumberOfRecords(-1));
    }

}