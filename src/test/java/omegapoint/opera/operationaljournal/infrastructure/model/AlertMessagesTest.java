package omegapoint.opera.operationaljournal.infrastructure.model;

import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertMessagesTest {
    @Test
    void testFromDomain() {
        List<JournalErrorItem> journalErrorItems = List.of(new JournalErrorItem(
                        "123",
                        6,
                        "2022-01-11",
                        "status",
                        "a message",
                        "functionapp",
                        "webhook1",
                        "87b93799-7471-4218-8cad-075779360cea"),
                new JournalErrorItem(
                        "123",
                        6,
                        "2022-01-11",
                        "status",
                        "a message",
                        "functionapp",
                        "webhook1",
                        "87b93799-7471-4218-8cad-075779360cea"),
                new JournalErrorItem(
                        "123",
                        6,
                        "2022-01-11",
                        "status",
                        "a message",
                        "functionapp",
                        "webhook2",
                        "87b93799-7471-4218-8cad-075779360cea")
        );
        AlertMessages actual = AlertMessages.fromDomain(journalErrorItems);
        List<String> expected = List.of("Journal Error Alert\n" +
                "Parent ID: 87b93799-7471-4218-8cad-075779360cea\n" +
                "Run ID: 123\n" +
                "Timestamp: 2022-01-11\n" +
                "Status: status\n" +
                "Message: a message\n" +
                "Origin Function: functionapp\n" +
                "Webhook Step: webhook2\n" +
                "Number of webhook fails: 1",
                "Journal Error Alert\n" +
                "Parent ID: 87b93799-7471-4218-8cad-075779360cea\n" +
                "Run ID: 123\n" +
                "Timestamp: 2022-01-11\n" +
                "Status: status\n" +
                "Message: a message\n" +
                "Origin Function: functionapp\n" +
                "Webhook Step: webhook1\n" +
                "Number of webhook fails: 2");

        assertEquals(expected, actual.messages);
    }
}