package omegapoint.opera.transactionlog.api.model.entity;

import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloseTransactionTest {

    @Test
    void should_create_domain() {
        final Either<RejectMessage, CloseTransaction> deserialize = CloseTransaction.deserialize("{\"id\":\"48fbd4f2-9916-4274-9a30-60d6de3cd32e\",\"endTime\":\"2024-03-25T08:00:27.177679\",\"numberOfRecords\":\"0\",\"blobPath\":\"N/A\",\"isSuccess\":\"true\",\"reason\":\"NOT_APPLICABLE\"}\n");

        final Either<RejectMessage, omegapoint.opera.transactionlog.domain.model.entity.CloseTransaction> domain = CloseTransaction.toDomain(deserialize.get());
        
        assertTrue(domain.isRight());
    }
}