package com.taxonomy.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single training record from the CSV file
 */
public class TaxonomyRecord {
    private final String text;
    private final String l1;
    private final String l2;
    private final String l3; // Optional for extension
    private final String l4; // Optional for extension

    public TaxonomyRecord(String text, String l1, String l2) {
        this(text, l1, l2, null, null);
    }

    public TaxonomyRecord(String text, String l1, String l2, String l3, String l4) {
        this.text = text;
        this.l1 = l1;
        this.l2 = l2;
        this.l3 = l3;
        this.l4 = l4;
    }

    // Getters
    public String getText() {
        return text;
    }

    public String getL1() {
        return l1;
    }

    public String getL2() {
        return l2;
    }

    public String getL3() {
        return l3;
    }

    public String getL4() {
        return l4;
    }

    @Override
    public String toString() {
        return String.format("TaxonomyRecord{text='%s', l1='%s', l2='%s', l3='%s', l4='%s'}",
                text, l1, l2, l3, l4);
    }

    /**
     * Utility class to read CSV data
     */
    public static class CSVReader {

        /**
         * Reads taxonomy data from CSV file
         * 
         * @param csvFilePath path to the CSV file
         * @return list of TaxonomyRecord objects
         * @throws IOException if file cannot be read
         */
        public static List<TaxonomyRecord> readCSV(String csvFilePath) throws IOException {
            List<TaxonomyRecord> records = new ArrayList<>();

            try (FileReader reader = new FileReader(csvFilePath);
                    CSVParser parser = CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                            .withIgnoreHeaderCase()
                            .withTrim()
                            .parse(reader)) {

                for (CSVRecord record : parser) {
                    String text = record.get("text");
                    String l1 = record.get("L1");
                    String l2 = record.get("L2");

                    // Handle optional L3, L4 columns
                    String l3 = null;
                    String l4 = null;
                    try {
                        l3 = record.get("L3");
                    } catch (IllegalArgumentException e) {
                        // L3 column doesn't exist
                    }
                    try {
                        l4 = record.get("L4");
                    } catch (IllegalArgumentException e) {
                        // L4 column doesn't exist
                    }

                    records.add(new TaxonomyRecord(text, l1, l2, l3, l4));
                }
            }

            return records;
        }
    }
}
