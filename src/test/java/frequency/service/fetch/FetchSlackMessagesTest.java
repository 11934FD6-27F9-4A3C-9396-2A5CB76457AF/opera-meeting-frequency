package frequency.service.fetch;


import meeting.frequency.parameter.Office;
import meeting.frequency.service.fetch.FetchSlackMessages;
import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.integration.slack.SlackHttpClient;
import meeting.frequency.service.integration.slack.pojo.BlocksItem;
import meeting.frequency.service.integration.slack.pojo.MessagesItem;
import meeting.frequency.service.integration.slack.pojo.SlackHistoryResponse;
import meeting.frequency.service.integration.slack.pojo.user.SlackUserInfoResponse;
import meeting.frequency.service.integration.slack.pojo.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FetchSlackMessagesTest {

    private final SlackHttpClient slackHttpClient = mock(SlackHttpClient.class);

    private final Map<String, Office> nameToOfficeMapping = Map.of(
            "Test testsson", Office.STOCKHOLM,
            "Bengt testsson", Office.STOCKHOLM
    );

    private final FetchSlackMessages fetchSlackMessages = new FetchSlackMessages(slackHttpClient, nameToOfficeMapping, Logger.getLogger("test"));

    private final static String USER_ID_1 = "123";
    private final static String USER_ID_2 = "456";

    @Test
    public void should_successfully_fetch_messages() {

        givenSlackClientReturnsHistory();
        givenSlackClientReturnsUserInformation();

        final List<Message> messages = fetchSlackMessages.fetchMessages();

        messages.forEach(message -> {
            assertEquals("Test testsson", message.name());
            assertEquals(2, message.rawMessages().size());
        });
    }

    @Test
    public void should_successfully_fetch_messages_multiple_users() {

        givenSlackClientReturnsHistoryMultipleUsers();
        givenSlackClientReturnsUserInformation();

        final List<Message> messages = fetchSlackMessages.fetchMessages();


        assertEquals(2, messages.size());

        assertTrue(messages.contains(new Message("Test testsson", "Stockholm", List.of("Test"))));
        assertTrue(messages.contains(new Message("Bengt testsson", "Stockholm", List.of("Test2"))));
    }

    @Test
    public void should_handle_when_user_not_found() {

        givenSlackClientReturnsHistory();
        givenSlackClientReturnsNoUserInfo();

        final List<Message> messages = fetchSlackMessages.fetchMessages();

        messages.forEach(message -> {
            assertEquals(0, message.rawMessages().size());
        });
    }

    @Test
    public void should_handle_when_no_history_is_returned() {

        givenSlackClientReturnsNoHistory();
        givenSlackClientReturnsUserInformation();

        final List<Message> messages = fetchSlackMessages.fetchMessages();

        messages.forEach(message -> {
            assertEquals(0, message.rawMessages().size());
        });

        verify(slackHttpClient, never()).fetchUserInformation(any(String.class));
    }


    private void givenSlackClientReturnsHistory() {

        given(slackHttpClient.fetchSlackHistory(any(Long.class)))
                .willReturn(new SlackHistoryResponse(true, List.of(
                        new MessagesItem("Test", "message", USER_ID_1, "1743508273.230809",
                                List.of(new BlocksItem(List.of(), "message", "333"))),
                        new MessagesItem("Test1", "message", USER_ID_1, "1743538223.230809", null),
                        new MessagesItem("Test2", "message", USER_ID_1, "1743508275.630809",
                                List.of(new BlocksItem(List.of(), "message", "333")))),
                        false, null));
    }

    private void givenSlackClientReturnsHistoryMultipleUsers() {

        given(slackHttpClient.fetchSlackHistory(any(Long.class)))
                .willReturn(new SlackHistoryResponse(true, List.of(
                        new MessagesItem("Test", "message", USER_ID_1, "1743508273.230809",
                                List.of(new BlocksItem(List.of(), "message", "333"))),
                        new MessagesItem("Test2", "message", USER_ID_2, "1743508275.630809",
                                List.of(new BlocksItem(List.of(), "message", "333")))),
                        false, null));
    }

    private void givenSlackClientReturnsNoHistory() {

        given(slackHttpClient.fetchSlackHistory(any(Long.class)))
                .willReturn(new SlackHistoryResponse(true, List.of(), false, "access denied"));
    }

    private void givenSlackClientReturnsUserInformation() {

        given(slackHttpClient.fetchUserInformation(eq(USER_ID_1)))
                .willReturn(new SlackUserInfoResponse(true, new User(USER_ID_1, "Test testsson"), null));

        given(slackHttpClient.fetchUserInformation(eq(USER_ID_2)))
                .willReturn(new SlackUserInfoResponse(true, new User(USER_ID_2, "Bengt testsson"), null));
    }

    private void givenSlackClientReturnsNoUserInfo() {

        given(slackHttpClient.fetchUserInformation(eq(USER_ID_1)))
                .willReturn(new SlackUserInfoResponse(false, User.EMPTY_USER(), "not found"));
    }
}