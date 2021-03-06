
package com.markrai.csvparser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.markrai.csvparser.record.Call;
import com.markrai.csvparser.record.Message;
import com.markrai.csvparser.sqlite.DBWriter;
import com.markrai.csvparser.utility.UtilityMethods;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class PhoneRecordParser {

    Map<String, String> numbersAndNames;

    public PhoneRecordParser() throws SQLException {

        // obtain mapping of recognized numbers
        DBWriter dbw = new DBWriter(DBWriter.connect());
        numbersAndNames = dbw.fetchNames();

        PreparedStatement psc = dbw.getConnection().prepareStatement("DELETE from records;");
        System.out.println("Deleting all records");
        psc.executeUpdate();

    }

    public static String fileBeingProcessed;

    // determine whether file is call data, or messaging data
    void determineFileType(Reader csvData) throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(fileBeingProcessed));

        String line;

        while ((line = br.readLine()) != null) {

            if (line.contains("MESSAGING CHARGES")) {

                parseMessages(csvData);
                br.close();
                break;

            } else if ((line.contains("LOCAL AIRTIME"))) {

                parseCalls(csvData);
                br.close();
                break;
            }

        }

    }

    private void parseCalls(Reader callData) throws Exception {

        int lengthOfFile = (UtilityMethods.getNumberOfLines(fileBeingProcessed));
        List<Call> callList = new ArrayList<>();
        CSVParser parser = new CSVParserBuilder().withSeparator(',').withIgnoreQuotations(false).build();
        CSVReader csvReader = new CSVReaderBuilder(callData).withSkipLines(8).withCSVParser(parser).build();
        String[] line;

        for (int i = 0; i < lengthOfFile; i++) {

            line = csvReader.readNext();

            if (i < lengthOfFile - 10) {

                Call call = new Call();

                if (numbersAndNames.containsKey(line[3])) {

                    call.setName(numbersAndNames.get(line[3]));

                }

                call.setDateTime(UtilityMethods.createDateTime(line[0], line[1]));
                call.setDestination(line[2]);
                call.setNumber(line[3]);
                call.setMinutes(UtilityMethods.convertToMinutes(line[4]));
                call.setCallType(line[5]);

                callList.add(call);
            }
        }

        callData.close();
        csvReader.close();

        DBWriter dbw = new DBWriter(DBWriter.connect());

        for (Call c : callList) {

            dbw.insertCall(c.getDateTime(), c.getDestination(), c.getNumber(), c.getMinutes(), c.getCallType(),
                    c.getName());

        }

    }

    public void parseMessages(Reader messageData) throws Exception {

        int lengthOfFile = (UtilityMethods.getNumberOfLines(fileBeingProcessed));
        List<Message> messageList = new ArrayList<>();
        CSVParser parser = new CSVParserBuilder().withSeparator(',').withIgnoreQuotations(false).build();
        CSVReader csvReader = new CSVReaderBuilder(messageData).withSkipLines(8).withCSVParser(parser).build();
        String[] line;

        for (int i = 0; i < lengthOfFile; i++) {

            line = csvReader.readNext();

            if (i < lengthOfFile - 10) {
                Message message = new Message();

                if (numbersAndNames.containsKey(line[3])) {

                    message.setName(numbersAndNames.get(line[3]));

                }

                LocalDateTime dateTime = UtilityMethods.fixMessageTime(UtilityMethods.createDateTime(line[0], line[1]));

                message.setDateTime(dateTime);
                message.setDestination(line[2]);
                message.setNumber(line[3]);
                message.setDirection(line[4]);
                message.setTextType(line[5]);

                messageList.add(message);
            }

        }

        messageData.close();
        csvReader.close();

        DBWriter dbw = new DBWriter(DBWriter.connect());

        for (Message m : messageList) {

            dbw.insertMessage(m.getDateTime(), m.getDestination(), m.getNumber(), m.getDirection(), m.getTextType(),
                    m.getName());

        }

    }
}
