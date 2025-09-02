package com.taxonomy.training;

import com.taxonomy.data.TaxonomyRecord;
import opennlp.tools.doccat.*;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Trainer for hierarchical taxonomy classification models
 */
public class HierarchicalModelTrainer {

    private static final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

    /**
     * Configuration for training parameters
     */
    public static class TrainingConfig {
        private int iterations = 100;
        private int cutoff = 5;
        private String algorithm = "MAXENT";

        public TrainingConfig setIterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        public TrainingConfig setCutoff(int cutoff) {
            this.cutoff = cutoff;
            return this;
        }

        public TrainingConfig setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public TrainingParameters toTrainingParameters() {
            TrainingParameters params = new TrainingParameters();
            params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(iterations));
            params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(cutoff));
            params.put(TrainingParameters.ALGORITHM_PARAM, algorithm);
            return params;
        }
    }

    /**
     * Trains L1 (top-level) classification model
     * 
     * @param records training data
     * @param config  training configuration
     * @return trained L1 model
     * @throws IOException if training fails
     */
    public DoccatModel trainL1Model(List<TaxonomyRecord> records, TrainingConfig config) throws IOException {
        System.out.println("Training L1 model with " + records.size() + " records...");

        // Create training samples for L1 classification
        ObjectStream<DocumentSample> sampleStream = createL1SampleStream(records);

        // Train the model
        DoccatModel model = DocumentCategorizerME.train("en", sampleStream,
                config.toTrainingParameters(),
                new DoccatFactory());

        System.out.println("L1 model training completed.");
        return model;
    }

    /**
     * Trains L2 models for each L1 category
     * 
     * @param records training data
     * @param config  training configuration
     * @return map of L1 category to L2 model
     * @throws IOException              if training fails
     * @throws IllegalArgumentException if any L1-L2 combination has fewer than 3
     *                                  records
     */
    public Map<String, DoccatModel> trainL2Models(List<TaxonomyRecord> records, TrainingConfig config)
            throws IOException {

        // Validate L1-L2 combinations have sufficient data
        validateL1L2Combinations(records, 3);

        Map<String, DoccatModel> l2Models = new HashMap<>();

        // Group records by L1 category
        Map<String, List<TaxonomyRecord>> recordsByL1 = records.stream()
                .collect(Collectors.groupingBy(TaxonomyRecord::getL1));

        for (Map.Entry<String, List<TaxonomyRecord>> entry : recordsByL1.entrySet()) {
            String l1Category = entry.getKey();
            List<TaxonomyRecord> l1Records = entry.getValue();

            System.out.println("Training L2 model for " + l1Category + " with " + l1Records.size() + " records...");

            // Create training samples for L2 classification within this L1 category
            ObjectStream<DocumentSample> sampleStream = createL2SampleStream(l1Records);

            // Train the L2 model
            DoccatModel model = DocumentCategorizerME.train("en", sampleStream,
                    config.toTrainingParameters(),
                    new DoccatFactory());

            l2Models.put(l1Category, model);
            System.out.println("L2 model for " + l1Category + " training completed.");
        }

        return l2Models;
    }

    /**
     * Trains L3 models for each L1-L2 combination (extension capability)
     * 
     * @param records training data
     * @param config  training configuration
     * @return map of "L1_L2" key to L3 model
     * @throws IOException if training fails
     */
    public Map<String, DoccatModel> trainL3Models(List<TaxonomyRecord> records, TrainingConfig config)
            throws IOException {
        Map<String, DoccatModel> l3Models = new HashMap<>();

        // Filter records that have L3 data
        List<TaxonomyRecord> recordsWithL3 = records.stream()
                .filter(r -> r.getL3() != null && !r.getL3().trim().isEmpty())
                .collect(Collectors.toList());

        if (recordsWithL3.isEmpty()) {
            System.out.println("No L3 data found in records. Skipping L3 model training.");
            return l3Models;
        }

        // Group records by L1_L2 combination
        Map<String, List<TaxonomyRecord>> recordsByL1L2 = recordsWithL3.stream()
                .collect(Collectors.groupingBy(r -> r.getL1() + "_" + r.getL2()));

        for (Map.Entry<String, List<TaxonomyRecord>> entry : recordsByL1L2.entrySet()) {
            String l1L2Key = entry.getKey();
            List<TaxonomyRecord> l1L2Records = entry.getValue();

            // Only train if we have enough samples
            if (l1L2Records.size() < 5) {
                System.out.println("Skipping L3 model for " + l1L2Key + " - insufficient data (" + l1L2Records.size()
                        + " records)");
                continue;
            }

            System.out.println("Training L3 model for " + l1L2Key + " with " + l1L2Records.size() + " records...");

            ObjectStream<DocumentSample> sampleStream = createL3SampleStream(l1L2Records);

            DoccatModel model = DocumentCategorizerME.train("en", sampleStream,
                    config.toTrainingParameters(),
                    new DoccatFactory());

            l3Models.put(l1L2Key, model);
            System.out.println("L3 model for " + l1L2Key + " training completed.");
        }

        return l3Models;
    }

    /**
     * Creates L1 training sample stream
     */
    private ObjectStream<DocumentSample> createL1SampleStream(List<TaxonomyRecord> records) {
        List<DocumentSample> samples = records.stream()
                .map(record -> new DocumentSample(record.getL1(), tokenizer.tokenize(record.getText())))
                .collect(Collectors.toList());

        return new CollectionObjectStream<>(samples);
    }

    /**
     * Creates L2 training sample stream for records within an L1 category
     */
    private ObjectStream<DocumentSample> createL2SampleStream(List<TaxonomyRecord> records) {
        List<DocumentSample> samples = records.stream()
                .map(record -> new DocumentSample(record.getL2(), tokenizer.tokenize(record.getText())))
                .collect(Collectors.toList());

        return new CollectionObjectStream<>(samples);
    }

    /**
     * Creates L3 training sample stream for records within an L1-L2 combination
     */
    private ObjectStream<DocumentSample> createL3SampleStream(List<TaxonomyRecord> records) {
        List<DocumentSample> samples = records.stream()
                .filter(record -> record.getL3() != null && !record.getL3().trim().isEmpty())
                .map(record -> new DocumentSample(record.getL3(), tokenizer.tokenize(record.getText())))
                .collect(Collectors.toList());

        return new CollectionObjectStream<>(samples);
    }

    /**
     * Saves a model to disk
     * 
     * @param model    the trained model
     * @param filePath path where to save the model
     * @throws IOException if model cannot be saved
     */
    public void saveModel(DoccatModel model, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            model.serialize(bos);
        }
        System.out.println("Model saved to: " + filePath);
    }

    /**
     * Loads a model from disk
     * 
     * @param filePath path to the model file
     * @return loaded model
     * @throws IOException if model cannot be loaded
     */
    public static DoccatModel loadModel(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
                BufferedInputStream bis = new BufferedInputStream(fis)) {
            return new DoccatModel(bis);
        }
    }

    /**
     * Validates that each L1-L2 combination has at least the minimum required
     * number of records
     * 
     * @param records                  the training records to validate
     * @param minRecordsPerCombination minimum number of records required per L1-L2
     *                                 combination
     * @throws IllegalArgumentException if any L1-L2 combination has insufficient
     *                                  records
     */
    private void validateL1L2Combinations(List<TaxonomyRecord> records, int minRecordsPerCombination) {
        // Group records by L1-L2 combination
        Map<String, Integer> combinationCounts = new HashMap<>();

        for (TaxonomyRecord record : records) {
            String combination = record.getL1() + "->" + record.getL2();
            combinationCounts.put(combination, combinationCounts.getOrDefault(combination, 0) + 1);
        }

        // Check for insufficient combinations
        List<String> insufficientCombinations = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : combinationCounts.entrySet()) {
            if (entry.getValue() < minRecordsPerCombination) {
                insufficientCombinations.add(entry.getKey() + " (" + entry.getValue() + " records)");
            }
        }

        if (!insufficientCombinations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Insufficient training data for L1-L2 combinations. ");
            errorMessage.append("Each combination requires at least ").append(minRecordsPerCombination)
                    .append(" records.\n");
            errorMessage.append("Problematic combinations:\n");
            for (String combination : insufficientCombinations) {
                errorMessage.append("  - ").append(combination).append("\n");
            }
            throw new IllegalArgumentException(errorMessage.toString());
        }

        System.out.println("Data validation passed: All " + combinationCounts.size() +
                " L1-L2 combinations have at least " + minRecordsPerCombination + " records");
    }
}
