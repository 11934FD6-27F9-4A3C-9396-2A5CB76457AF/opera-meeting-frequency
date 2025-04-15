package meeting.frequency.service.integration.slack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import meeting.frequency.secret.SecretService;
import meeting.frequency.secret.model.SlackSecrets;
import meeting.frequency.service.integration.slack.pojo.SlackHistoryRequest;
import meeting.frequency.service.integration.slack.pojo.SlackHistoryResponse;
import meeting.frequency.service.integration.slack.pojo.upload.SlackUploadCompleteRequest;
import meeting.frequency.service.integration.slack.pojo.upload.SlackUploadCompleteResponse;
import meeting.frequency.service.integration.slack.pojo.upload.SlackUploadRequest;
import meeting.frequency.service.integration.slack.pojo.upload.SlackUploadStartResponse;
import meeting.frequency.service.integration.slack.pojo.upload.UploadFile;
import meeting.frequency.service.integration.slack.pojo.user.SlackUserInfoResponse;
import meeting.frequency.service.integration.slack.pojo.user.User;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class SlackHttpClient {

    private final SlackSecrets slackSecrets;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HISTORY_URL = "https://slack.com/api/conversations.history";
    private static final String USER_INFO_URL = "https://slack.com/api/users.info";
    private static final String GET_UPLOAD_URL_API = "https://slack.com/api/files.getUploadURLExternal";
    private static final String COMPLETE_UPLOAD_API = "https://slack.com/api/files.completeUploadExternal";

    private final static int LIMIT_HISTORY_RESPONSE = 999;

    public SlackHttpClient(final SecretService secretService) {
        this.slackSecrets = secretService.fetchSlackSecrets();
        this.httpClient = HttpClient.newBuilder()
                .build();
    }

    public SlackHttpClient(final SlackSecrets slackSecrets, final HttpClient httpClient) {
        this.slackSecrets = slackSecrets;
        this.httpClient = httpClient;
    }

    public SlackHistoryResponse fetchSlackHistory(final long startTimestamp){
        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(HISTORY_URL))
                    .headers("Content-Type","application/json", "Authorization", "Bearer " + slackSecrets.token())
                    .POST(HttpRequest.BodyPublishers.ofString(getHistoryRequestBodyAsString(startTimestamp)))
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            final SlackHistoryResponse slackHistoryResponse = objectMapper.readValue(response.body(), SlackHistoryResponse.class);

            if(slackHistoryResponse.ok()){
                return slackHistoryResponse;
            }

            System.out.printf("Error when fetching channel history : %s".formatted(slackHistoryResponse.error()));
            return new SlackHistoryResponse(false, List.of(), false, slackHistoryResponse.error());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public SlackUserInfoResponse fetchUserInformation(final String userId){
        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(USER_INFO_URL + "?user=" + userId))
                    .headers("Content-Type","application/json", "Authorization", "Bearer " + slackSecrets.token())
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            final SlackUserInfoResponse slackUserInfoResponse = objectMapper.readValue(response.body(), SlackUserInfoResponse.class);

            if(slackUserInfoResponse.ok()){
                return slackUserInfoResponse;
            }

            System.out.printf("Could not fetch user info on userId %s, got the following error : %s%n", userId, slackUserInfoResponse.error());
            return new SlackUserInfoResponse(false, User.EMPTY_USER(), slackUserInfoResponse.error());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean uploadFile(final File file){
        try {

            final SlackUploadStartResponse startUploadResponse = requestUploadUrl(file.getName(), file.length());

            if(uploadFileToUrl(startUploadResponse.upload_url(), file)){
                return finalizeUpload(startUploadResponse.file_id());
            }

            return false;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private SlackUploadStartResponse requestUploadUrl(final String fileName, final long fileSize) throws URISyntaxException, IOException, InterruptedException {


        final SlackUploadRequest slackUploadRequest = new SlackUploadRequest(fileName, fileSize);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(GET_UPLOAD_URL_API))
                .headers("Content-Type","application/x-www-form-urlencoded", "Authorization", "Bearer " + slackSecrets.token())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "filename=%s&length=%s".formatted(slackUploadRequest.filename(), slackUploadRequest.length())
                ))
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        final SlackUploadStartResponse slackUploadStartResponse = objectMapper.readValue(response.body(), SlackUploadStartResponse.class);

        if(slackUploadStartResponse.ok()){
            return slackUploadStartResponse;
        }
        System.out.println("Error when fetching start upload url : " + slackUploadStartResponse);

        throw new IllegalStateException("Error when fetching upload url");
    }

    private String getHistoryRequestBodyAsString(final long startTimestamp) throws JsonProcessingException {

        return objectMapper
                .writeValueAsString(new SlackHistoryRequest(slackSecrets.channelId(), String.valueOf(startTimestamp),
                        String.valueOf(ZonedDateTime.now(ZoneId.of("Europe/Stockholm")).toEpochSecond()), LIMIT_HISTORY_RESPONSE));
    }

    private boolean uploadFileToUrl(final String uploadUrl,final File file) throws URISyntaxException, IOException, InterruptedException {

        Path filePath = Paths.get(file.getPath());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uploadUrl))
                .headers("Content-Type","application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofFile(filePath))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() == 200){
            return true;
        }

        System.out.println("Status code " + response.statusCode());
        System.out.println("Error when uploading file " + response.body());

        return false;
    }

    private boolean finalizeUpload(final String fileId) throws URISyntaxException, IOException, InterruptedException {



        String meetingTitle = "Meeting Frequency " + LocalDate.now().minusDays(7) + " - " + LocalDate.now();
        final SlackUploadCompleteRequest test = new SlackUploadCompleteRequest(List.of(new UploadFile(fileId, meetingTitle)),
                slackSecrets.sendToChannelId());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(COMPLETE_UPLOAD_API))
                .headers("Content-Type","application/json", "Authorization", "Bearer " + slackSecrets.token())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(test)))
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        final SlackUploadCompleteResponse slackUploadCompleteResponse = objectMapper.readValue(response.body(), SlackUploadCompleteResponse.class);

        if (slackUploadCompleteResponse.ok()){
            return true;
        }

        System.out.println("Error when completing upload of file " + response.body());

        return false;
    }


}
