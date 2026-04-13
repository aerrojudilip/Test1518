# SpringRAG

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

## Quick Start

### Prerequisites

- Java 21+, Maven 3.9+, OpenAI API key

### Build

```bash
mvn clean package -DskipTests
```

### Run (Windows PowerShell)

```powershell
$env:OPENAI_API_KEY = "sk-..."
java -jar target/SpringRAG-1.0-SNAPSHOT.jar
```

### Run (Linux / macOS)

```bash
export OPENAI_API_KEY=sk-...
java -jar target/SpringRAG-1.0-SNAPSHOT.jar
```

Or use the included helper script (prompts for key if not set):

```powershell
.\start.ps1 -ApiKey "sk-..."
```

### First Use

1. Drop your documents into the `documents/` folder
2. Open `http://localhost:7070` → **Documents** tab → click **Sync (incremental)**
3. Switch to **Chat** tab and start asking questions

---

## Configuration

All settings are in `src/main/resources/application.properties`:

| Property                                   | Default                            | Description                            |
| ------------------------------------------ | ---------------------------------- | -------------------------------------- |
| `spring.ai.openai.api-key`                 | _(required)_                       | OpenAI API key                         |
| `spring.ai.openai.chat.options.model`      | `gpt-4o`                           | LLM model for answers                  |
| `spring.ai.openai.embedding.options.model` | `text-embedding-3-small`           | Embedding model                        |
| `rag.documents.path`                       | `./documents`                      | Folder scanned for documents           |
| `spring.datasource.url`                    | `jdbc:h2:file:./data/chat_history` | Persistent conversational DB           |
| `spring.ai.vectorstore.chroma.client.host` | `http://127.0.0.1`                 | Local ChromaDB server                  |
| `rag.retrieval.topK`                       | `10`                               | Chunks retrieved per query             |
| `rag.retrieval.similarityThreshold`        | `0.1`                              | Min cosine-similarity (0 = accept all) |
| `server.port`                              | `7070`                             | HTTP port                              |

---

## REST API

| Method   | Endpoint                       | Body / Params                                  | Description                                                                                                                    |
| -------- | ------------------------------ | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `POST`   | `/api/query`                   | `{ question, conversationId?, useDocuments? }` | RAG query; pass same `conversationId` across turns for chat history. Set `useDocuments: false` to skip RAG and use baseline AI |
| `DELETE` | `/api/conversations/{id}`      | —                                              | Clear server-side history for a session                                                                                        |
| `GET`    | `/api/conversations/{id}/size` | —                                              | Number of turns stored (use to verify history is working)                                                                      |
| `POST`   | `/api/sync`                    | —                                              | Incremental sync (runs asynchronously; HTTP 202 Accepted)                                                                      |
| `POST`   | `/api/ingest`                  | —                                              | Full wipe + re-ingest all documents (runs asynchronously; HTTP 202 Accepted)                                                   |
| `GET`    | `/api/documents`               | —                                              | List indexed files with chunk counts                                                                                           |
| `GET`    | `/api/debug/search`            | `?q=...&topK=10&threshold=0.0`                 | Inspect retrieved chunks + scores                                                                                              |
| `GET`    | `/api/status`                  | —                                              | Liveness check                                                                                                                 |

### Chat history usage

```json
// Turn 1
POST /api/query
{ "question": "explain IBM OpenPages calculations", "conversationId": "my-uuid" }

// Turn 2 — follow-up; server auto-rewrites "it" → "IBM OpenPages calculations"
POST /api/query
{ "question": "how to configure it?", "conversationId": "my-uuid" }

// Turn 3 — skip RAG, use baseline AI knowledge
POST /api/query
{ "question": "what is a REST API?", "conversationId": "my-uuid", "useDocuments": false }

// Clear history (e.g. on "New Chat")
DELETE /api/conversations/my-uuid
```

---

## How Chat History + Query Rewriting Works

```
Turn 1:  "explain IBM OpenPages calculations"
         → vector search: "explain IBM OpenPages calculations"   ✅ direct
         → answer stored in server memory for conversationId

Turn 2:  "how to configure it?"
         → query rewriter sees last turn about "calculations"
         → rewrites to: "how to configure IBM OpenPages calculations"
         → vector search on rewritten query                      ✅ relevant chunks
         → LLM receives: [System] [Turn1 Q] [Turn1 A] [Turn2 Q + context]
         → answer is relevant to calculations, not email/SMTP    ✅
```

**Without query rewriting**, `"how to configure it?"` would match SMTP/email configuration chunks in the docs — a very common failure mode in naïve RAG + chat history systems.

---

## Document Management

### When to use Sync vs Full Re-ingest

| Scenario                              | Action             |
| ------------------------------------- | ------------------ |
| Added new files to `documents/`       | **Sync**           |
| Removed files from `documents/`       | **Sync**           |
| Modified existing files               | **Sync**           |
| Changed chunk size or embedding model | **Full Re-ingest** |
| Vector store corrupted / fresh start  | **Full Re-ingest** |

### Sidecar source index

`vectorstore-sources.json` tracks `filename → { chunkIds[], lastModified }`. Sync uses this to delete only the affected chunks without touching the rest of the vector store.

---

## Troubleshooting

| Problem                                                                | Cause                                       | Fix                                                                                                                                        |
| ---------------------------------------------------------------------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Answer is about wrong topic (e.g. email when asked about calculations) | Old JAR without query rewriting             | Rebuild with `mvn clean package -DskipTests` and restart                                                                                   |
| "not enough information" answers                                       | Chunks retrieved are TOC/cover pages        | Use `GET /api/debug/search?q=...` to inspect; threshold is already 0.1                                                                     |
| Port 7070 in use                                                       | Another process holds the port              | `Get-Process -Name java \| Stop-Process -Force` then restart                                                                               |
| JAR locked during rebuild                                              | Running Java holds file lock                | Kill Java first, then rebuild                                                                                                              |
| HTTP timeout on large re-ingest                                        | Check the java background logs              | Since syncing is fully asynchronous, your web UI reacts instantly and the work is safely buffered on a background thread. Check Java logs. |
| Chat history not working                                               | Old JAR (pre-history changes) still running | `mvn clean package -DskipTests` then restart; verify with `GET /api/conversations/{id}/size`                                               |
| `spring-ai-openai-spring-boot-starter` not found                       | Wrong artifact name                         | Use `spring-ai-starter-model-openai`                                                                                                       |

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
