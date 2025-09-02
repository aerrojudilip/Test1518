package com.taxonomy.classifier;

import com.taxonomy.data.TaxonomyRecord;
import com.taxonomy.training.HierarchicalModelTrainer;
import opennlp.tools.doccat.DoccatModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Demo class that shows how to train and use the hierarchical taxonomy
 * classifier
 */
public class TaxonomyClassifierDemo {

    public static void main(String[] args) {
        try {
            String csvFilePath;
            String userText = null;
            String modelsDir = "models";

            // Check if user provided custom CSV file path
            if (args.length > 0 && args[0].endsWith(".csv")) {
                csvFilePath = args[0];
                System.out.println("Using custom CSV file: " + csvFilePath);

                // Check if user also provided text to classify
                if (args.length > 1) {
                    userText = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                }
            } else {
                // Use default sample CSV
                csvFilePath = "data/sample_taxonomy.csv";
                createSampleCSVData(csvFilePath);

                // Check if user provided text to classify
                if (args.length > 0) {
                    userText = String.join(" ", args);
                }
            } // Train the models
            trainModels(csvFilePath, modelsDir);

            // Classify text or run default tests
            if (userText != null) {
                classifyUserText(userText, modelsDir);
            } else {
                testClassifier(modelsDir);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Trains all hierarchical models from CSV data
     */
    private static void trainModels(String csvFilePath, String modelsDir) throws IOException {
        System.out.println("=== Training Hierarchical Models ===");

        // Create models directory
        java.io.File dir = new java.io.File(modelsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Read training data
        List<TaxonomyRecord> records = TaxonomyRecord.CSVReader.readCSV(csvFilePath);
        System.out.println("Loaded " + records.size() + " training records");

        // Initialize trainer and config
        HierarchicalModelTrainer trainer = new HierarchicalModelTrainer();
        HierarchicalModelTrainer.TrainingConfig config = new HierarchicalModelTrainer.TrainingConfig()
                .setIterations(100)
                .setCutoff(1) // Lower cutoff for small datasets
                .setAlgorithm("MAXENT");

        // Train L1 model
        DoccatModel l1Model = trainer.trainL1Model(records, config);
        trainer.saveModel(l1Model, modelsDir + "/L1.bin");

        // Train L2 models
        Map<String, DoccatModel> l2Models = trainer.trainL2Models(records, config);
        for (Map.Entry<String, DoccatModel> entry : l2Models.entrySet()) {
            String l1Category = entry.getKey();
            DoccatModel l2Model = entry.getValue();
            trainer.saveModel(l2Model, modelsDir + "/L2_" + l1Category + ".bin");
        }

        // Train L3 models (if L3 data exists)
        Map<String, DoccatModel> l3Models = trainer.trainL3Models(records, config);
        for (Map.Entry<String, DoccatModel> entry : l3Models.entrySet()) {
            String l1L2Key = entry.getKey();
            DoccatModel l3Model = entry.getValue();
            trainer.saveModel(l3Model, modelsDir + "/L3_" + l1L2Key + ".bin");
        }

        System.out.println("All models trained and saved to: " + modelsDir);
    }

    /**
     * Tests the trained classifier with sample texts
     */
    private static void testClassifier(String modelsDir) throws IOException {
        System.out.println("\n=== Testing Hierarchical Classifier ===");

        // Load the classifier with configurable thresholds
        HierarchicalTaxonomyClassifier classifier = HierarchicalTaxonomyClassifier.loadFromModels(
                modelsDir,
                0.6, // L1 threshold
                0.5 // L2 threshold
        );

        // Test texts
        String[] testTexts = {
                "Messi scored a hat-trick in the Champions League final",
                "Apple unveiled new MacBook Pro with M3 chip technology",
                "WHO releases new guidelines for COVID-19 vaccination protocols",
                "The stock market showed volatile trading patterns today"
        };

        System.out.println("Classification Results:");
        System.out.println("======================");

        for (String text : testTexts) {
            HierarchicalTaxonomyClassifier.ClassificationResult result = classifier.classify(text);

            System.out.println("\nText: \"" + text + "\"");
            System.out.println("Prediction: " + result.getFullPath());
            System.out.println("Details: " + result);
        }
    }

    /**
     * Classifies a user-provided text string with detailed multi-level results
     */
    private static void classifyUserText(String userText, String modelsDir) throws IOException {
        System.out.println("\n=== Classifying User Text ===");

        // Load the classifier with configurable thresholds
        HierarchicalTaxonomyClassifier classifier = HierarchicalTaxonomyClassifier.loadFromModels(
                modelsDir,
                0.6, // L1 threshold
                0.5 // L2 threshold
        );

        System.out.println("Input Text: \"" + userText + "\"");
        System.out.println("==================================================");

        // Get detailed classification showing all levels with confidence scores
        HierarchicalTaxonomyClassifier.DetailedClassificationResult detailedResult = classifier
                .classifyDetailed(userText);

        System.out.println("=== Hierarchical Predictions (L1 → L2) ===");

        // Create a list of L1→L2 pairs with their combined information
        List<String> predictions = new ArrayList<>();

        for (Map.Entry<String, Double> l1Entry : detailedResult.getL1Results().entrySet()) {
            String l1Category = l1Entry.getKey();
            double l1Confidence = l1Entry.getValue();
            boolean l1ThresholdMet = l1Confidence >= detailedResult.getL1Threshold();

            // Get the best L2 prediction for this L1 category (if any)
            Map<String, Double> l2Results = detailedResult.getL2Results().get(l1Category);
            if (l2Results != null && !l2Results.isEmpty()) {
                // Find the best L2 category
                String bestL2 = l2Results.entrySet().stream()
                        .max((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()))
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
                double l2Confidence = l2Results.get(bestL2);
                boolean l2ThresholdMet = l2Confidence >= detailedResult.getL2Threshold();

                String prediction = String.format("  %s → %s: L1=%.3f %s, L2=%.3f %s",
                        l1Category, bestL2,
                        l1Confidence, l1ThresholdMet ? "✓" : "✗",
                        l2Confidence, l2ThresholdMet ? "✓" : "✗");
                predictions.add(prediction);
            } else {
                // Only show L1 when no L2 predictions are available
                String prediction = String.format("  %s: %.3f %s",
                        l1Category, l1Confidence,
                        l1ThresholdMet ? "✓" : "✗");
                predictions.add(prediction);
            }
        }

        // Sort predictions by L1 confidence (descending)
        predictions.stream()
                .sorted((p1, p2) -> {
                    // Extract L1 confidence for sorting
                    String conf1 = p1.substring(p1.indexOf("L1=") + 3, p1.indexOf(" ", p1.indexOf("L1=") + 3));
                    String conf2 = p2.substring(p2.indexOf("L1=") + 3, p2.indexOf(" ", p2.indexOf("L1=") + 3));
                    try {
                        return Double.compare(Double.parseDouble(conf2), Double.parseDouble(conf1));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .forEach(System.out::println);

        // Also show the standard hierarchical result
        HierarchicalTaxonomyClassifier.ClassificationResult standardResult = classifier.classify(userText);
        System.out.println("\n=== Final Hierarchical Prediction ===");
        String prediction = standardResult.getFullPath().isEmpty() ? "No confident prediction"
                : standardResult.getFullPath();
        System.out.println("Best Path: " + prediction);
        System.out.println("Full Details: " + standardResult);
    }

    /**
     * Creates sample CSV data for demonstration
     */
    private static void createSampleCSVData(String csvFilePath) throws IOException {
        java.io.File file = new java.io.File(csvFilePath);
        if (file.exists()) {
            System.out.println("Sample CSV already exists: " + csvFilePath);
            return;
        }

        // Create parent directory
        file.getParentFile().mkdirs();

        String csvContent = "text,L1,L2\n" +
                "\"Messi scored a hat-trick for Inter Miami\",Sports,Soccer\n" +
                "\"Apple announced a new M-series chip\",Tech,Hardware\n" +
                "\"The CDC issued updated flu vaccine guidance\",Health,PublicHealth\n" +
                "\"LeBron James breaks scoring record in Lakers victory\",Sports,Basketball\n" +
                "\"Google releases new AI language model\",Tech,AI\n" +
                "\"New study shows benefits of Mediterranean diet\",Health,Nutrition\n" +
                "\"Champions League final draws record viewership\",Sports,Soccer\n" +
                "\"Microsoft Azure launches new cloud services\",Tech,Cloud\n" +
                "\"FDA approves new diabetes medication\",Health,Medicine\n" +
                "\"Tennis Grand Slam tournament begins next week\",Sports,Tennis\n" +
                "\"OpenAI ChatGPT usage surpasses 100 million users\",Tech,AI\n" +
                "\"Regular exercise reduces heart disease risk\",Health,Fitness\n" +
                "\"NBA playoff schedule announced for upcoming season\",Sports,Basketball\n" +
                "\"Intel releases new processor architecture\",Tech,Hardware\n" +
                "\"Mental health awareness week promotes wellness\",Health,MentalHealth\n" +
                "\"World Cup qualifying matches begin in Europe\",Sports,Soccer\n" +
                "\"Amazon Web Services expands data center capacity\",Tech,Cloud\n" +
                "\"Vitamin D deficiency linked to immune system problems\",Health,Nutrition\n" +
                "\"Olympic swimming records broken at world championships\",Sports,Swimming\n" +
                "\"Machine learning advances in natural language processing\",Tech,AI\n" +
                "\"Ronaldo scores winning goal in Champions League\",Sports,Soccer\n" +
                "\"New smartphone processor beats benchmarks\",Tech,Hardware\n" +
                "\"Yoga and meditation improve mental wellbeing\",Health,MentalHealth\n" +
                "\"Baseball World Series tickets go on sale\",Sports,Baseball\n" +
                "\"Artificial intelligence revolutionizes healthcare\",Tech,AI\n" +
                "\"Mediterranean diet linked to longer life expectancy\",Health,Nutrition\n" +
                "\"Premier League season starts with exciting matches\",Sports,Soccer\n" +
                "\"Cloud computing market shows strong growth\",Tech,Cloud\n" +
                "\"New cancer treatment shows promising results\",Health,Medicine\n" +
                "\"Formula One racing championship begins\",Sports,Racing\n";

        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(csvContent);
        }

        System.out.println("Sample CSV created: " + csvFilePath);
    }
}
