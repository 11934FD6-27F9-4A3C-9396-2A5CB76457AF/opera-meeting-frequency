package meeting.frequency.service.generate;

import meeting.frequency.service.process.model.MeetingFrequency;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExcelClient {

    public File generateDocument(final List<MeetingFrequency> meetingFrequencyList,
                                 final Map<String, Integer> officeToTotalMeetings) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet =
                workbook.createSheet("Meeting Frequency " + LocalDate.now().minusDays(7) + " - " + LocalDate.now());

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Total meetings");
        headerRow.createCell(2).setCellValue("Companies");
        headerRow.createCell(3).setCellValue("Office");

        int rowNum = 1;
        for (MeetingFrequency meetingFrequency : meetingFrequencyList) {
            Row row = sheet.createRow(rowNum++);


            row.createCell(0).setCellValue(meetingFrequency.name());
            row.createCell(1).setCellValue(meetingFrequency.meetings());
            row.createCell(2).setCellValue(String.join(", ", meetingFrequency.companies()));
            row.createCell(3).setCellValue(meetingFrequency.office());
        }
        Row totalMeetingsRow = sheet.createRow(rowNum++);
        totalMeetingsRow.createCell(1).setCellValue(meetingFrequencyList.stream().mapToInt(MeetingFrequency::meetings).sum());

        rowNum = rowNum + 2;
        Row officeTotalMeetingsHeaderRow = sheet.createRow(rowNum++);

        officeTotalMeetingsHeaderRow.createCell(0).setCellValue("Office");
        officeTotalMeetingsHeaderRow.createCell(1).setCellValue("Total Meetings");

        for (Map.Entry<String, Integer> officeToTotalMeetingsEntry : officeToTotalMeetings.entrySet()) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(officeToTotalMeetingsEntry.getKey());
            row.createCell(1).setCellValue(officeToTotalMeetingsEntry.getValue());
        }

        // Auto-size columns for better readability
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);

        //Only way to store write files on AWS lambda
        File classDir = new File(System.getProperty("java.io.tmpdir"));

        File file = new File(classDir,
                "MeetingFrequency %s.xlsx".formatted(LocalDate.now().minusDays(7) + " - " + LocalDate.now()));

        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);

            workbook.close();

            return file;
        }

    }
}
