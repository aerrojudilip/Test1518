package com.taxonomy.classifier;

import com.taxonomy.training.HierarchicalModelTrainer;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Hierarchical taxonomy classifier that predicts both L1 and L2 categories
 */
public class HierarchicalTaxonomyClassifier {

    private static final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

    private final DocumentCategorizerME l1Classifier;
    private final Map<String, DocumentCategorizerME> l2Classifiers;
    private final Map<String, DocumentCategorizerME> l3Classifiers;

    private final double l1Threshold;
    private final double l2Threshold;
    private final double l3Threshold;

    /**
     * Classification result containing the hierarchical prediction
     */
    public static class ClassificationResult {
        private final String l1Category;
        private final String l2Category;
        private final String l3Category;
        private final double l1Confidence;
        private final double l2Confidence;
        private final double l3Confidence;
        private final boolean l1ThresholdMet;
        private final boolean l2ThresholdMet;
        private final boolean l3ThresholdMet;

        public ClassificationResult(String l1Category, String l2Category, String l3Category,
                double l1Confidence, double l2Confidence, double l3Confidence,
                boolean l1ThresholdMet, boolean l2ThresholdMet, boolean l3ThresholdMet) {
            this.l1Category = l1Category;
            this.l2Category = l2Category;
            this.l3Category = l3Category;
            this.l1Confidence = l1Confidence;
            this.l2Confidence = l2Confidence;
            this.l3Confidence = l3Confidence;
            this.l1ThresholdMet = l1ThresholdMet;
            this.l2ThresholdMet = l2ThresholdMet;
            this.l3ThresholdMet = l3ThresholdMet;
        }

        // Getters
        public String getL1Category() {
            return l1Category;
        }

        public String getL2Category() {
            return l2Category;
        }

        public String getL3Category() {
            return l3Category;
        }

        public double getL1Confidence() {
            return l1Confidence;
        }

        public double getL2Confidence() {
            return l2Confidence;
        }

        public double getL3Confidence() {
            return l3Confidence;
        }

        public boolean isL1ThresholdMet() {
            return l1ThresholdMet;
        }

        public boolean isL2ThresholdMet() {
            return l2ThresholdMet;
        }

        public boolean isL3ThresholdMet() {
            return l3ThresholdMet;
        }

        /**
         * Returns the full hierarchical path
         */
        public String getFullPath() {
            StringBuilder path = new StringBuilder();

            if (l1ThresholdMet && l1Category != null) {
                path.append(l1Category);

                if (l2ThresholdMet && l2Category != null) {
                    path.append(" -> ").append(l2Category);

                    if (l3ThresholdMet && l3Category != null) {
                        path.append(" -> ").append(l3Category);
                    }
                }
            }

            return path.toString();
        }

        @Override
        public String toString() {
            return String.format(
                    "ClassificationResult{path='%s', confidences=[L1:%.3f, L2:%.3f, L3:%.3f], thresholds=[L1:%b, L2:%b, L3:%b]}",
                    getFullPath(), l1Confidence, l2Confidence, l3Confidence, l1ThresholdMet, l2ThresholdMet,
                    l3ThresholdMet);
        }
    }

    /**
     * Constructor
     * 
     * @param l1Model     the L1 classification model
     * @param l2Models    map of L1 categories to their L2 models
     * @param l3Models    map of L1_L2 keys to their L3 models (optional)
     * @param l1Threshold confidence threshold for L1 classification
     * @param l2Threshold confidence threshold for L2 classification
     * @param l3Threshold confidence threshold for L3 classification
     */
    public HierarchicalTaxonomyClassifier(DoccatModel l1Model,
            Map<String, DoccatModel> l2Models,
            Map<String, DoccatModel> l3Models,
            double l1Threshold,
            double l2Threshold,
            double l3Threshold) {
        this.l1Classifier = new DocumentCategorizerME(l1Model);
        this.l2Classifiers = new HashMap<>();
        this.l3Classifiers = new HashMap<>();

        // Initialize L2 classifiers
        for (Map.Entry<String, DoccatModel> entry : l2Models.entrySet()) {
            this.l2Classifiers.put(entry.getKey(), new DocumentCategorizerME(entry.getValue()));
        }

        // Initialize L3 classifiers if provided
        if (l3Models != null) {
            for (Map.Entry<String, DoccatModel> entry : l3Models.entrySet()) {
                this.l3Classifiers.put(entry.getKey(), new DocumentCategorizerME(entry.getValue()));
            }
        }

        this.l1Threshold = l1Threshold;
        this.l2Threshold = l2Threshold;
        this.l3Threshold = l3Threshold;
    }

    /**
     * Constructor without L3 models
     */
    public HierarchicalTaxonomyClassifier(DoccatModel l1Model,
            Map<String, DoccatModel> l2Models,
            double l1Threshold,
            double l2Threshold) {
        this(l1Model, l2Models, new HashMap<>(), l1Threshold, l2Threshold, 0.5);
    }

    /**
     * Classifies text hierarchically
     * 
     * @param text the text to classify
     * @return classification result with hierarchical prediction
     */
    public ClassificationResult classify(String text) {
        String[] tokens = tokenizer.tokenize(text);

        // L1 Classification
        double[] l1Scores = l1Classifier.categorize(tokens);
        String l1Category = l1Classifier.getBestCategory(l1Scores);
        double l1Confidence = getMaxScore(l1Scores);
        boolean l1ThresholdMet = l1Confidence >= l1Threshold;

        String l2Category = null;
        double l2Confidence = 0.0;
        boolean l2ThresholdMet = false;

        String l3Category = null;
        double l3Confidence = 0.0;
        boolean l3ThresholdMet = false;

        // L2 Classification (only if L1 threshold is met)
        if (l1ThresholdMet && l2Classifiers.containsKey(l1Category)) {
            DocumentCategorizerME l2Classifier = l2Classifiers.get(l1Category);
            double[] l2Scores = l2Classifier.categorize(tokens);
            l2Category = l2Classifier.getBestCategory(l2Scores);
            l2Confidence = getMaxScore(l2Scores);
            l2ThresholdMet = l2Confidence >= l2Threshold;

            // L3 Classification (only if L2 threshold is met)
            if (l2ThresholdMet) {
                String l1L2Key = l1Category + "_" + l2Category;
                if (l3Classifiers.containsKey(l1L2Key)) {
                    DocumentCategorizerME l3Classifier = l3Classifiers.get(l1L2Key);
                    double[] l3Scores = l3Classifier.categorize(tokens);
                    l3Category = l3Classifier.getBestCategory(l3Scores);
                    l3Confidence = getMaxScore(l3Scores);
                    l3ThresholdMet = l3Confidence >= l3Threshold;
                }
            }
        }

        return new ClassificationResult(l1Category, l2Category, l3Category,
                l1Confidence, l2Confidence, l3Confidence,
                l1ThresholdMet, l2ThresholdMet, l3ThresholdMet);
    }

    /**
     * Classifies text and returns all predictions with confidence scores for each
     * level
     * 
     * @param text the text to classify
     * @return detailed classification results showing all predictions at each level
     */
    public DetailedClassificationResult classifyDetailed(String text) {
        String[] tokens = tokenizer.tokenize(text);

        // L1 Classification - get all categories with scores
        double[] l1Scores = l1Classifier.categorize(tokens);

        Map<String, Double> l1Results = new HashMap<>();
        // Get all categories from the model
        for (int i = 0; i < l1Scores.length; i++) {
            String category = l1Classifier.getCategory(i);
            l1Results.put(category, l1Scores[i]);
        }

        // L2 Classification - for all L1 categories that have L2 models
        Map<String, Map<String, Double>> l2Results = new HashMap<>();
        for (Map.Entry<String, Double> l1Entry : l1Results.entrySet()) {
            String l1Cat = l1Entry.getKey();

            // Generate L2 predictions for all L1 categories that have L2 models,
            // regardless of L1 threshold
            if (l2Classifiers.containsKey(l1Cat)) {
                DocumentCategorizerME l2Classifier = l2Classifiers.get(l1Cat);
                double[] l2Scores = l2Classifier.categorize(tokens);

                Map<String, Double> l2CatResults = new HashMap<>();
                for (int i = 0; i < l2Scores.length; i++) {
                    String category = l2Classifier.getCategory(i);
                    l2CatResults.put(category, l2Scores[i]);
                }
                l2Results.put(l1Cat, l2CatResults);
            }
        }

        return new DetailedClassificationResult(l1Results, l2Results, l1Threshold, l2Threshold);
    }

    /**
     * Detailed classification result showing all predictions at each level
     */
    public static class DetailedClassificationResult {
        private final Map<String, Double> l1Results;
        private final Map<String, Map<String, Double>> l2Results;
        private final double l1Threshold;
        private final double l2Threshold;

        public DetailedClassificationResult(Map<String, Double> l1Results,
                Map<String, Map<String, Double>> l2Results,
                double l1Threshold, double l2Threshold) {
            this.l1Results = l1Results;
            this.l2Results = l2Results;
            this.l1Threshold = l1Threshold;
            this.l2Threshold = l2Threshold;
        }

        public Map<String, Double> getL1Results() {
            return l1Results;
        }

        public Map<String, Map<String, Double>> getL2Results() {
            return l2Results;
        }

        public double getL1Threshold() {
            return l1Threshold;
        }

        public double getL2Threshold() {
            return l2Threshold;
        }
    }

    /**
     * Helper method to get the maximum score from the scores array
     */
    private double getMaxScore(double[] scores) {
        double max = 0.0;
        for (double score : scores) {
            if (score > max) {
                max = score;
            }
        }
        return max;
    }

    /**
     * Factory method to load classifier from saved models
     * 
     * @param modelsDir   directory containing the model files
     * @param l1Threshold confidence threshold for L1
     * @param l2Threshold confidence threshold for L2
     * @param l3Threshold confidence threshold for L3
     * @return loaded classifier
     * @throws IOException if models cannot be loaded
     */
    public static HierarchicalTaxonomyClassifier loadFromModels(String modelsDir,
            double l1Threshold,
            double l2Threshold,
            double l3Threshold) throws IOException {
        // Load L1 model
        DoccatModel l1Model = HierarchicalModelTrainer.loadModel(modelsDir + "/L1.bin");

        // Load L2 models
        Map<String, DoccatModel> l2Models = new HashMap<>();
        java.io.File dir = new java.io.File(modelsDir);
        if (dir.exists() && dir.isDirectory()) {
            for (java.io.File file : dir.listFiles()) {
                if (file.getName().startsWith("L2_") && file.getName().endsWith(".bin")) {
                    String l1Category = file.getName().substring(3, file.getName().length() - 4);
                    DoccatModel l2Model = HierarchicalModelTrainer.loadModel(file.getAbsolutePath());
                    l2Models.put(l1Category, l2Model);
                }
            }
        }

        // Load L3 models
        Map<String, DoccatModel> l3Models = new HashMap<>();
        if (dir.exists() && dir.isDirectory()) {
            for (java.io.File file : dir.listFiles()) {
                if (file.getName().startsWith("L3_") && file.getName().endsWith(".bin")) {
                    String l1L2Key = file.getName().substring(3, file.getName().length() - 4);
                    DoccatModel l3Model = HierarchicalModelTrainer.loadModel(file.getAbsolutePath());
                    l3Models.put(l1L2Key, l3Model);
                }
            }
        }

        return new HierarchicalTaxonomyClassifier(l1Model, l2Models, l3Models, l1Threshold, l2Threshold, l3Threshold);
    }

    /**
     * Factory method to load classifier from saved models (without L3)
     */
    public static HierarchicalTaxonomyClassifier loadFromModels(String modelsDir,
            double l1Threshold,
            double l2Threshold) throws IOException {
        return loadFromModels(modelsDir, l1Threshold, l2Threshold, 0.5);
    }
}
