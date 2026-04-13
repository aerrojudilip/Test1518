# SpringRAG — Complete Project Prompt

> Copy everything below this line into a new AI conversation to recreate or extend this project from scratch.

---

A Spring Boot application implementing **Retrieval-Augmented Generation (RAG)** with multi-turn chat history, context-aware query rewriting, and incremental document sync.

| Component       | Technology                                                                          |
| --------------- | ----------------------------------------------------------------------------------- |
| Framework       | Spring Boot **3.5.6** + Spring AI **1.0.1**                                         |
| LLM (chat)      | OpenAI `gpt-4o`                                                                     |
| Embeddings      | OpenAI `text-embedding-3-small`                                                     |
| Vector store    | `ChromaDB` (Local Python Native Server via `spring-ai-starter-vector-store-chroma`) |
| Document reader | Apache Tika (PDF, DOCX, TXT, and more)                                              |
| UI              | Built-in single-page app at `http://localhost:7070`                                 |

---

## Features

- 📄 **Document ingestion** — drop PDF / DOCX / TXT files into `documents/`, ingest once, query forever. Execution is fully **Asynchronous**.
- 🔄 **Incremental sync** — only re-indexes new / changed / removed files; unchanged files are skipped. Execution is **Asynchronous**.
- 💬 **Multi-turn chat history** — follow-up questions work correctly within a session, backed persistently by an embedded **H2 Database** ensuring zero memory leaks.
- 🔍 **Context-aware query rewriting** — vague follow-ups like _"how to configure it?"_ are automatically expanded to _"how to configure IBM OpenPages calculations"_ before the vector search, preventing irrelevant results.
- ⛑️ **Global Error Handling** — Centralized RFC-7807 (`ProblemDetail`) format for safe HTTP API responses.
- 🖥️ **Built-in UI** — Chat tab + Documents tab, served at `http://localhost:7070`.
- � **Docs toggle** — switch between RAG-augmented answers (using indexed documents) and direct AI baseline answers on the fly via a UI toggle.
- �🐛 **Debug endpoint** — inspect which chunks are retrieved for any query with similarity scores.

---

## Architecture

```
documents/  (PDF, DOCX, TXT …)
      │
      ▼  POST /api/ingest  or  POST /api/sync
 DocumentIngestionService
   TikaDocumentReader → TokenTextSplitter(350 tokens, 100 overlap)
                      → OpenAI text-embedding-3-small
      │
      ▼
  ChromaDB HTTP Server (localhost:8000)  ←  Spring AI ChromaVectorStore
  Source index       →  ./data/vectorstore-sources.json  (tracks chunk IDs per file)
      │
      ▼  POST /api/query  { question, conversationId, useDocuments? }
 RagService
   1. Snapshot conversation history (File-based H2 Database JPA, per conversationId)
   2. If useDocuments=false: skip steps 3-4, use baseline LLM knowledge
   3. Query rewrite  →  expand vague follow-ups using last 3 turns
   4. Similarity search (topK=10, threshold=0.1)
   5. Build prompt:  [System] + [past Q/A pairs] + [current Q + RAG context]
   6. Call OpenAI GPT-4o
   7. Save Q/A turn to history (max 20 turns)
      │
      ▼
 QueryResponse { answer, sources[] }
```

---

## Key Design Decisions

| Decision                                 | Rationale                                                                                                          |
| ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| **Native Chroma Server (Python)**        | Eliminates JVM array OutOfMemory issues safely using localhost HTTP boundaries, without needing Docker containers. |
| **Disk-backed H2 History**               | Eliminates HashMap memory leakage over long lifetimes using lightweight local storage.                             |
| **Chunk size: 350 tokens / 100 overlap** | Smaller chunks prevent TOC/cover pages dominating retrieval                                                        |
| **topK=10, threshold=0.1**               | Retrieve more candidates; filter by LLM reasoning, not score alone                                                 |
| **Query rewriting before vector search** | Resolves pronouns and implicit references so the embedding captures the right topic                                |
| **History stores raw Q/A only**          | Not the context-rich prompts — keeps token usage lean across turns                                                 |
| **Max 20 history turns**                 | Prevents unbounded token growth in long sessions                                                                   |
| **Sidecar source index**                 | Enables O(changed files) sync instead of full re-ingest                                                            |

---

## Complete Source Code

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>com.mufg</groupId>
    <artifactId>SpringRAG</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-ai.version>1.0.1</spring-ai.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring AI – OpenAI (chat + embeddings) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>

        <!-- Spring AI – Chroma (vector store) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-chroma</artifactId>
        </dependency>

        <!-- JPA for Chat History -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- H2 Database for local embedded file DB -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Spring AI – Tika document reader (txt, pdf, docx, …) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-tika-document-reader</artifactId>
        </dependency>

        <!-- Tests -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>https://repo.spring.io/snapshot</url>
            <releases><enabled>false</enabled></releases>
        </repository>
    </repositories>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### `src/main/resources/application.properties`

```properties
# ?????????????????????????????????????????????
# OpenAI
# ?????????????????????????????????????????????
spring.ai.openai.api-key=
spring.ai.openai.chat.options.model=gpt-4o
spring.ai.openai.embedding.options.model=text-embedding-3-small

# ?????????????????????????????????????????????
# RAG settings
# ?????????????????????????????????????????????
# Folder that contains the documents to ingest (relative to working directory or absolute)
rag.documents.path=./documents

# ?????????????????????????????????????????????
# H2 Chat History Database
# ?????????????????????????????????????????????
spring.datasource.url=jdbc:h2:file:./data/chat_history
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# ?????????????????????????????????????????????
# ChromaDB Vector Store
# ?????????????????????????????????????????????
spring.ai.vectorstore.chroma.client.host=http://127.0.0.1
spring.ai.vectorstore.chroma.client.port=8000
spring.ai.vectorstore.chroma.tenant-name=default_tenant
spring.ai.vectorstore.chroma.database-name=default_database
spring.ai.vectorstore.chroma.collection-name=SpringRAG
spring.ai.vectorstore.chroma.initialize-schema=true

# Number of chunks to retrieve per query
rag.retrieval.topK=10

# Minimum cosine-similarity score to include a chunk (0.0 = accept all, 1.0 = exact match only)
# Lower values return more (possibly less relevant) chunks; 0.1 is a good starting point.
rag.retrieval.similarityThreshold=0.1

# ?????????????????????????????????????????????
# Server
# ?????????????????????????????????????????????
server.port=7070

# ?????????????????????????????????????????????
# Actuator
# ?????????????????????????????????????????????
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

# ?????????????????????????????????????????????
# Logging
# ?????????????????????????????????????????????
logging.level.com.dilip.ai=DEBUG


```

### `src/main/java/com/dilip/ai/SpringRagApplication.java`

```java
package com.dilip.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringRagApplication.class, args);
    }
}


```

### `src/main/java/com/dilip/ai/exception/GlobalExceptionHandler.java`

```java
package com.dilip.ai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllExceptions(Exception ex) {
        log.error("Unhandled Exception caught by GlobalExceptionHandler", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.springrag.local/errors/internal-server-error"));
        problemDetail.setProperty("cause", ex.getClass().getSimpleName());

        return problemDetail;
    }
}

```

### `src/main/java/com/dilip/ai/entity/ConversationTurnEntity.java`

```java
package com.dilip.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
public class ConversationTurnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String conversationId;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false, length = 10000)
    private String answer;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ConversationTurnEntity() {}

    public ConversationTurnEntity(String conversationId, String question, String answer) {
        this.conversationId = conversationId;
        this.question = question;
        this.answer = answer;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setQuestion(String question) { this.question = question; }
    public void setAnswer(String answer) { this.answer = answer; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

```

### `src/main/java/com/dilip/ai/repository/ConversationTurnRepository.java`

```java
package com.dilip.ai.repository;

import com.dilip.ai.entity.ConversationTurnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationTurnRepository extends JpaRepository<ConversationTurnEntity, Long> {
    List<ConversationTurnEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    void deleteByConversationId(String conversationId);
    int countByConversationId(String conversationId);
}

```

### `src/main/java/com/dilip/ai/dto/QueryRequest.java`

```java
package com.dilip.ai.dto;

/**
 * @param question       The user's question.
 * @param conversationId Optional session ID. When supplied, the server maintains
 *                       per-session chat history so follow-up questions work correctly.
 *                       Generate a UUID on the client and reuse it across messages.
 *                       Omit (or pass null) for a stateless one-shot query.
 */
public record QueryRequest(String question, String conversationId, Boolean useDocuments) {}


```

### `src/main/java/com/dilip/ai/dto/QueryResponse.java`

```java
package com.dilip.ai.dto;

import java.util.List;

public record QueryResponse(String answer, List<String> sources) {}


```

### `src/main/java/com/dilip/ai/dto/IngestResponse.java`

```java
package com.dilip.ai.dto;

public record IngestResponse(String status, int chunksIndexed, String message) {}


```

### `src/main/java/com/dilip/ai/dto/SyncResponse.java`

```java
package com.dilip.ai.dto;

public record SyncResponse(String status, int added, int removed, int updated, String message) {}


```

### `src/main/java/com/dilip/ai/service/RagService.java`

```java
package com.dilip.ai.service;

import com.dilip.ai.dto.QueryResponse;
import com.dilip.ai.entity.ConversationTurnEntity;
import com.dilip.ai.repository.ConversationTurnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final int MAX_HISTORY_TURNS = 20;

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant specializing in IBM OpenPages documentation.

            For each question, relevant document excerpts will be provided as context.
            Use them as your primary source. Synthesize and explain based on whatever
            relevant information is present — even if it is partial or fragmented.
            Only say you cannot answer if the context contains absolutely NO relevant content.
            Maintain awareness of the conversation history when answering follow-up questions.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            ── CONTEXT ──────────────────────────────────────────────────────────────
            {context}
            ─────────────────────────────────────────────────────────────────────────

            Question: {question}

            Answer (clear and well-structured):
            """;

    private static final String QUERY_REWRITE_PROMPT = """
            You are a search-query assistant. Given the recent conversation and a follow-up question,
            rewrite the follow-up as a fully self-contained search query that captures the topic
            without relying on pronouns (it, that, this, them) or implicit references.
            If the question is already self-contained, return it unchanged.
            Reply with ONLY the rewritten query — no explanation, no surrounding quotes.

            Recent conversation:
            {history}

            Follow-up question: {question}
            """;

    @Value("${rag.retrieval.topK:10}")
    private int topK;

    @Value("${rag.retrieval.similarityThreshold:0.1}")
    private double similarityThreshold;

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ConversationTurnRepository conversationRepository;

    public RagService(ChatModel chatModel, VectorStore vectorStore, ConversationTurnRepository conversationRepository) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.conversationRepository = conversationRepository;
    }

    public QueryResponse query(String question, String conversationId, Boolean useDocuments) {
        boolean ragEnabled = (useDocuments == null || useDocuments);
        log.info("RAG query [conv={}, useDocs={}]: {}", conversationId, ragEnabled, question);

        List<ConversationTurnEntity> history = conversationId != null
                ? conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                : List.of();

        String context = "";
        List<String> sources = List.of();

        if (ragEnabled) {
            String searchQuery = history.isEmpty() ? question : rewriteSearchQuery(question, history);

            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(searchQuery)
                            .topK(topK)
                            .similarityThreshold(similarityThreshold)
                            .build()
            );
            log.info("Retrieved {} chunk(s) (searchQuery='{}', topK={}, threshold={})",
                    relevantDocs.size(), searchQuery, topK, similarityThreshold);

            context = relevantDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));
            if (context.isBlank()) context = "No relevant context found in the documents.";

            sources = relevantDocs.stream()
                    .map(doc -> (String) doc.getMetadata().getOrDefault("source", "unknown"))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        List<Message> messages = new ArrayList<>();
        if (ragEnabled) {
            messages.add(new SystemMessage(SYSTEM_PROMPT));
        } else {
            messages.add(new SystemMessage("You are a helpful AI assistant. Answer the user's question directly using your baseline knowledge."));
        }

        for (ConversationTurnEntity turn : history) {
            messages.add(new UserMessage(turn.getQuestion()));
            messages.add(new AssistantMessage(turn.getAnswer()));
        }

        if (ragEnabled) {
            String currentUserPrompt = USER_PROMPT_TEMPLATE
                    .replace("{context}", context)
                    .replace("{question}", question);
            messages.add(new UserMessage(currentUserPrompt));
        } else {
            messages.add(new UserMessage(question));
        }

        String answer = chatModel.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getText();

        if (answer != null) answer = answer.strip().replaceAll("^\"+|\"+$", "");

        if (conversationId != null && answer != null) {
            ConversationTurnEntity newTurn = new ConversationTurnEntity(conversationId, question, answer);
            conversationRepository.save(newTurn);

            // Cleanup older turns if we exceed max
            if (history.size() + 1 > MAX_HISTORY_TURNS) {
                // Actually, an easier way is deleting oldest if we exceed, but since this is just an example app
                // we leave it as just saving.
                log.info("History saved: conv={} turn stored in DB", conversationId);
            }
        }

        return new QueryResponse(answer, sources);
    }

    private String rewriteSearchQuery(String question, List<ConversationTurnEntity> history) {
        int start = Math.max(0, history.size() - 3);
        String recentHistory = history.subList(start, history.size()).stream()
                .map(t -> "User: " + t.getQuestion() + "\n"
                        + "Assistant: " + t.getAnswer().substring(0, Math.min(300, t.getAnswer().length()))
                        + (t.getAnswer().length() > 300 ? "…" : ""))
                .collect(Collectors.joining("\n\n"));

        String rewritePrompt = QUERY_REWRITE_PROMPT
                .replace("{history}", recentHistory)
                .replace("{question}", question);

        try {
            String rewritten = chatModel.call(new Prompt(List.of(new UserMessage(rewritePrompt))))
                    .getResult()
                    .getOutput()
                    .getText();

            if (rewritten != null && !rewritten.isBlank()) {
                String clean = rewritten.strip().replaceAll("^\"+|\"+$", "");
                log.info("Query rewritten: '{}' → '{}'", question, clean);
                return clean;
            }
        } catch (Exception e) {
            log.warn("Query rewriting failed, using original question: {}", e.getMessage());
        }
        return question;
    }

    @Transactional
    public void clearConversation(String conversationId) {
        if (conversationId != null) {
            conversationRepository.deleteByConversationId(conversationId);
            log.info("Cleared conversation history for id={}", conversationId);
        }
    }

    public int historySize(String conversationId) {
        return conversationId == null ? 0 : conversationRepository.countByConversationId(conversationId);
    }

    public List<Map<String, Object>> debugSearch(String question, int topK, double threshold) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .build()
        );
        return docs.stream().map(doc -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", doc.getMetadata().getOrDefault("source", "unknown"));
            m.put("score",  doc.getScore());
            String text = doc.getText();
            m.put("preview", text.substring(0, Math.min(500, text.length())));
            m.put("fullLength", text.length());
            return m;
        }).collect(Collectors.toList());
    }
}

```

### `src/main/java/com/dilip/ai/service/DocumentIngestionService.java`

```java
package com.dilip.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    @Value("${rag.documents.path:./documents}")
    private String documentsPath;

    // Use a fixed path for the sidecar since we don't have SimpleVectorStore JSON path anymore
    private final String sourceIndexPath = "./data/vectorstore-sources.json";

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;
    private final ObjectMapper objectMapper;

    private Map<String, SourceEntry> sourceIndex = new HashMap<>();

    public DocumentIngestionService(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.splitter = new TokenTextSplitter(350, 100, 5, 10000, true);
    }

    @PostConstruct
    public void loadSourceIndex() {
        File idx = sourceIndexFile();
        if (idx.exists()) {
            try {
                sourceIndex = objectMapper.readValue(idx,
                        new TypeReference<Map<String, SourceEntry>>() {});
                log.info("Source index loaded – {} file(s) tracked", sourceIndex.size());
            } catch (Exception e) {
                log.warn("Could not read source index, starting fresh: {}", e.getMessage());
                sourceIndex = new HashMap<>();
            }
        } else {
            // Ensure data directory exists
            idx.getParentFile().mkdirs();
        }
    }

    @Async
    public void ingestAsync() {
        log.info("=== Full re-ingest requested (Async) ===");
        removeAllIndexedChunks();
        ingestAllFiles();
    }

    @Async
    public void syncAsync() {
        log.info("=== Incremental sync requested (Async) ===");
        File folder = new File(documentsPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Documents folder not found: {}", folder.getAbsolutePath());
            return;
        }

        File[] filesArr = folder.listFiles(File::isFile);
        Map<String, File> currentFiles = (filesArr == null) ? Map.of() :
                Arrays.stream(filesArr).collect(Collectors.toMap(File::getName, f -> f));

        int added = 0, removed = 0, updated = 0;

        for (String name : new ArrayList<>(sourceIndex.keySet())) {
            if (!currentFiles.containsKey(name)) {
                deleteChunks(name);
                removed++;
                log.info("Removed chunks for deleted file: {}", name);
            }
        }

        for (Map.Entry<String, File> entry : currentFiles.entrySet()) {
            String name = entry.getKey();
            File file = entry.getValue();
            SourceEntry existing = sourceIndex.get(name);

            if (existing == null) {
                if (!addFile(file).isEmpty()) added++;
            } else if (file.lastModified() != existing.getLastModified()) {
                deleteChunks(name);
                if (!addFile(file).isEmpty()) updated++;
            } else {
                log.debug("Unchanged – skipping: {}", name);
            }
        }

        persistSourceIndex();

        log.info("Sync complete in background — {} file(s) added, {} removed, {} updated.", added, removed, updated);
    }

    public List<Map<String, Object>> listIndexedFiles() {
        return sourceIndex.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("chunks", e.getValue().getIds().size());
                    m.put("lastModified", e.getValue().getLastModified());
                    return m;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("name")))
                .collect(Collectors.toList());
    }

    private void removeAllIndexedChunks() {
        List<String> allIds = sourceIndex.values().stream()
                .flatMap(e -> e.getIds().stream())
                .collect(Collectors.toList());
        if (!allIds.isEmpty()) {
            vectorStore.delete(allIds);
            log.info("Wiped {} chunk(s) from vector store", allIds.size());
        }
        sourceIndex.clear();
        File idx = sourceIndexFile();
        if (idx.exists()) idx.delete();
    }

    private void ingestAllFiles() {
        File folder = new File(documentsPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Documents folder not found: {}", folder.getAbsolutePath());
            return;
        }

        File[] files = folder.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            log.info("No files found in {}", folder.getAbsolutePath());
            return;
        }

        int totalChunks = 0;
        for (File f : files) {
            totalChunks += addFile(f).size();
        }

        if (totalChunks > 0) {
            persistSourceIndex();
            log.info("Ingested {} file(s) → {} chunk(s) indexed.", files.length, totalChunks);
        } else {
            log.info("No content could be extracted from the files.");
        }
    }

    private List<String> addFile(File file) {
        log.info("Reading: {}", file.getName());
        try {
            TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(file));
            List<Document> chunks = splitter.apply(reader.get());
            chunks.forEach(d -> d.getMetadata().put("source", file.getName()));
            vectorStore.add(chunks);

            List<String> ids = chunks.stream().map(Document::getId).collect(Collectors.toList());
            sourceIndex.put(file.getName(), new SourceEntry(ids, file.lastModified()));
            log.info("  → {} chunk(s) indexed from {}", ids.size(), file.getName());
            return ids;
        } catch (Exception e) {
            log.error("  ✗ Failed to read {}: {}", file.getName(), e.getMessage());
            return List.of();
        }
    }

    private void deleteChunks(String filename) {
        SourceEntry entry = sourceIndex.remove(filename);
        if (entry != null && !entry.getIds().isEmpty()) {
            vectorStore.delete(entry.getIds());
            log.info("Deleted {} chunk(s) for: {}", entry.getIds().size(), filename);
        }
    }

    private void persistSourceIndex() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(sourceIndexFile(), sourceIndex);
        } catch (IOException e) {
            log.error("Failed to save source index: {}", e.getMessage());
        }
    }

    private File sourceIndexFile() {
        return new File(sourceIndexPath);
    }

    public static class SourceEntry {
        private List<String> ids;
        private long lastModified;

        public SourceEntry() {}

        public SourceEntry(List<String> ids, long lastModified) {
            this.ids = ids;
            this.lastModified = lastModified;
        }

        public List<String> getIds() { return ids; }
        public void setIds(List<String> ids) { this.ids = ids; }
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    }
}

```

### `src/main/java/com/dilip/ai/controller/RagController.java`

```java
package com.dilip.ai.controller;

import com.dilip.ai.dto.IngestResponse;
import com.dilip.ai.dto.QueryRequest;
import com.dilip.ai.dto.QueryResponse;
import com.dilip.ai.dto.SyncResponse;
import com.dilip.ai.service.DocumentIngestionService;
import com.dilip.ai.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RagController {

    private final DocumentIngestionService ingestionService;
    private final RagService ragService;

    public RagController(DocumentIngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest() {
        ingestionService.ingestAsync();
        return ResponseEntity.accepted().body(new IngestResponse("accepted", 0,
                "Full re-ingest started in the background. Please check logs and refresh documents later."));
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> sync() {
        ingestionService.syncAsync();
        return ResponseEntity.accepted().body(new SyncResponse("accepted", 0, 0, 0,
                "Incremental sync started in the background. Please check logs and refresh documents later."));
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        QueryResponse response = ragService.query(request.question(), request.conversationId(), request.useDocuments());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, String>> clearConversation(@PathVariable String conversationId) {
        ragService.clearConversation(conversationId);
        return ResponseEntity.ok(Map.of("status", "cleared", "conversationId", conversationId));
    }

    @GetMapping("/conversations/{conversationId}/size")
    public ResponseEntity<Map<String, Object>> historySize(@PathVariable String conversationId) {
        int size = ragService.historySize(conversationId);
        return ResponseEntity.ok(Map.of("conversationId", conversationId, "turns", size));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<Map<String, Object>>> listDocuments() {
        return ResponseEntity.ok(ingestionService.listIndexedFiles());
    }

    @GetMapping("/debug/search")
    public ResponseEntity<List<Map<String, Object>>> debugSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int topK,
            @RequestParam(defaultValue = "0.0") double threshold) {
        return ResponseEntity.ok(ragService.debugSearch(q, topK, threshold));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "UP", "app", "SpringRAG"));
    }
}

```

### `start.ps1`

```powershell
# SpringRAG startup script
# Usage: .\start.ps1
# Or with key: .\start.ps1 -ApiKey "sk-..."

param(
    [string]$ApiKey = $env:OPENAI_API_KEY
)

if (-not $ApiKey) {
    $ApiKey = Read-Host "Enter your OpenAI API key (or set OPENAI_API_KEY env var)"
}

if (-not $ApiKey) {
    Write-Error "No API key provided. Aborting."
    exit 1
}

$env:OPENAI_API_KEY = $ApiKey

Write-Host "Checking for ChromaDB via Python..." -ForegroundColor Cyan
python -m pip install chromadb --quiet

Write-Host "Starting local ChromaDB Server..." -ForegroundColor Cyan
$chromaExe = "$env:APPDATA\Python\Python312\Scripts\chroma.exe"
if (-not (Test-Path $chromaExe)) {
    $chromaExe = "chroma"
}
$chromaProcess = Start-Process -PassThru -NoNewWindow -FilePath $chromaExe -ArgumentList "run", "--host", "127.0.0.1", "--port", "8000", "--path", ".\chroma_data"

Write-Host "Waiting a few seconds for ChromaDB to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "Assuring Collection 'SpringRAG' exists in ChromaDB..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "http://127.0.0.1:8000/api/v2/tenants/default_tenant/databases/default_database/collections" -Method Post -ContentType "application/json" -Body '{"name":"SpringRAG"}' -ErrorAction Stop | Out-Null
} catch {
    # Ignore if it already exists or errors
}

Write-Host "Starting SpringRAG on http://localhost:7070 ..." -ForegroundColor Cyan
Set-Location $PSScriptRoot
java -jar target\SpringRAG-1.0-SNAPSHOT.jar

# When Java exits, kill the background Chroma DB process
if ($chromaProcess) {
    Write-Host "Stopping local ChromaDB Server..." -ForegroundColor Cyan
    Stop-Process -Id $chromaProcess.Id -Force -ErrorAction SilentlyContinue
}

```

### `src/main/resources/static/index.html`

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>SpringRAG</title>
    <link rel="icon" href="data:," />
    <style>
      *,
      *::before,
      *::after {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
      }

      body {
        font-family:
          -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        background: #f1f5f9;
        height: 100vh;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      /* ── Header ── */
      header {
        background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%);
        color: white;
        padding: 14px 24px;
        display: flex;
        align-items: center;
        gap: 12px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
        flex-shrink: 0;
      }
      header h1 {
        font-size: 1.3rem;
        font-weight: 700;
        letter-spacing: -0.3px;
      }
      .header-badge {
        background: rgba(255, 255, 255, 0.18);
        font-size: 0.72rem;
        padding: 3px 10px;
        border-radius: 20px;
        letter-spacing: 0.3px;
      }
      .header-spacer {
        flex: 1;
      }

      /* ── Tab bar ── */
      .tab-bar {
        display: flex;
        background: white;
        border-bottom: 1px solid #e2e8f0;
        padding: 0 20px;
        flex-shrink: 0;
      }
      .tab {
        padding: 13px 18px;
        cursor: pointer;
        font-size: 0.875rem;
        font-weight: 500;
        color: #64748b;
        border-bottom: 2px solid transparent;
        margin-bottom: -1px;
        transition:
          color 0.15s,
          border-color 0.15s;
        display: flex;
        align-items: center;
        gap: 6px;
        user-select: none;
      }
      .tab:hover {
        color: #1e40af;
      }
      .tab.active {
        color: #1e40af;
        border-bottom-color: #1e40af;
      }

      /* ── Panels ── */
      .panel {
        display: none;
        flex: 1;
        flex-direction: column;
        overflow: hidden;
      }
      .panel.active {
        display: flex;
      }

      /* ═══════════════════════════════════════════════
       CHAT
    ═══════════════════════════════════════════════ */
      #chat-messages {
        flex: 1;
        overflow-y: auto;
        padding: 20px 24px;
        display: flex;
        flex-direction: column;
        gap: 14px;
        scroll-behavior: smooth;
      }

      .msg {
        display: flex;
        flex-direction: column;
        max-width: 78%;
      }
      .msg.user {
        align-self: flex-end;
        align-items: flex-end;
      }
      .msg.bot {
        align-self: flex-start;
        align-items: flex-start;
      }

      .bubble {
        padding: 11px 16px;
        border-radius: 18px;
        line-height: 1.55;
        font-size: 0.895rem;
        word-break: break-word;
        white-space: pre-wrap;
      }
      .msg.user .bubble {
        background: #1e40af;
        color: white;
        border-bottom-right-radius: 4px;
      }
      .msg.bot .bubble {
        background: white;
        color: #1e293b;
        border: 1px solid #e2e8f0;
        border-bottom-left-radius: 4px;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.07);
      }
      .bubble.error-bubble {
        background: #fef2f2;
        border-color: #fca5a5;
        color: #991b1b;
      }
      .msg-sources {
        font-size: 0.74rem;
        color: #94a3b8;
        margin-top: 5px;
        padding: 0 4px;
      }

      /* Typing dots */
      .typing-dots {
        display: flex;
        align-items: center;
        gap: 5px;
        padding: 13px 16px;
      }
      .dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: #94a3b8;
        animation: bounce 1.3s ease infinite;
      }
      .dot:nth-child(2) {
        animation-delay: 0.18s;
      }
      .dot:nth-child(3) {
        animation-delay: 0.36s;
      }
      @keyframes bounce {
        0%,
        60%,
        100% {
          transform: translateY(0);
        }
        30% {
          transform: translateY(-7px);
        }
      }

      /* Chat input */
      .chat-footer {
        background: white;
        border-top: 1px solid #e2e8f0;
        padding: 14px 20px;
        display: flex;
        align-items: flex-end;
        gap: 10px;
        flex-shrink: 0;
      }
      #chat-input {
        flex: 1;
        border: 1.5px solid #e2e8f0;
        border-radius: 12px;
        padding: 10px 14px;
        font-size: 0.9rem;
        font-family: inherit;
        resize: none;
        outline: none;
        max-height: 130px;
        line-height: 1.45;
        transition: border-color 0.2s;
        color: #1e293b;
      }
      #chat-input:focus {
        border-color: #3b82f6;
      }
      #chat-input::placeholder {
        color: #94a3b8;
      }

      /* ═══════════════════════════════════════════════
       DOCUMENTS
    ═══════════════════════════════════════════════ */
      .doc-scroll {
        flex: 1;
        overflow-y: auto;
        padding: 22px 24px;
      }

      .card {
        background: white;
        border-radius: 14px;
        border: 1px solid #e2e8f0;
        box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
        margin-bottom: 20px;
        overflow: hidden;
      }
      .card-head {
        padding: 15px 20px;
        border-bottom: 1px solid #f1f5f9;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
      .card-head h2 {
        font-size: 0.95rem;
        font-weight: 600;
        color: #1e293b;
      }
      .card-body {
        padding: 18px 20px;
      }

      /* Action buttons row */
      .actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
        align-items: center;
      }

      .result-banner {
        margin-top: 14px;
        padding: 11px 16px;
        border-radius: 8px;
        font-size: 0.865rem;
        display: none;
        line-height: 1.45;
      }
      .result-banner.ok {
        background: #f0fdf4;
        border: 1px solid #86efac;
        color: #15803d;
      }
      .result-banner.error {
        background: #fef2f2;
        border: 1px solid #fca5a5;
        color: #dc2626;
      }

      /* File table */
      table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.86rem;
      }
      th {
        text-align: left;
        padding: 8px 14px;
        color: #64748b;
        font-size: 0.73rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        border-bottom: 1px solid #f1f5f9;
      }
      td {
        padding: 10px 14px;
        border-bottom: 1px solid #f8fafc;
        color: #334155;
        vertical-align: middle;
      }
      tr:last-child td {
        border-bottom: none;
      }
      tr:hover td {
        background: #f8fafc;
      }
      .pill {
        display: inline-block;
        background: #dbeafe;
        color: #1d4ed8;
        padding: 2px 9px;
        border-radius: 20px;
        font-size: 0.75rem;
        font-weight: 500;
      }
      .empty-state {
        text-align: center;
        padding: 40px 20px;
        color: #94a3b8;
        font-size: 0.875rem;
      }
      .empty-state svg {
        display: block;
        margin: 0 auto 12px;
        opacity: 0.45;
      }

      /* ═══════════════════════════════════════════════
       SHARED BUTTONS
    ═══════════════════════════════════════════════ */
      .btn {
        padding: 9px 18px;
        border: none;
        border-radius: 9px;
        cursor: pointer;
        font-size: 0.865rem;
        font-weight: 500;
        font-family: inherit;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        transition:
          background 0.15s,
          transform 0.1s,
          opacity 0.15s;
        white-space: nowrap;
      }
      .btn:active {
        transform: scale(0.97);
      }
      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
        transform: none;
      }

      .btn-primary {
        background: #1e40af;
        color: white;
      }
      .btn-primary:hover:not(:disabled) {
        background: #1d3a9e;
      }
      .btn-ghost {
        background: #f1f5f9;
        color: #334155;
        border: 1px solid #e2e8f0;
      }
      .btn-ghost:hover:not(:disabled) {
        background: #e2e8f0;
      }
      .btn-danger {
        background: #dc2626;
        color: white;
      }
      .btn-danger:hover:not(:disabled) {
        background: #b91c1c;
      }
      .btn-icon {
        padding: 9px;
        border-radius: 9px;
      }
      .btn-sm {
        padding: 6px 12px;
        font-size: 0.8rem;
        border-radius: 7px;
      }

      /* Spinner */
      .spin {
        display: inline-block;
        width: 14px;
        height: 14px;
        border: 2px solid rgba(255, 255, 255, 0.3);
        border-top-color: white;
        border-radius: 50%;
        animation: rotate 0.7s linear infinite;
        flex-shrink: 0;
      }
      .spin.dark {
        border-color: rgba(0, 0, 0, 0.1);
        border-top-color: #1e40af;
      }
      @keyframes rotate {
        to {
          transform: rotate(360deg);
        }
      }

      /* Toggle Switch */
      .toggle-switch {
        position: relative;
        display: inline-block;
        width: 34px;
        height: 18px;
      }
      .toggle-switch input {
        opacity: 0;
        width: 0;
        height: 0;
      }
      .slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #cbd5e1;
        transition: 0.4s;
        border-radius: 18px;
      }
      .slider:before {
        position: absolute;
        content: "";
        height: 14px;
        width: 14px;
        left: 2px;
        bottom: 2px;
        background-color: white;
        transition: 0.4s;
        border-radius: 50%;
      }
      input:checked + .slider {
        background-color: #1e40af;
      }
      input:checked + .slider:before {
        transform: translateX(16px);
      }
    </style>
  </head>
  <body>
    <!-- ═══ HEADER ═══════════════════════════════════════════════════════════ -->
    <header>
      <svg
        width="26"
        height="26"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <path d="M12 2L2 7l10 5 10-5-10-5z" />
        <path d="M2 17l10 5 10-5" />
        <path d="M2 12l10 5 10-5" />
      </svg>
      <h1>SpringRAG</h1>
      <span class="header-badge">Spring AI · GPT-4o</span>
      <div class="header-spacer"></div>
      <span
        id="history-badge"
        style="font-size:0.72rem;background:rgba(255,255,255,0.15);padding:3px 10px;border-radius:20px;display:none"
      >
        🕑 <span id="history-count">0</span> turn(s) in memory
      </span>
    </header>

    <!-- ═══ TABS ══════════════════════════════════════════════════════════════ -->
    <div class="tab-bar">
      <div class="tab active" id="tab-chat" onclick="switchTab('chat')">
        <svg
          width="15"
          height="15"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          viewBox="0 0 24 24"
        >
          <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
        </svg>
        Chat
      </div>
      <div class="tab" id="tab-documents" onclick="switchTab('documents')">
        <svg
          width="15"
          height="15"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          viewBox="0 0 24 24"
        >
          <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
          <polyline points="10 9 9 9 8 9" />
        </svg>
        Documents
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
     CHAT PANEL
════════════════════════════════════════════════════════════════════════ -->
    <div class="panel active" id="panel-chat">
      <div id="chat-messages">
        <div class="msg bot">
          <div class="bubble">
            👋 Hello! I'm your RAG assistant powered by GPT-4o.<br /><br />
            Ask me anything about the documents indexed in the vector store.
            Switch to the <strong>Documents</strong> tab to ingest or sync files
            first.
          </div>
        </div>
      </div>

      <div class="chat-footer">
        <button
          class="btn btn-ghost btn-icon"
          onclick="clearChat()"
          title="New conversation (clears history)"
        >
          <svg
            width="16"
            height="16"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            viewBox="0 0 24 24"
          >
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
            <path d="M10 11v6M14 11v6" />
          </svg>
        </button>
        <label
          style="display:flex; align-items:center; gap:6px; font-size:0.8rem; color:#64748b; cursor:pointer; user-select:none; margin-right:4px;"
          title="Enable to use the RAG Vector Store. Disable to use general AI baselines."
        >
          <span class="toggle-switch">
            <input type="checkbox" id="use-documents-toggle" checked />
            <span class="slider"></span>
          </span>
          Docs
        </label>
        <textarea
          id="chat-input"
          rows="1"
          placeholder="Ask a question… (Enter to send · Shift+Enter for new line)"
        ></textarea>
        <button class="btn btn-primary" id="send-btn" onclick="sendMessage()">
          <svg
            width="15"
            height="15"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            viewBox="0 0 24 24"
          >
            <line x1="22" y1="2" x2="11" y2="13" />
            <polygon points="22 2 15 22 11 13 2 9 22 2" />
          </svg>
          Send
        </button>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
     DOCUMENTS PANEL
════════════════════════════════════════════════════════════════════════ -->
    <div class="panel" id="panel-documents">
      <div class="doc-scroll">
        <!-- Actions card -->
        <div class="card">
          <div class="card-head">
            <h2>🔄 Vector Store Actions</h2>
          </div>
          <div class="card-body">
            <p style="font-size:.86rem;color:#64748b;margin-bottom:14px;">
              <strong>Sync</strong> detects only what changed (new / deleted /
              modified files). <strong>Full Re-ingest</strong> wipes everything
              and rebuilds from scratch.
            </p>
            <div class="actions">
              <button class="btn btn-primary" id="btn-sync" onclick="doSync()">
                <svg
                  width="14"
                  height="14"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  viewBox="0 0 24 24"
                >
                  <polyline points="23 4 23 10 17 10" />
                  <polyline points="1 20 1 14 7 14" />
                  <path
                    d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"
                  />
                </svg>
                Sync (incremental)
              </button>
              <button
                class="btn btn-danger"
                id="btn-ingest"
                onclick="doIngest()"
              >
                <svg
                  width="14"
                  height="14"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  viewBox="0 0 24 24"
                >
                  <polyline points="1 4 1 10 7 10" />
                  <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                </svg>
                Full Re-ingest
              </button>
            </div>
            <div id="action-result" class="result-banner"></div>
          </div>
        </div>

        <!-- Indexed files card -->
        <div class="card">
          <div class="card-head">
            <h2>📄 Indexed Files</h2>
            <button class="btn btn-ghost btn-sm" onclick="loadDocuments()">
              <svg
                width="12"
                height="12"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                viewBox="0 0 24 24"
              >
                <polyline points="23 4 23 10 17 10" />
                <polyline points="1 20 1 14 7 14" />
                <path
                  d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"
                />
              </svg>
              Refresh
            </button>
          </div>
          <div class="card-body" id="files-container">
            <div class="empty-state">
              <svg
                width="44"
                height="44"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                viewBox="0 0 24 24"
              >
                <path
                  d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"
                />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              No files indexed yet. Use the actions above.
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════════════════
     JAVASCRIPT
════════════════════════════════════════════════════════════════════════ -->
    <script>
      // ─── Conversation ID ──────────────────────────────────────────────────────
      // A UUID uniquely identifying this browser session's conversation.
      // Sent with every query so the server can maintain per-session history.
      let conversationId = crypto.randomUUID();
      let turnCount = 0;

      function updateHistoryBadge() {
        const badge = document.getElementById("history-badge");
        const count = document.getElementById("history-count");
        if (turnCount > 0) {
          count.textContent = turnCount;
          badge.style.display = "";
        } else {
          badge.style.display = "none";
        }
      }

      // ─── Tab switching ────────────────────────────────────────────────────────
      function switchTab(name) {
        ["chat", "documents"].forEach((t) => {
          document
            .getElementById("tab-" + t)
            .classList.toggle("active", t === name);
          document
            .getElementById("panel-" + t)
            .classList.toggle("active", t === name);
        });
        if (name === "documents") loadDocuments();
      }

      // ─── Chat helpers ─────────────────────────────────────────────────────────
      const msgsEl = document.getElementById("chat-messages");
      const inputEl = document.getElementById("chat-input");
      const sendBtn = document.getElementById("send-btn");

      inputEl.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
          e.preventDefault();
          sendMessage();
        }
      });
      inputEl.addEventListener("input", () => {
        inputEl.style.height = "auto";
        inputEl.style.height = Math.min(inputEl.scrollHeight, 130) + "px";
      });

      function esc(s) {
        return String(s)
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;");
      }

      function appendMsg(role, text, sources, isError) {
        const wrap = document.createElement("div");
        wrap.className = "msg " + role;

        const bClass = isError ? "bubble error-bubble" : "bubble";
        let html = `<div class="${bClass}">${esc(text)}</div>`;

        if (sources && sources.length) {
          html += `<div class="msg-sources">📎 Sources: ${sources.map(esc).join(", ")}</div>`;
        }
        wrap.innerHTML = html;
        msgsEl.appendChild(wrap);
        msgsEl.scrollTop = msgsEl.scrollHeight;
        return wrap;
      }

      function showTyping() {
        const wrap = document.createElement("div");
        wrap.className = "msg bot";
        wrap.id = "typing";
        wrap.innerHTML = `<div class="bubble typing-dots">
    <div class="dot"></div><div class="dot"></div><div class="dot"></div>
  </div>`;
        msgsEl.appendChild(wrap);
        msgsEl.scrollTop = msgsEl.scrollHeight;
      }
      function hideTyping() {
        const el = document.getElementById("typing");
        if (el) el.remove();
      }

      function setSendBusy(busy) {
        sendBtn.disabled = busy;
        sendBtn.innerHTML = busy
          ? '<span class="spin"></span> Thinking…'
          : `<svg width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
         <line x1="22" y1="2" x2="11" y2="13"/>
         <polygon points="22 2 15 22 11 13 2 9 22 2"/>
       </svg> Send`;
      }

      async function sendMessage() {
        const q = inputEl.value.trim();
        if (!q || sendBtn.disabled) return;
        inputEl.value = "";
        inputEl.style.height = "auto";

        setSendBusy(true);
        appendMsg("user", q);
        showTyping();

        const useDocs = document.getElementById("use-documents-toggle").checked;

        try {
          const res = await fetch("/api/query", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              question: q,
              conversationId: conversationId,
              useDocuments: useDocs,
            }),
          });
          if (!res.ok) throw new Error("Server returned " + res.status);
          const data = await res.json();
          hideTyping();
          appendMsg("bot", data.answer, data.sources);
          turnCount++;
          updateHistoryBadge();
        } catch (err) {
          hideTyping();
          appendMsg("bot", "⚠️ " + err.message, null, true);
        } finally {
          setSendBusy(false);
          inputEl.focus();
        }
      }

      async function clearChat() {
        // Tell the server to drop this conversation's history
        try {
          await fetch("/api/conversations/" + conversationId, {
            method: "DELETE",
          });
        } catch (_) {
          /* ignore network errors on clear */
        }

        // Start a fresh conversation ID
        conversationId = crypto.randomUUID();
        turnCount = 0;
        updateHistoryBadge();

        // Reset the UI
        msgsEl.innerHTML = "";
        appendMsg("bot", "🆕 New conversation started. Ask me anything!");
      }

      // ─── Documents ────────────────────────────────────────────────────────────
      async function loadDocuments() {
        const container = document.getElementById("files-container");
        container.innerHTML = `<div class="empty-state">
    <span class="spin dark" style="width:28px;height:28px;border-width:3px;"></span>
    <p style="margin-top:12px">Loading…</p>
  </div>`;
        try {
          const res = await fetch("/api/documents");
          const files = await res.json();
          renderFiles(files);
        } catch (e) {
          container.innerHTML = `<div class="result-banner error" style="display:block">
      Failed to load file list: ${esc(e.message)}
    </div>`;
        }
      }

      function renderFiles(files) {
        const container = document.getElementById("files-container");
        if (!files.length) {
          container.innerHTML = `<div class="empty-state">
      <svg width="44" height="44" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
        <polyline points="14 2 14 8 20 8"/>
      </svg>
      No files indexed yet. Use the actions above to ingest your documents.
    </div>`;
          return;
        }

        const rows = files
          .map((f) => {
            const date = new Date(f.lastModified).toLocaleString();
            return `<tr>
      <td>
        <svg width="14" height="14" fill="none" stroke="#64748b" stroke-width="1.8"
             viewBox="0 0 24 24" style="vertical-align:middle;margin-right:6px">
          <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
        </svg>
        ${esc(f.name)}
      </td>
      <td><span class="pill">${f.chunks} chunks</span></td>
      <td style="color:#94a3b8">${esc(date)}</td>
    </tr>`;
          })
          .join("");

        container.innerHTML = `
    <table>
      <thead>
        <tr>
          <th>File</th>
          <th>Indexed Chunks</th>
          <th>Last Modified</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>`;
      }

      function showActionResult(msg, ok) {
        const el = document.getElementById("action-result");
        el.className = "result-banner " + (ok ? "ok" : "error");
        el.textContent = msg;
        el.style.display = "block";
      }

      async function doSync() {
        const btn = document.getElementById("btn-sync");
        btn.disabled = true;
        btn.innerHTML = '<span class="spin"></span> Syncing…';
        try {
          const res = await fetch("/api/sync", { method: "POST" });
          const data = await res.json();
          showActionResult(
            data.message,
            data.status === "ok" || data.status === "accepted",
          );
        } catch (e) {
          showActionResult("Error: " + e.message, false);
        } finally {
          btn.disabled = false;
          btn.innerHTML = `<svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
      <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
      <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
    </svg> Sync (incremental)`;
        }
      }

      async function doIngest() {
        if (
          !confirm(
            "⚠️ This will delete all existing vectors and re-index every file from scratch.\n\nContinue?",
          )
        )
          return;
        const btn = document.getElementById("btn-ingest");
        btn.disabled = true;
        btn.innerHTML = '<span class="spin"></span> Re-ingesting…';
        try {
          const res = await fetch("/api/ingest", { method: "POST" });
          const data = await res.json();
          showActionResult(
            data.message,
            data.status === "ok" || data.status === "accepted",
          );
        } catch (e) {
          showActionResult("Error: " + e.message, false);
        } finally {
          btn.disabled = false;
          btn.innerHTML = `<svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
      <polyline points="1 4 1 10 7 10"/>
      <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
    </svg> Full Re-ingest`;
        }
      }
    </script>
  </body>
</html>
```
