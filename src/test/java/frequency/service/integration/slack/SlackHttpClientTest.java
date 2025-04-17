package frequency.service.integration.slack;

import meeting.frequency.secret.model.SlackSecrets;
import meeting.frequency.service.integration.slack.SlackHttpClient;
import meeting.frequency.service.integration.slack.pojo.BlocksItem;
import meeting.frequency.service.integration.slack.pojo.ElementsItem;
import meeting.frequency.service.integration.slack.pojo.MessagesItem;
import meeting.frequency.service.integration.slack.pojo.SlackHistoryResponse;
import meeting.frequency.service.integration.slack.pojo.user.SlackUserInfoResponse;
import meeting.frequency.service.integration.slack.pojo.user.User;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlackHttpClientTest {

    private final SlackSecrets slackSecrets = new SlackSecrets("token", "12345", "12345");
    private final HttpClient httpClient = mock(HttpClient.class);
    private final SlackHttpClient slackHttpClient = new SlackHttpClient(slackSecrets, httpClient, Logger.getLogger("test"));


    @Test
    public void should_fetch_history() {

        givenSlackReturnsJson("slack/historyResponse.json");

        final SlackHistoryResponse slackHistoryResponse =
                slackHttpClient.fetchSlackHistory(ZonedDateTime.now(ZoneId.of("Europe/Stockholm")).minusDays(7).toEpochSecond());

        thenEqualsExpectedResponse(slackHistoryResponse);
    }

    @Test
    public void should_handle_when_fetch_history_call_fails() {

        givenSlackReturnsJson("slack/historyResponseError.json");

        final SlackHistoryResponse slackHistoryResponse =
                slackHttpClient.fetchSlackHistory(ZonedDateTime.now(ZoneId.of("Europe/Stockholm")).minusDays(7).toEpochSecond());


        final SlackHistoryResponse expected = new SlackHistoryResponse(false, List.of(), false, "something went wrong");
        assertEquals(expected, slackHistoryResponse);
    }

    @Test
    public void should_return_error_user_information() {

        givenSlackReturnsJson("slack/userResponse.json");

        final SlackUserInfoResponse result = slackHttpClient.fetchUserInformation("UMLF");

        SlackUserInfoResponse expectedResponse = new SlackUserInfoResponse(true, new User("UMLF", "Nikita Berezkin"),
                null);

        assertEquals(expectedResponse, result);
    }

    @Test
    public void should_handle_when_user_info_call_fails() {

        givenSlackReturnsJson("slack/userResponseError.json");

        final SlackUserInfoResponse result = slackHttpClient.fetchUserInformation("UMLF");

        SlackUserInfoResponse expectedResponse = new SlackUserInfoResponse(false, User.EMPTY_USER(),
                "could not find user");

        assertEquals(expectedResponse, result);
    }

    @Test
    public void should_upload_file() {

        givenSlackReturnsJson("slack/startUploadFile.json", "slack/emptyResponse.json", "slack/completeUploadFile.json");

        final boolean success = slackHttpClient.uploadFile(new File("/tmp"));

        assertTrue(success);
    }

    @Test
    public void should_fail_when_start_upload_file_does_not_work() {

        givenSlackReturnsJson("slack/startUploadFileFails.json", "slack/emptyResponse.json", "slack/historyResponse.json");

        assertThrows(IllegalStateException.class, () -> slackHttpClient.uploadFile(new File("/tmp")));
    }

    @Test
    public void should_fail_when_complete_upload_file_does_not_work() {

        givenSlackReturnsJson("slack/startUploadFile.json", "slack/emptyResponse.json", "slack/completeUploadFileFails.json");

        final boolean success = slackHttpClient.uploadFile(new File("/tmp"));

        assertFalse(success);
    }

    private static void thenEqualsExpectedResponse(final SlackHistoryResponse slackHistoryResponse) {

        final List<MessagesItem> messagesItems = List.of(new MessagesItem("Testing1!", "message", "UMLF", "1743508273" +
                                                                                                          ".230809",
                        List.of(new BlocksItem(List.of(new ElementsItem(List.of(new ElementsItem(null, "text",
                                "Testing1!")), "rich_text_section", null)), "rich_text", "tCqgl"))),
                new MessagesItem("<@UMLF> has joined the channel", "message", "UMLF", "1743504626.880599", null)
        );
        final SlackHistoryResponse expected = new SlackHistoryResponse(true, messagesItems, false, null);

        assertEquals(expected, slackHistoryResponse);
    }

    private void givenSlackReturnsJson(final String... fileNames) {

        try {
            final List<String> responses = Arrays.stream(fileNames)
                    .map(fileName -> {
                        try {
                            return new String(getClass().getClassLoader().getResourceAsStream(fileName).readAllBytes());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();


            AtomicInteger counter = new AtomicInteger(0);

            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenAnswer(invocation -> {
                        int i = counter.getAndIncrement();
                        String body = (i < responses.size()) ? responses.get(i) : "Default Response";

                        HttpResponse<String> mockResponse = mock(HttpResponse.class);
                        when(mockResponse.body()).thenReturn(body);
                        when(mockResponse.statusCode()).thenReturn(200);
                        return mockResponse;
                    });
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}