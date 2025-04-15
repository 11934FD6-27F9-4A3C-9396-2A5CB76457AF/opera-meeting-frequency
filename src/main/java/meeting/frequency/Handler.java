package meeting.frequency;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import meeting.frequency.parameter.ParameterService;
import meeting.frequency.parameter.ParameterServiceImpl;
import meeting.frequency.secret.SecretService;
import meeting.frequency.secret.SecretServiceImpl;
import meeting.frequency.service.fetch.FetchMessageService;
import meeting.frequency.service.fetch.FetchSlackMessages;
import meeting.frequency.service.fetch.model.Message;
import meeting.frequency.service.generate.ExcelDocumentService;
import meeting.frequency.service.generate.GenerateDocumentService;
import meeting.frequency.service.process.OpenAIService;
import meeting.frequency.service.process.ProcessMessageService;
import meeting.frequency.service.process.model.MeetingFrequency;
import meeting.frequency.service.upload.SlackUploadService;
import meeting.frequency.service.upload.UploadService;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class Handler {

    private final FetchMessageService fetchMessageService;
    private final ProcessMessageService processMessageService;
    private final GenerateDocumentService generateDocumentService;
    private final UploadService uploadService;

    public Handler(final FetchMessageService fetchMessageService, final ProcessMessageService processMessageService,
                   final GenerateDocumentService generateDocumentService, final UploadService uploadService) {

        this.fetchMessageService = fetchMessageService;
        this.processMessageService = processMessageService;
        this.generateDocumentService = generateDocumentService;
        this.uploadService = uploadService;
    }

    public Handler() {

        SecretService secretService = new SecretServiceImpl();
        ParameterService parameterService = new ParameterServiceImpl();

        this.fetchMessageService = new FetchSlackMessages(secretService, parameterService);
        this.processMessageService = new OpenAIService(secretService);
        this.generateDocumentService = new ExcelDocumentService();
        this.uploadService = new SlackUploadService(secretService);
    }


    @FunctionName("weekly-report-trigger")
    public void weeklyRepost(final @TimerTrigger(
                    name = "weekly-report-trigger",
                    schedule = "0 2 * * 4") String timerInfo, ExecutionContext context) {

        try {

            System.out.println("Fetching messages...");
            final List<Message> messages = fetchMessageService.fetchMessages();

            if(messages.isEmpty()){
                throw new IllegalStateException("Could not find messages");
            }

            System.out.println("Process messages...");
            final List<MeetingFrequency> meetingFrequency = processMessageService.process(messages)
                    .stream()
                    .filter(frequency -> frequency.meetings() > 0)
                    .sorted(Comparator.comparingInt(MeetingFrequency::meetings).reversed())
                    .toList();

            System.out.println("Generate file...");
            File file = generateDocumentService.generateDocument(meetingFrequency);

            System.out.println("Upload file...");
            final boolean uploadSuccess = uploadService.upload(file);

            if (uploadSuccess){
                System.out.println("Successfully uploaded file : " + file);
            } else {
                System.out.println("Failed uploaded file : " + file);
                throw new RuntimeException("Failed to upload file");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}