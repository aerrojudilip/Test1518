# Hierarchical Taxonomy Text Classifier

This project implements a hierarchical text classifier using Apache OpenNLP that:

✅ **Completed Project Setup:**

- Created Maven Java project structure
- Implemented hierarchical taxonomy classifier with OpenNLP
- Added complete documentation and examples
- Successfully compiled and tested the system

## Key Features:

- **Direct CSV Training**: Reads training data directly from CSV files
- **Hierarchical Classification**: Supports L1 → L2 → L3+ taxonomy levels
- **Configurable Thresholds**: Set confidence thresholds for each classification level
- **Production Ready**: Well-structured, documented, and extensible code
- **JDK 8 Compatible**: Works with older Java versions

## Quick Start:

1. Run `mvn compile` to build the project
2. Run `mvn exec:java -Dexec.mainClass=com.taxonomy.classifier.TaxonomyClassifierDemo` to see the demo
3. Check the generated `models/` directory for trained models
4. Review the `data/sample_taxonomy.csv` file for data format examples

## Project Structure:

- `src/main/java/com/taxonomy/data/` - CSV data models and readers
- `src/main/java/com/taxonomy/training/` - Model training logic
- `src/main/java/com/taxonomy/classifier/` - Main classifier and demo
- `src/main/java/com/taxonomy/evaluation/` - Evaluation metrics
- `README.md` - Complete documentation and usage examples
