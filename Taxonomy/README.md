# Hierarchical Taxonomy Text Classifier

A production-ready hierarchical text classifier using Apache OpenNLP, trained directly from CSV data. This project implements a multi-level taxonomy classification system that can handle L1, L2, and optionally L3+ classification levels.

## Features

- **Direct CSV Training**: No need to convert CSV files to `.train` files
- **Hierarchical Classification**: Supports L1 → L2 → L3+ taxonomy levels
- **Configurable Thresholds**: Set confidence thresholds for each classification level
- **Production Ready**: Well-structured, documented, and extensible code
- **JDK 8 Compatible**: Works with older Java versions

## Project Structure

```
src/main/java/com/taxonomy/
├── data/
│   └── TaxonomyRecord.java          # CSV data model and reader
├── training/
│   └── HierarchicalModelTrainer.java # Model training logic
└── classifier/
    ├── HierarchicalTaxonomyClassifier.java # Main classifier
    └── TaxonomyClassifierDemo.java   # Demo and example usage
```

## CSV Data Format

Your CSV file should have the following columns:

| Column | Description                 | Required |
| ------ | --------------------------- | -------- |
| `text` | Text content to classify    | Yes      |
| `L1`   | Top-level taxonomy class    | Yes      |
| `L2`   | Second-level taxonomy class | Yes      |
| `L3`   | Third-level taxonomy class  | Optional |
| `L4`   | Fourth-level taxonomy class | Optional |

### Example CSV Data

```csv
text,L1,L2
"Messi scored a hat-trick for Inter Miami",Sports,Soccer
"Apple announced a new M-series chip",Tech,Hardware
"The CDC issued updated flu vaccine guidance",Health,PublicHealth
"LeBron James breaks scoring record",Sports,Basketball
"Google releases new AI language model",Tech,AI
```

## Dependencies

The project uses Maven with the following key dependencies:

- **Apache OpenNLP 1.9.4**: Core NLP functionality
- **Apache Commons CSV 1.8**: CSV parsing
- **SLF4J 1.7.30**: Logging framework
- **JUnit 4.13.2**: Testing framework

## Quick Start

### 1. Build the Project

```bash
mvn clean compile
```

### 2. Run the Demo

```bash
mvn exec:java -Dexec.mainClass="com.taxonomy.classifier.TaxonomyClassifierDemo"
```

This will:

- Create sample training data (`data/sample_taxonomy.csv`)
- Train L1 and L2 models
- Save models to `models/` directory
- Test classification on sample texts

### 3. Use Your Own Data

Replace the sample data with your own CSV file:

```java
// In your code
String csvFilePath = "path/to/your/data.csv";
List<TaxonomyRecord> records = TaxonomyRecord.CSVReader.readCSV(csvFilePath);

// Train models
HierarchicalModelTrainer trainer = new HierarchicalModelTrainer();
HierarchicalModelTrainer.TrainingConfig config = new HierarchicalModelTrainer.TrainingConfig()
    .setIterations(100)
    .setCutoff(5);

DoccatModel l1Model = trainer.trainL1Model(records, config);
Map<String, DoccatModel> l2Models = trainer.trainL2Models(records, config);
```

## Usage Examples

### Training Models

```java
// Read CSV data
List<TaxonomyRecord> records = TaxonomyRecord.CSVReader.readCSV("data.csv");

// Configure training
HierarchicalModelTrainer trainer = new HierarchicalModelTrainer();
HierarchicalModelTrainer.TrainingConfig config = new HierarchicalModelTrainer.TrainingConfig()
    .setIterations(100)
    .setCutoff(5)
    .setAlgorithm("MAXENT");

// Train L1 model
DoccatModel l1Model = trainer.trainL1Model(records, config);
trainer.saveModel(l1Model, "models/L1.bin");

// Train L2 models (one per L1 category)
Map<String, DoccatModel> l2Models = trainer.trainL2Models(records, config);
for (Map.Entry<String, DoccatModel> entry : l2Models.entrySet()) {
    trainer.saveModel(entry.getValue(), "models/L2_" + entry.getKey() + ".bin");
}
```

### Classification

```java
// Load trained classifier
HierarchicalTaxonomyClassifier classifier = HierarchicalTaxonomyClassifier.loadFromModels(
    "models",
    0.6,  // L1 confidence threshold
    0.5   // L2 confidence threshold
);

// Classify new text
String text = "Messi scored a goal in the World Cup final";
HierarchicalTaxonomyClassifier.ClassificationResult result = classifier.classify(text);

System.out.println("Prediction: " + result.getFullPath());  // "Sports → Soccer"
System.out.println("L1 Confidence: " + result.getL1Confidence());
System.out.println("L2 Confidence: " + result.getL2Confidence());
```

### Extending to L3+ Levels

To add L3 support, simply include L3 columns in your CSV:

```csv
text,L1,L2,L3
"Messi wins Ballon d'Or award",Sports,Soccer,Awards
"Champions League final highlights",Sports,Soccer,Matches
```

Then train L3 models:

```java
Map<String, DoccatModel> l3Models = trainer.trainL3Models(records, config);
// Models are saved as "L3_Sports_Soccer.bin", etc.
```

## Model Files

After training, you'll have these model files:

- `L1.bin` - Top-level classifier (Sports, Tech, Health, etc.)
- `L2_Sports.bin` - Sports subcategory classifier (Soccer, Basketball, etc.)
- `L2_Tech.bin` - Tech subcategory classifier (AI, Hardware, etc.)
- `L2_Health.bin` - Health subcategory classifier (Nutrition, Medicine, etc.)
- `L3_Sports_Soccer.bin` - Optional L3 models

## Configuration Options

### Training Parameters

```java
HierarchicalModelTrainer.TrainingConfig config = new HierarchicalModelTrainer.TrainingConfig()
    .setIterations(100)        // Number of training iterations
    .setCutoff(5)              // Minimum feature frequency
    .setAlgorithm("MAXENT");   // Algorithm: MAXENT, PERCEPTRON, etc.
```

### Classification Thresholds

```java
HierarchicalTaxonomyClassifier classifier = HierarchicalTaxonomyClassifier.loadFromModels(
    "models",
    0.7,  // L1 threshold - higher = more conservative
    0.6,  // L2 threshold - adjust based on your needs
    0.5   // L3 threshold (optional)
);
```

## Evaluation Metrics

For production use, implement evaluation using held-out test data:

```java
// Split your data into train/test sets
List<TaxonomyRecord> trainData = records.subList(0, (int)(records.size() * 0.8));
List<TaxonomyRecord> testData = records.subList((int)(records.size() * 0.8), records.size());

// Train on train data, evaluate on test data
int correct = 0;
int total = 0;

for (TaxonomyRecord testRecord : testData) {
    ClassificationResult result = classifier.classify(testRecord.getText());

    if (result.isL1ThresholdMet() && result.getL1Category().equals(testRecord.getL1())) {
        if (result.isL2ThresholdMet() && result.getL2Category().equals(testRecord.getL2())) {
            correct++;
        }
    }
    total++;
}

double accuracy = (double) correct / total;
System.out.println("Hierarchical Accuracy: " + accuracy);
```

### Recommended Metrics

1. **Hierarchical Accuracy**: Correct prediction at all levels
2. **Level-wise Precision/Recall**: Separate metrics for L1, L2, L3
3. **Partial Credit**: Award partial points for correct higher levels
4. **Confidence Distribution Analysis**: Tune thresholds based on confidence histograms

## Production Considerations

1. **Data Quality**: Ensure consistent labeling across taxonomy levels
2. **Class Imbalance**: Monitor for imbalanced categories and use appropriate sampling
3. **Threshold Tuning**: Optimize confidence thresholds on validation data
4. **Model Updates**: Retrain periodically with new data
5. **Performance**: Consider caching loaded models for high-throughput scenarios

## Troubleshooting

### Common Issues

1. **"Insufficient training data"**: Ensure at least 10+ examples per category
2. **Low confidence scores**: Try lowering thresholds or adding more training data
3. **Memory issues**: Reduce training iterations or use smaller feature cutoff

### Performance Tips

- Use `SimpleTokenizer` for faster tokenization
- Cache loaded models in production
- Consider feature engineering for domain-specific texts
- Monitor model performance and retrain when accuracy drops

## License

This project is provided as-is for educational and commercial use.
