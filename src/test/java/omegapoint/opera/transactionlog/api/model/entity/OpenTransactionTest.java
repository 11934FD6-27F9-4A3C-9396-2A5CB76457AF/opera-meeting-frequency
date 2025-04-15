package omegapoint.opera.transactionlog.api.model.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenTransactionTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serialize_deserialize() throws JsonProcessingException {
        OpenTransaction openTransaction = new OpenTransaction(
                UUID.randomUUID(),
                "2020-01-01",
                "Maconomy",
                "DWH",
                "Transaction",
                "Both",
                "Manual",
                "trigger",
                true
        );

        final String serialized = objectMapper.writeValueAsString(openTransaction);
        final OpenTransaction result = objectMapper.readValue(serialized, OpenTransaction.class);
        assertEquals(openTransaction, result);
    }
}