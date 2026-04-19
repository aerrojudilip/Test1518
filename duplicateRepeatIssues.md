You are a senior full-stack architect. Build a complete production-grade 
application for detecting duplicate and repeat issues in IBM OpenPages GRC 
using Spring AI with Google Gemini, an embedded Java vector database, and 
a React frontend styled with IBM Carbon Design System.

═══════════════════════════════════════════════════════════════
PROJECT: OpenPages Duplicate Issue Detector
═══════════════════════════════════════════════════════════════

TECH STACK
──────────
Backend:
  • Java 21, Spring Boot 3.3.x (standalone, embedded Tomcat - JAR packaging)
  • Spring AI 1.0.x with Vertex AI Gemini starter
  • Spring AI SimpleVectorStore (embedded, file-persisted, no server)
  • Spring WebFlux (async OpenPages REST calls)
  • Apache Commons CSV (CSV read/write for local snapshot)
  • Apache Commons Text (Jaro-Winkler, Levenshtein)
  • Lombok, MapStruct
  • Caffeine (shingle signature cache)
  • Spring Boot Actuator (health, metrics)
  • Maven build producing executable fat JAR

Vector Database (NO EXTERNAL SERVER, NO DOCKER):
  • Spring AI SimpleVectorStore — default free in-JVM vector store
    - Pure Java, runs inside the Spring Boot JVM
    - Persists to local JSON file (./data/vector-store.json)
    - Auto-loads on startup, auto-saves on shutdown
    - Cosine similarity built-in
    - Metadata filtering via Spring AI filter DSL
    - Handles 10,000+ vectors comfortably (~70MB heap for 768-dim)
  • Zero external dependencies, zero network calls, zero Docker

Frontend:
  • React 18 + Vite
  • @carbon/react (IBM Carbon Design System v11)
  • @carbon/icons-react
  • @carbon/charts-react
  • Carbon themes: White + G100 toggle
  • IBM Plex Sans / IBM Plex Mono (Carbon default)
  • TanStack Query for API state
  • Axios for REST calls

LLM:
  • Google Vertex AI Gemini
    - gemini-1.5-flash (LLM judgment)
    - text-embedding-004 (semantic embeddings, 768 dimensions)

Deployment:
  • Single Spring Boot executable JAR
  • React bundled into Spring Boot /static at build time
  • Vector store persisted to ./data/vector-store.json on disk
  • Run: `java -jar openpages-duplicate-detector.jar`
  • NO Docker, NO external services, NO app server

═══════════════════════════════════════════════════════════════
FUNCTIONAL REQUIREMENTS
═══════════════════════════════════════════════════════════════

Detect in IBM OpenPages issues:
  • DUPLICATE = same control failure, same entity/period
  • REPEAT    = same control weakness recurring in new period/entity
  • SIMILAR   = related theme but distinct finding
  • DIFFERENT = unrelated

Issues may describe the SAME problem with completely different wording.
Must scale to 10,000+ issues with sub-3-second response time.

═══════════════════════════════════════════════════════════════
ARCHITECTURE: 4-STAGE DETECTION FUNNEL
═══════════════════════════════════════════════════════════════

                    ┌──────────────────────────┐
                    │  SimpleVectorStore       │
                    │  (embedded, in-memory    │
                    │   + disk persistence)    │
                    │  ./data/vector-store.json│
                    └──────────▲───────────────┘
                               │
Incoming Issue                 │
    │                          │
    ▼                          │
STAGE 1 — Metadata Pre-filter (Spring AI filter expression)
    - FilterExpressionBuilder:
      resourceType == X AND riskCategory == Y
      AND status IN ['Open','In Progress','Draft']
    - SimpleVectorStore applies filter during search
    - 10,000 → effectively narrowed during vector search
    │
    ▼
STAGE 2 — Vector Similarity Search
    - Embed incoming issue via text-embedding-004
    - vectorStore.similaritySearch(
        SearchRequest.query(text)
          .withTopK(50)
          .withSimilarityThreshold(0.70)
          .withFilterExpression(...))
    - Returns top 50 most similar documents
    │
    ▼
STAGE 3 — Jaccard Shingle Re-rank
    - Pre-computed 3-gram shingles cached in Caffeine
    - Composite score = 0.7 * vectorScore + 0.3 * jaccardScore
    - ~50 → top 20
    │
    ▼
STAGE 4 — Gemini LLM Judge
    - gemini-1.5-flash with strict JSON output
    - Parallel calls with Semaphore(5)
    - Confidence threshold 0.80
    - top 20 → final ranked verdicts

═══════════════════════════════════════════════════════════════
SIMPLEVECTORSTORE CONFIGURATION
═══════════════════════════════════════════════════════════════

1) Maven pom.xml — NO external vector DB dependency needed:
   <dependency>
     <groupId>org.springframework.ai</groupId>
     <artifactId>spring-ai-core</artifactId>
   </dependency>
   <!-- SimpleVectorStore is bundled in spring-ai-core -->

2) VectorStoreConfig.java MUST:
   @Bean
   public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                    @Value("${vectorstore.path}") String path) {
     SimpleVectorStore store = new SimpleVectorStore(embeddingModel);
     File file = new File(path);
     if (file.exists()) store.load(file);
     return store;
   }
   
   @PreDestroy lifecycle MUST save store to disk:
     store.save(new File(path));
   
   @Scheduled(fixedRate = 600_000) MUST checkpoint every 10 min:
     store.save(new File(path));

3) application.yml:
   vectorstore:
     path: ./data/vector-store.json
     checkpoint-interval-ms: 600000

4) Document structure stored:
   Document {
     id:       <openpages_object_id>
     content:  <name + description + rootCause + recommendation>
     metadata: {
       objectId, name, status, riskCategory, resourceType,
       primaryFilter, owner, issueType, regulatoryDomain,
       period, createdDate, modifiedDate
     }
     embedding: [768 floats from text-embedding-004]
   }

5) Filter expressions use Spring AI FilterExpressionBuilder:
   var b = new FilterExpressionBuilder();
   Expression filter = b.and(
     b.eq("resourceType",  incoming.getResourceType()),
     b.eq("riskCategory",  incoming.getRiskCategory()),
     b.in("status",        "Open", "In Progress", "Draft")
   ).build();

═══════════════════════════════════════════════════════════════
BACKEND PACKAGE STRUCTURE
═══════════════════════════════════════════════════════════════

com.enterprise.openpages.duplicate/
├── config/
│   ├── GeminiConfig.java              // Vertex AI beans
│   ├── VectorStoreConfig.java         // SimpleVectorStore bean + persistence
│   ├── WebClientConfig.java           // OpenPages REST client
│   ├── AsyncConfig.java               // parallel judge executor
│   └── WebMvcConfig.java              // serves React SPA
├── model/
│   ├── IssueFingerprint.java
│   ├── DuplicateJudgment.java
│   ├── DuplicateResult.java
│   ├── DetectionReport.java
│   └── StageStats.java
├── openpages/
│   ├── OpenPagesRestClient.java
│   ├── IssuesCsvExtractor.java        // @Scheduled nightly
│   ├── IssuesCsvRepository.java       // local CSV snapshot
│   └── DeltaRefreshService.java       // @Scheduled 5-min
├── vectorstore/
│   ├── VectorIndexService.java        // index/upsert/delete
│   ├── VectorSearchService.java       // metadata-filtered search
│   ├── VectorStorePersistence.java    // save/load/checkpoint
│   └── DocumentMapper.java            // IssueFingerprint ↔ Document
├── detection/
│   ├── ShingleCacheService.java       // Caffeine cache
│   ├── JaccardReranker.java           // Stage 3
│   ├── GeminiDuplicateJudge.java      // Stage 4
│   ├── BatchJudgeService.java         // parallel execution
│   └── DuplicateDetectionOrchestrator.java  // stages 1-4
├── prompt/
│   └── DuplicateJudgePromptBuilder.java
├── api/
│   ├── DetectionController.java       // POST /api/detect
│   ├── IndexController.java           // POST /api/index/rebuild
│   └── StatsController.java           // GET /api/stats
└── OpenPagesDuplicateApplication.java

═══════════════════════════════════════════════════════════════
KEY BACKEND IMPLEMENTATION DETAILS
═══════════════════════════════════════════════════════════════

1) VectorIndexService MUST provide:
   - void indexIssue(IssueFingerprint)       → add/update single doc
   - void indexBatch(List<IssueFingerprint>) → batch add (50 at a time
                                                to respect Gemini embedding
                                                rate limits)
   - void deleteIssue(String objectId)
   - void rebuildIndex()                     → clear + reindex all from CSV
   - long countIndexed()
   - Implementation note: SimpleVectorStore.add(List<Document>) 
     triggers embedding calls automatically

2) VectorSearchService MUST provide:
   List<Document> search(
       String queryText,
       IssueFingerprint incoming,
       int topK,
       double threshold)
   - Builds FilterExpression using FilterExpressionBuilder
   - Uses SearchRequest.query(text)
        .withTopK(topK)
        .withSimilarityThreshold(threshold)
        .withFilterExpression(filterExpr)

3) VectorStorePersistence MUST:
   - @PostConstruct: load from ./data/vector-store.json if exists
   - @Scheduled(fixedRate = 600_000): checkpoint save every 10 min
   - @PreDestroy: final save on shutdown
   - Register JVM shutdown hook as backup

4) DuplicateDetectionOrchestrator flow: