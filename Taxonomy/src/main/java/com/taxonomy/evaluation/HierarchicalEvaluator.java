package com.taxonomy.evaluation;

import com.taxonomy.classifier.HierarchicalTaxonomyClassifier;
import com.taxonomy.data.TaxonomyRecord;

import java.util.List;

/**
 * Evaluation metrics for hierarchical taxonomy classification
 */
public class HierarchicalEvaluator {

    /**
     * Evaluation metrics container
     */
    public static class EvaluationResult {
        private final int totalSamples;
        private final int l1Correct;
        private final int l2Correct;
        private final int hierarchicalCorrect;
        private final double l1Accuracy;
        private final double l2Accuracy;
        private final double hierarchicalAccuracy;

        public EvaluationResult(int totalSamples, int l1Correct, int l2Correct, int hierarchicalCorrect) {
            this.totalSamples = totalSamples;
            this.l1Correct = l1Correct;
            this.l2Correct = l2Correct;
            this.hierarchicalCorrect = hierarchicalCorrect;
            this.l1Accuracy = (double) l1Correct / totalSamples;
            this.l2Accuracy = (double) l2Correct / totalSamples;
            this.hierarchicalAccuracy = (double) hierarchicalCorrect / totalSamples;
        }

        // Getters
        public int getTotalSamples() {
            return totalSamples;
        }

        public int getL1Correct() {
            return l1Correct;
        }

        public int getL2Correct() {
            return l2Correct;
        }

        public int getHierarchicalCorrect() {
            return hierarchicalCorrect;
        }

        public double getL1Accuracy() {
            return l1Accuracy;
        }

        public double getL2Accuracy() {
            return l2Accuracy;
        }

        public double getHierarchicalAccuracy() {
            return hierarchicalAccuracy;
        }

        @Override
        public String toString() {
            return String.format(
                    "EvaluationResult{\n" +
                            "  Total Samples: %d\n" +
                            "  L1 Accuracy: %.3f (%d/%d correct)\n" +
                            "  L2 Accuracy: %.3f (%d/%d correct)\n" +
                            "  Hierarchical Accuracy: %.3f (%d/%d correct)\n" +
                            "}",
                    totalSamples,
                    l1Accuracy, l1Correct, totalSamples,
                    l2Accuracy, l2Correct, totalSamples,
                    hierarchicalAccuracy, hierarchicalCorrect, totalSamples);
        }
    }

    /**
     * Evaluates classifier performance on test data
     * 
     * @param classifier the trained classifier
     * @param testData   test records
     * @return evaluation results
     */
    public static EvaluationResult evaluate(HierarchicalTaxonomyClassifier classifier,
            List<TaxonomyRecord> testData) {
        int totalSamples = testData.size();
        int l1Correct = 0;
        int l2Correct = 0;
        int hierarchicalCorrect = 0;

        for (TaxonomyRecord testRecord : testData) {
            HierarchicalTaxonomyClassifier.ClassificationResult result = classifier.classify(testRecord.getText());

            // Check L1 accuracy
            if (result.isL1ThresholdMet() &&
                    result.getL1Category().equals(testRecord.getL1())) {
                l1Correct++;

                // Check L2 accuracy (only if L1 is correct)
                if (result.isL2ThresholdMet() &&
                        result.getL2Category().equals(testRecord.getL2())) {
                    l2Correct++;
                    hierarchicalCorrect++; // Both L1 and L2 correct
                }
            }
        }

        return new EvaluationResult(totalSamples, l1Correct, l2Correct, hierarchicalCorrect);
    }

    /**
     * Splits data into train/test sets
     * 
     * @param data       full dataset
     * @param trainRatio percentage of data to use for training (0.0 to 1.0)
     * @return array with [trainData, testData]
     */
    public static List<TaxonomyRecord>[] splitData(List<TaxonomyRecord> data, double trainRatio) {
        int trainSize = (int) (data.size() * trainRatio);

        @SuppressWarnings("unchecked")
        List<TaxonomyRecord>[] result = new List[2];
        result[0] = data.subList(0, trainSize); // Train data
        result[1] = data.subList(trainSize, data.size()); // Test data

        return result;
    }

    /**
     * Example usage demonstrating evaluation with train/test split
     */
    public static void demonstrateEvaluation(List<TaxonomyRecord> allData,
            HierarchicalTaxonomyClassifier classifier) {
        // Split data 80/20
        List<TaxonomyRecord>[] splitData = splitData(allData, 0.8);
        List<TaxonomyRecord> testData = splitData[1];

        if (testData.size() == 0) {
            System.out.println("No test data available for evaluation");
            return;
        }

        System.out.println("\n=== Model Evaluation ===");
        System.out.println("Test set size: " + testData.size());

        EvaluationResult evaluation = evaluate(classifier, testData);
        System.out.println(evaluation);

        // Show per-sample predictions for small test sets
        if (testData.size() <= 10) {
            System.out.println("\nDetailed Test Results:");
            for (int i = 0; i < testData.size(); i++) {
                TaxonomyRecord record = testData.get(i);
                HierarchicalTaxonomyClassifier.ClassificationResult result = classifier.classify(record.getText());

                String expected = record.getL1() + " -> " + record.getL2();
                String predicted = result.getFullPath();
                boolean correct = result.isL1ThresholdMet() && result.isL2ThresholdMet() &&
                        result.getL1Category().equals(record.getL1()) &&
                        result.getL2Category().equals(record.getL2());

                System.out.printf("Sample %d: %s\n", i + 1, correct ? "✓ CORRECT" : "✗ INCORRECT");
                System.out.printf("  Text: \"%s\"\n", record.getText());
                System.out.printf("  Expected: %s\n", expected);
                System.out.printf("  Predicted: %s\n", predicted);
                System.out.printf("  Confidences: L1=%.3f, L2=%.3f\n\n",
                        result.getL1Confidence(), result.getL2Confidence());
            }
        }
    }
}
