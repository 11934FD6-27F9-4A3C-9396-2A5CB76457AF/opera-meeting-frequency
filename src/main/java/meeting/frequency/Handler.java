package meeting.frequency;

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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Handler {

    private final FetchMessageService fetchMessageService;
    private final ProcessMessageService processMessageService;
    private final GenerateDocumentService generateDocumentService;
    private final UploadService uploadService;

    private final Logger logger;

    public Handler(final FetchMessageService fetchMessageService, final ProcessMessageService processMessageService,
                   final GenerateDocumentService generateDocumentService, final UploadService uploadService,
                   final Logger logger) {

        this.fetchMessageService = fetchMessageService;
        this.processMessageService = processMessageService;
        this.generateDocumentService = generateDocumentService;
        this.uploadService = uploadService;
        this.logger = logger;
    }

    public Handler(final Logger logger) {

        SecretService secretService = new SecretServiceImpl();
        ParameterService parameterService = new ParameterServiceImpl();

        this.fetchMessageService = new FetchSlackMessages(secretService, parameterService, logger);
        this.processMessageService = new OpenAIService(secretService);
        this.generateDocumentService = new ExcelDocumentService();
        this.uploadService = new SlackUploadService(secretService, logger);
        this.logger = logger;
    }


    public void weeklyRepost() {

        try {

            logger.log(Level.INFO, "Fetching messages...");
            final List<Message> messages = fetchMessageService.fetchMessages();

            if (messages.isEmpty()) {
                logger.log(Level.SEVERE, "Could not find messages");
                throw new IllegalStateException("Could not find messages");
            }

            logger.log(Level.INFO, "Process messages...");
            final List<MeetingFrequency> meetingFrequency = processMessageService.process(messages)
                    .stream()
                    .filter(frequency -> frequency.meetings() > 0)
                    .sorted(Comparator.comparingInt(MeetingFrequency::meetings).reversed())
                    .toList();

            logger.log(Level.INFO, "Generate file...");
            File file = generateDocumentService.generateDocument(meetingFrequency);

            logger.log(Level.INFO, "Upload file...");
            final boolean uploadSuccess = uploadService.upload(file);

            if (uploadSuccess) {
                logger.log(Level.INFO, "Successfully uploaded file : " + file);
            } else {
                logger.log(Level.SEVERE, "Failed uploaded file : " + file);
                throw new RuntimeException("Failed to upload file");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}