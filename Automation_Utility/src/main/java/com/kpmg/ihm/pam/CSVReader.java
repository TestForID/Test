package com.kpmg.ihm.pam;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


/**
 * Read CSV file.
 */
public class CSVReader {

    private static final Logger LOGGER = Logger.getLogger(CSVReader.class.getName());
    
    private static final String DELIM1 = " - ";
    private static final int LEN1 = DELIM1.length();
    private static final String DELIM2 = ": ";
    
    public static void main(String[] args) {
        
        if (args == null || args.length < 1) {
            LOGGER.info("Please provide host-account csv file name as 1st argument!");
            System.exit(1);
        }
        
        
        String hostAccountFile = args[0];


        try {
            Reader in = new FileReader(hostAccountFile);

            Iterable<CSVRecord> records = CSVFormat.RFC4180
                                                   .withFirstRecordAsHeader()
                                                   .parse(in);
            for (CSVRecord record : records) {
                Map<String, String> map = record.toMap();
                
                String assetAndOs = map.remove("Asset_and_OS_Name");
                int pos = assetAndOs.indexOf(DELIM1);
                
                String assetName = assetAndOs.substring(0, pos);
                String osName = assetAndOs.substring(pos + LEN1, assetAndOs.lastIndexOf(DELIM2));

                map.put("Asset_Name", assetName);
                map.put("OS_Name", osName);
            
                System.out.println(map);
                
                
            }
        } catch (FileNotFoundException fnfe) {
            LOGGER.log(Level.SEVERE, "File not found!", fnfe);
            System.exit(2);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Failed to read CSV file!", ioe);
            System.exit(2);
        }
             
    }

}
