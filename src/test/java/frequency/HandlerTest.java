package frequency;

import meeting.frequency.Handler;
import meeting.frequency.service.fetch.FetchMessageService;
import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.generate.GenerateDocumentService;
import meeting.frequency.service.process.ProcessMessageService;
import meeting.frequency.service.process.model.MeetingFrequency;
import meeting.frequency.service.upload.UploadService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class HandlerTest {

    private final FetchMessageService fetchMessageService = mock(FetchMessageService.class);
    private final ProcessMessageService processMessageService = mock(ProcessMessageService.class);
    private final GenerateDocumentService generateDocumentService = mock(GenerateDocumentService.class);
    private final UploadService uploadService = mock(UploadService.class);

    private final Handler handler = new Handler(fetchMessageService, processMessageService, generateDocumentService, uploadService);

    private final static List<Message> MESSAGES = List.of(new Message("Test T", "Stockholm", List.of("1 möte, OP")));
    private final static List<MeetingFrequency> MEETING_FREQUENCIES = List.of(new MeetingFrequency("Test T", 1,  List.of("OP"), ""));
    private final static File GENERATED_FILE;

    static {
        try {
            GENERATED_FILE = File.createTempFile("test", ".xlsx");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void should_pass_everything() throws Exception {
        givenMessageServiceReturns();
        givenProcessMessageServiceReturns();
        givenGenerateDocumentServiceReturns();
        givenUploadServiceReturnsTrue();

        handler.weeklyRepost(null, null);
    }

    /*
    @Test
    public void should_handle_failed_upload() throws Exception {
        givenMessageServiceReturns();
        givenProcessMessageServiceReturns();
        givenGenerateDocumentServiceReturns();
        givenUploadServiceReturnsFalse();

        final String result = handler.handleRequest(null, null);

        assertEquals("Failed", result);
    }

     */

    private void givenMessageServiceReturns() throws URISyntaxException {
        given(fetchMessageService.fetchMessages())
                .willReturn(MESSAGES);

    }

    private void givenProcessMessageServiceReturns() throws URISyntaxException, IOException {
        given(processMessageService.process(eq(MESSAGES)))
                .willReturn(MEETING_FREQUENCIES);

    }

    private void givenGenerateDocumentServiceReturns() throws URISyntaxException, IOException {
        given(generateDocumentService.generateDocument(eq(MEETING_FREQUENCIES)))
                .willReturn(GENERATED_FILE);

    }

    private void givenUploadServiceReturnsTrue(){
        given(uploadService.upload(eq(GENERATED_FILE)))
                .willReturn(true);
    }

    private void givenUploadServiceReturnsFalse(){
        given(uploadService.upload(eq(GENERATED_FILE)))
                .willReturn(false);
    }

}