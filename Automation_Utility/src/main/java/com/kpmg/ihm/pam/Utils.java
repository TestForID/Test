package com.kpmg.ihm.pam;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * Common Utilties.
 */
public class Utils {
    private Utils() {
        super();
    }


    private static Gson GSON = new Gson();
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());


    
    private static final String IPADDRESS_PATTERN = 
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
          
        
    private static Pattern PATTERN = Pattern.compile(IPADDRESS_PATTERN);
          
    public static boolean validate(final String ip){             
          Matcher matcher = PATTERN.matcher(ip);
          return matcher.matches();                 
    }


    public static String toIDString(Object o) {
        if (o!= null && o instanceof Number) {
            return String.valueOf(((Number) o).intValue());
        }
        throw new RuntimeException("Invalid value as input for ID: " + o);
    }


    public static String toIntString(String o) {
        if (o != null && o.endsWith(".0")) {
            return o.substring(0, o.length() - 2);
        }
        return o;
    }



    public static List jsonToList(String str) {
        return GSON.fromJson(str, List.class);
    }


    public static Map jsonToMap(String str) {
        return GSON.fromJson(str, Map.class);
    }

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }




    public static List<Map<String, String>> readSheetByName(String fileName, String sheetName) {
        FileInputStream file = null;
        try {
            file = new FileInputStream(new File(fileName));

            //Create Workbook instance holding reference to .xlsx file
    //            XSSFWorkbook workbook = new XSSFWorkbook(file);
            Workbook workbook = WorkbookFactory.create(file);

            Sheet sheet = workbook.getSheet(sheetName);
            return processSheet(sheet);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception ee) {
                    LOGGER.severe("Failed to close file " + fileName + " - " + ee);
                }
            }
        }
        return null;
    }



    private static List<Map<String, String>> processSheet(Sheet sheet) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        boolean headerMode = true;
        ArrayList<String> headers = new ArrayList<String>();
        
        //Iterate through each rows one by one
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
            Map<String, String> record = new HashMap<String, String>();
            Row row = rowIterator.next();


            int lastColumn = Math.max(row.getLastCellNum(), 20);

            for (int colIdx = 0; colIdx < lastColumn; colIdx ++) {
                Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) continue;
                if (headerMode) {
                    headers.add(cell.getStringCellValue());
                } else {
                    switch (cell.getCellType()) {
                    case NUMERIC:
                        record.put(headers.get(colIdx), String.valueOf(cell.getNumericCellValue()));
                        break;
                    case STRING:
                        record.put(headers.get(colIdx), cell.getStringCellValue());
                        break;
                    }
                }

            }

            if (!headerMode) result.add(record);                
            headerMode = false;
        }
        return result;
    }


    public static List<Map<String, String>> readSheet1(String fileName) {
        FileInputStream file = null;
        try {
            file = new FileInputStream(new File(fileName));

            //Create Workbook instance holding reference to .xlsx file
            //            XSSFWorkbook workbook = new XSSFWorkbook(file);
            Workbook workbook = WorkbookFactory.create(file);

            Sheet sheet = workbook.getSheetAt(0);

            return processSheet(sheet);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception ee) {
                    LOGGER.severe("Failed to close file " + fileName + " - " + ee);
                }
            }
        }
        return null;
    }

 


}
