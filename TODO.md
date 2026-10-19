# VoxCode Implementation TODO

## Current Project Status

**Current Phase:** Phase 22 — Engineering Report
**Current Task:** VXC-210 — Report Generation
**Current Task Status:** COMPLETED

### Completed Tasks
- [x] VXC-001 — Initialize backend repository
- [x] VXC-002 — Establish local environment
- [x] VXC-010 — Configuration Management and Logging
- [x] VXC-011 — Database and Migrations
- [x] VXC-020 — GitHub Actions Core Pipeline
- [x] VXC-030 — Repository Ingestion & Validation
- [x] VXC-040 — JavaParser Integration
- [x] VXC-050 — JGraphT Integration
- [x] VXC-060 — Structural and Semantic Indexing Layer
- [x] VXC-070 — RAG Pipeline Setup
- [x] VXC-080 — Retrieval Benchmarking
- [x] VXC-090 — Core Agent Tools
- [x] VXC-100 — Core Investigation Loop
- [x] VXC-110 — Agent Benchmark
- [x] VXC-120 — Structured Evidence Model
- [x] VXC-130 — Evidence Verification
- [x] VXC-140 — Plan Generation
- [x] VXC-150 — Approval Workflow
- [x] VXC-160 — Bounded Remediation Service
- [x] VXC-170 — Isolated Workspace Execution
- [x] VXC-180 — Executable Verification
- [x] VXC-190 — Bounded Repair Loop
- [x] VXC-200 — State Preservation and Recovery
- [x] VXC-210 — Report Generation

### In Progress Tasks
- [ ] VXC-220 — Telemetry and Metrics

### Next Tasks
- [ ] VXC-220 — Telemetry and Metrics
- [ ] VXC-095 — Focused MCP Server (OPTIONAL)

### Overall Progress
**22 / 28 phases completed** (Phases 0–21 complete)
**24 / 34 VXC tasks completed** (70.6% complete)
**Phase 0:** 2/2 tasks complete (100%)  
**Phase 1:** 2/2 tasks complete (100%)  
**Phase 2:** 1/1 tasks complete (100%)  
**Phase 3:** 1/1 tasks complete (100%)  
**Phase 4:** 1/1 tasks complete (100%)  
**Phase 5:** 1/1 tasks complete (100%)
**Phase 6:** 1/1 tasks complete (100%)
**Phase 7:** 1/1 tasks complete (100%)
**Phase 8:** 1/1 tasks complete (100%)
**Phase 9:** 1/1 tasks complete (100%)
**Phase 10:** 0/1 tasks complete (0%) — OPTIONAL, deferred
**Phase 11:** 1/1 tasks complete (100%)
**Phase 12:** 1/1 tasks complete (100%)
**Phase 13:** 1/1 tasks complete (100%)
**Phase 14:** 1/1 tasks complete (100%)
**Phase 15:** 1/1 tasks complete (100%)
**Phase 16:** 1/1 tasks complete (100%)
**Phase 17:** 1/1 tasks complete (100%)
**Phase 18:** 1/1 tasks complete (100%)
**Phase 19:** 1/1 tasks complete (100%)
**Phase 20:** 1/1 tasks complete (100%)
**Phase 21:** 1/1 tasks complete (100%)
**Phase 22:** 1/1 tasks complete (100%)

### Status Rules

#### Task Status
- `[ ]` = NOT STARTED
- `[/]` = IN PROGRESS  
- `[x]` = COMPLETE

#### Current Phase Rule
The current phase is the earliest phase containing an incomplete VXC task. A phase becomes COMPLETE only when ALL VXC tasks inside that phase are marked `[x]`.

#### Current Task Rule
The current task is the VXC task actively being implemented and marked `[/]`. Only ONE VXC task should normally be marked `[/]` at a time unless there is a genuine dependency reason.

#### Next Task Rule
The next task is determined from the existing dependency order in TODO.md. Do NOT invent a new task order. Respect all existing VXC dependencies.

### Phase Progress Overview

| Phase | Area | Status |
|---|---|---|
| 0 | Repository / Project Setup | COMPLETE |
| 1 | Backend Engineering Foundation (2 tasks) | COMPLETE |
| 2 | CI/CD (1 task) | COMPLETE |
| 3 | Repository Ingestion / Workspaces | COMPLETE |
| 4 | AST Intelligence | COMPLETE |
| 5 | Dependency Graph | COMPLETE |
| 6 | Repository Index | COMPLETE |
| 7 | Hybrid Repository-Aware RAG | COMPLETE |
| 8 | RAG Evaluation | COMPLETE |
| 9 | Agent Tools | COMPLETE |
| 10 | MCP Tool Interface | NOT STARTED (OPTIONAL) |
| 11 | Adaptive Investigation Agent | COMPLETE |
| 12 | Agent Evaluation | COMPLETE |
| 13 | Evidence Engine | COMPLETE |
| 14 | Finding Validation | COMPLETE |
| 15 | Engineering Planning | COMPLETE |
| 16 | Human Approval | COMPLETE |
| 17 | Bounded Remediation | COMPLETE |
| 18 | Docker Sandbox | COMPLETE |
| 19 | Build/Test/Static Verification | COMPLETE |
| 20 | Limited Repair | COMPLETE |
| 21 | Rollback | COMPLETE |
| 22 | Engineering Report | COMPLETE |
| 22 | Engineering Report | NOT STARTED |
| 23 | Observability | NOT STARTED |
| 24 | Security Hardening | NOT STARTED |
| 25 | Frontend (4 tasks) | NOT STARTED |
| 26 | End-to-End Integration | NOT STARTED |
| 27 | Final Evaluation | NOT STARTED |
| 28 | Release Engineering | NOT STARTED |

### Developer Workflow
1. Select the next VXC task according to dependencies.
2. Mark it `[/]`.
3. Create the appropriate Git branch.
4. Implement the task.
5. Write/update tests.
6. Run required validation.
7. Commit changes.
8. Push branch.
9. Open PR.
10. CI passes.
11. Review completed.
12. Merge.
13. Verify the VXC Definition of Done.
14. Mark the VXC task `[x]`.
15. Move to the next task.

**Note:** Tasks are only marked `[x]` when the existing Definition of Done has genuinely been satisfied, not merely when code has been written.

---

## Project Identity

**VoxCode — Agentic Java RAG Code Intelligence**

VoxCode is an Agentic Java RAG intelligence system that investigates Java/Spring repositories using AST, dependency analysis and hybrid retrieval, produces evidence-backed engineering plans, performs human-approved bounded code changes, and verifies or rolls back those changes.

**Core Principle:**
Understand → Retrieve → Investigate → Prove → Plan → Act → Verify

**Core Thesis:**
"Understand before changing, prove before acting, and verify after acting."

**Architectural Flow:**
Repository → Repository Intelligence (AST + Dependency Graph + Repository Index) → Hybrid Repository-Aware RAG → One Adaptive Primary Agent → Evidence Engine → Finding Validation → Engineering Plan → Human Approval → Bounded Remediation → MCP/Tool Interface → Isolated Docker Workspace → Build + Tests + Static Analysis + Diff/Scope Validation → PASS (Accept) / FAIL (Bounded Repair) / FAIL after retry budget (Rollback) → Engineering Report → Evaluation

**Technology Scope (LOCKED):**

**TIER 1 — PRIMARY IDENTITY**
- **Backend:** Java 21, Spring Boot, Maven
- **Agentic AI:** Spring AI, one adaptive primary agent, tool calling, structured agent state, persisted decision/engineering traces
- **Repository Intelligence:** JavaParser for AST/program structure, JGraphT for dependency relationships, Repository Index for unified querying
- **Evidence Engine:** Structured evidence model, finding validation

**TIER 2 — MAJOR AI SUBSYSTEM**
- **RAG:** Embeddings, Qdrant, hybrid retrieval (vector + lexical + metadata/symbol + dependency-aware), reranking, context assembly, agentic retrieval, RAG evaluation and ablation

**TIER 3 — CONTROLLED ENGINEERING ACTION**
- **Remediation:** OpenRewrite for deterministic Java/Spring transformations where appropriate, AST-aware transformation, AI-generated targeted patches only when deterministic transformation is insufficient
- **Execution:** Docker sandbox, controlled build/test execution, resource limits, restricted filesystem/network access
- **Verification:** Build, tests, static analysis, diff validation, bounded repair, rollback
- **Planning:** Engineering planning, human approval

**TIER 4 — SUPPORTING PRODUCT INFRASTRUCTURE**
- **Database:** MySQL for application state (repositories, investigations, agent traces, findings, evidence, plans, approvals, executions, verification results, evaluation results) - treated as supporting infrastructure, NOT a major project focus
- **Security:** Spring Security, authentication/authorization, repository isolation, path traversal protection, secret filtering, prompt-injection defenses, unsafe command protection
- **Frontend:** React with SSE for agent status streaming
- **CI/CD:** Git, GitHub, GitHub Actions, Pull Requests, branch protection, automated quality gates
- **Testing:** JUnit 5, Mockito, Testcontainers

**TIER 5 — OPTIONAL / ONLY IF TIME REMAINS**
- **MCP:** Focused MCP server/interface exposing only useful VoxCode capabilities/tools (repository inspection, AST analysis, dependency analysis, retrieval, build, test, verification) - NOT a separate architecture or ecosystem

**EXCLUDED TECHNOLOGIES**
- Redis, Kafka, Kubernetes, Microservices, Distributed service architecture, Complex caching, Complex messaging infrastructure, Custom distributed workflow engines, Huge MCP ecosystem, Multi-agent swarm architecture, Foundation-model training, Custom LLM, Generic multi-language support, Full IDE replacement, Unlimited autonomous coding, Complex voice infrastructure, Arbitrary feature generation

## Project Rules
- **Scope Protection:** VoxCode is a one-year bounded project. No feature creep. No multi-agent architecture (one primary agent only), no general-purpose coding assistants, no graph RAG, Kubernetes, Kafka, Redis, microservices, distributed service architecture, complex caching, complex messaging infrastructure, custom distributed workflow engines, huge MCP ecosystem, foundation-model training, custom LLM, generic multi-language support, full IDE replacement, unlimited autonomous coding, complex voice infrastructure, or arbitrary feature generation without absolute justification.
- **Primary Capabilities:** Investigation and Bounded Remediation are EQUAL, first-class capabilities. Investigation discovers, analyzes and proves repository problems. Remediation applies targeted fixes to confirmed findings.
- **Governing Principle:** Evidence and verification govern remediation. The LLM must NEVER be treated as the source of truth. No remediation is accepted without evidence and verification.
- **Not a Feature Generator:** VoxCode applies safe, targeted fixes for confirmed findings. It does NOT generate arbitrary application features (e.g., "Build an authentication system"). Requests for new features must explicitly be classified as OUT OF SCOPE.
- **Evidence Over Generation:** LLM reasoning must always be supported by deterministic repository intelligence (AST, Graph) and executable verification. RAG must NOT replace AST or graph analysis for structural claims.
- **Agent Traces:** The agent must leave a clear persisted decision trace for investigation (Request → Classification → Hypothesis → Tool → Observation → Evidence → Decision → Finding) and remediation (Finding → Plan → Approval → Transformation → Diff → Verification → Repair → Accept/Rollback). Do NOT expose hidden chain-of-thought.
- **Human Approval:** No code modification may occur without explicit human approval of an engineering plan. Investigation and read-only analysis may occur before approval.
- **Verification Criteria:** A generated patch is NEVER considered successful merely because it looks correct or builds. Remediation is successful ONLY when: intended files were modified, no unauthorized files were modified, diff matches approved plan, project builds successfully, tests pass, static analysis passes, and no unexpected modifications are detected. 
- **Rollback:** Failed remediation must be recoverable through automated rollback. Rollback triggers: verification permanently fails, repair budget is exhausted, unauthorized modifications detected, or remediation violates the approved scope.
- **Database as Supporting Infrastructure:** MySQL/JPA/Hibernate are supporting infrastructure for Agentic AI state persistence, NOT a major project focus. No complex database optimization, advanced SQL engineering, database sharding, complicated caching architecture, or unnecessary database abstractions.
- **MCP as Focused Tool Interface:** MCP is a focused interoperability boundary for selected VoxCode tools (repository inspection, AST analysis, dependency analysis, retrieval, build, test, verification). Do NOT create multiple MCP servers, MCP microservices, dozens of MCP tools, or an MCP ecosystem.
- **Technology Priority Rule:** Every technology must have a clear role in Retrieve → Investigate → Decide → Act → Verify → Evaluate. If a technology does not strengthen this loop, it should NOT be added.

## Remediation Scope
**Supported Scope (IN SCOPE):**
- Localized bug fixes
- Security fixes using existing infrastructure (e.g., missing `@PreAuthorize`)
- Incorrect annotations/configuration
- Deprecated API migrations
- Incorrect method calls
- Transaction-boundary fixes
- Dependency/configuration corrections
- Other small, single-concern, few-file fixes

**Unsupported Scope (OUT OF SCOPE):**
- Creating entire new application features
- Building authentication infrastructure from scratch
- Building new payment systems
- Creating large new modules
- Arbitrary application generation
- Unrestricted autonomous coding

## User Request Classification
Before investigation or remediation begins, VoxCode must classify every developer request as exactly one of:
- `INVESTIGATE`: The user wants explanation/investigation only. Stops after report.
- `REMEDIATE`: The user identifies a specific issue and wants a bounded fix. May skip discovery, but MUST still validate and create a CONFIRMED finding before remediation.
- `INVESTIGATE_AND_REMEDIATE`: The user wants VoxCode to investigate and then fix it. Follows complete lifecycle.
- `OUT_OF_SCOPE`: Requires arbitrary feature generation, large-scale dev, or unrestricted coding. Must be explicitly rejected. Stops after classification.

## Architecture Invariants
- **Backend:** Java 21, Spring Boot, Maven, Spring Security. Modular monolith structure.
- **AI/LLM:** Spring AI, semantic embeddings, Qdrant for vector storage. ONE primary adaptive agent/orchestrator.
- **Intelligence:** JavaParser (AST), JGraphT (Dependency Graph), Repository Index. RAG is for semantic context, not structural truth.
- **RAG:** Hybrid retrieval (vector + lexical + metadata/symbol + dependency-aware), reranking, context assembly. NOT just chunk→embed→top-K.
- **Execution:** Docker-based isolated workspaces for code modification, build, and tests.
- **Frontend:** React-based dashboard (Near-black + Neon green aesthetic).
- **Database:** MySQL for application state (investigations, traces, findings, plans, approvals, reports).
- **MCP:** Focused tool interface for interoperability, NOT a separate architecture or ecosystem.

## Definition of Done
Every task is only "Done" when:
- Code is implemented according to the locked architectural pattern.
- The feature is fully tested (Unit and/or Integration tests).
- Automated CI pipeline passes successfully.
- For AI features, deterministic fallback and error handling are present.
- Any architectural decisions are documented in an ADR.

## VoxCode Product Development Workflow
This describes the phased development approach for building the VoxCode software itself. See "Cross-Cutting Requirements — Professional Git & GitHub Development Workflow" for the Git/GitHub practices developers must follow while implementing these phases.

1. Start with foundation and repository ingestion.
2. Build intelligence layers (AST, Graph, Repository Index).
3. Develop hybrid RAG with retrieval evaluation.
4. Develop Agent Tools.
5. Implement focused MCP tool interface.
6. Implement Adaptive Investigation Agent and Evidence Engine.
7. Create Planning and Human Approval layers.
8. Implement Bounded Remediation and Execution/Verification layers.
9. Connect the Frontend UI.
10. Continuously measure with Evaluation/Benchmark tests.
11. Release.

---

## Phase 0 — Repository/Project Setup

### [x] VXC-001 — Initialize backend repository
Priority: P0
Phase: 0
Dependencies: None

Goal:
Initialize the production backend repository structure and base configuration.

Tasks:
- Initialize Git repository with branch strategy (main, develop, feature/*, fix/*, hotfix/*).
- Create standard `.gitignore` for Java/Spring/React (exclude build artifacts, .env files, IDE files).
- Configure Java 21 and Maven.
- Initialize Spring Boot 3.x project with required dependencies (Web, Data JPA, Security).
- Establish codebase structure (repository, intelligence, agent, remediation, execution, common).
- Setup editorconfig and code style checks (e.g., Checkstyle, Spotless).
- Refer to "Cross-Cutting Requirements — Professional Git & GitHub Development Workflow" for complete Git workflow requirements.

Deliverables:
- Initialized Spring Boot repository with correct module structure.

Definition of Done:
- The backend application builds successfully using `mvn clean install` on Java 21.

Tests:
- Application context loads test passes.

CI:
- None yet.

### [x] VXC-002 — Establish local environment
Priority: P0
Phase: 0
Dependencies: VXC-001

Goal:
Set up local infrastructure requirements via Docker Compose.

Tasks:
- Create `docker-compose.yml` containing MySQL and Qdrant.
- Add local development environment variables (`.env`).
- Document local setup instructions in `README.md`.

Deliverables:
- Working `docker-compose.yml` and `README.md`.

Definition of Done:
- A developer can run `docker-compose up` and start MySQL and Qdrant locally.

Tests:
- N/A

CI:
- N/A

---

## Phase 1 — Backend Engineering Foundation

### [x] VXC-010 — Configuration Management and Logging
Priority: P0
Phase: 1
Dependencies: VXC-001

Goal:
Set up robust configuration, profiles, and logging.

Tasks:
- Define Spring profiles (`local`, `test`, `prod`).
- Set up SLF4J/Logback configuration with structured logging for observability.
- Configure global exception handler and standardized API error responses.
- Add health and basic actuator endpoints.

Deliverables:
- Standardized API response wrappers, global exception handler, logback.xml.

Definition of Done:
- API exceptions return consistent JSON errors. Logs contain structured correlation IDs.

Tests:
- Unit tests for global exception handler.

CI:
- Basic build check.

### [x] VXC-011 — Database and Migrations
Priority: P0
Phase: 1
Dependencies: VXC-002, VXC-010

Goal:
Establish database schema management as supporting infrastructure for Agentic AI system state.

Tasks:
- Integrate Flyway or Liquibase for database migrations.
- Define initial schemas for Users, Repositories, Investigations, Agent Traces, Findings, Evidence, Plans, Approvals, Executions, Verification Results, and Evaluation Results.
- Configure Spring Data JPA entities for persistence.
- Keep database design simple and focused on supporting the Agentic AI workflow (no complex optimization, sharding, or advanced SQL engineering).

Deliverables:
- Migration scripts and base JPA entities for Agentic AI state persistence.

Definition of Done:
- Application starts and successfully applies migrations to the MySQL container. Database schema supports the core Agentic AI workflow without unnecessary complexity.

Tests:
- Repository layer integration tests using Testcontainers (MySQL).

CI:
- N/A

---

## Phase 2 — CI/CD

### [x] VXC-020 — GitHub Actions Core Pipeline
Priority: P0
Phase: 2
Dependencies: VXC-011

Goal:
Ensure quality gates are enforced automatically via GitHub Actions CI pipeline.

Tasks:
- Create `build.yml` GitHub Actions workflow.
- Configure Maven compilation and unit tests on PRs.
- Add integration test execution using service containers (MySQL, Qdrant).
- Add static analysis (SpotBugs/Checkstyle).
- Configure branch protection rules (main, develop) requiring CI checks before merge.
- Refer to "Cross-Cutting Requirements — Professional Git & GitHub Development Workflow" for complete workflow details.

Deliverables:
- GitHub Actions configuration files.
- Branch protection rules configured in GitHub repository settings.

Definition of Done:
- PRs to main/develop require passing CI pipeline.
- Branch protection rules prevent direct pushes to main/develop.
- All required status checks must pass before merge.

Tests:
- CI runs all existing unit/integration tests.

CI:
- Triggers on push and PR to enforce quality gates.

---

## Phase 3 — Repository Ingestion/Workspaces

### [x] VXC-030 — Repository Ingestion & Validation
Priority: P0
Phase: 3
Dependencies: VXC-011

Goal:
Allow VoxCode to ingest and validate target Java/Spring repositories.

Tasks:
- Implement `RepositoryService` for handling local and Git-based repositories.
- Validate repository structure (pom.xml, src/main/java).
- Implement Git clone and checkout capabilities.
- Define internal workspace path management (isolated temporary directories).

Deliverables:
- API endpoint to register and clone a repository.

Definition of Done:
- System can successfully clone a public GitHub repository, detect Java/Spring configuration, and record it in MySQL.

Tests:
- Integration tests cloning a local fixture repository.

CI:
- Pipeline includes repository validation tests.

---

## Phase 4 — AST Intelligence

### [x] VXC-040 — JavaParser Integration
Priority: P0
Phase: 4
Dependencies: VXC-030

Goal:
Extract deterministic structural intelligence from Java repositories.

Tasks:
- Integrate JavaParser library.
- Implement AST visitors to discover: Classes, Interfaces, Methods, Fields, Annotations, Imports.
- Persist AST metadata (or build an in-memory structural index queryable by the agent).
- Support finding implementations and method calls via static analysis.

Deliverables:
- `AstAnalysisService` exposing deterministic queries.

Definition of Done:
- Service can correctly identify all `@RestController` and `@Service` classes, their methods, and annotations in a sample repository.

Tests:
- Unit tests for AST visitors against a complex Java fixture.

CI:
- Runs AST tests.

---

## Phase 5 — Dependency Graph

### [x] VXC-050 — JGraphT Integration
Priority: P0
Phase: 5
Dependencies: VXC-040

Goal:
Construct a dependency graph of repository components.

Tasks:
- Integrate JGraphT.
- Map AST results (caller/callee, class dependencies, interfaces) to graph nodes and edges.
- Implement graph traversal algorithms (impact analysis, finding dependencies of a component).

Deliverables:
- `DependencyGraphService` enabling traversal queries.

Definition of Done:
- A query for "what components does PaymentController depend on" returns the correct Service and Repository nodes.

Tests:
- Graph construction and traversal tests against a fixture repository.

CI:
- Runs graph tests.

---

## Phase 6 — Repository Index

### [x] VXC-060 — Structural and Semantic Indexing Layer
Priority: P0
Phase: 6
Dependencies: VXC-050

Goal:
Provide a unified querying interface over AST and Graph for agent tools.

Tasks:
- Create `RepositoryIndexService` that aggregates AST and Graph outputs.
- Define DTOs representing indexed repository state.
- Expose internal APIs for searching classes, methods, and relationships.

Deliverables:
- Unified repository querying layer.

Definition of Done:
- The system correctly separates structural queries from graph queries, returning unified responses.

Tests:
- Integration tests across AST and Graph using `RepositoryIndexService`.

CI:
- Test execution.

---

## Phase 7 — Hybrid Repository-Aware RAG

### [x] VXC-070 — RAG Pipeline Setup
Priority: P1
Phase: 7
Dependencies: VXC-002, VXC-060

Goal:
Implement deep hybrid retrieval for repository semantic context. NOT just chunk→embed→top-K.

Tasks:
- Configure Spring AI with an Embedding Model.
- Implement document parsing and chunking strategy (Markdown, comments, code blocks).
- Configure Qdrant vector store integration.
- Store vectors with rich metadata (repositoryId, documentType, symbolInfo, filePath, className, methodName).
- Implement hybrid retrieval combining:
  - Vector retrieval (semantic similarity)
  - Lexical retrieval (keyword/BM25)
  - Metadata/symbol retrieval (exact matches on classes, methods, annotations)
  - Dependency-aware retrieval (context from related components)
- Implement reranking (cross-encoder or similar) to improve relevance.
- Implement context assembly that balances multiple retrieval sources.
- Support retrieval for: Documentation, README, Comments, Similar implementations, Related classes/methods, Tests, Configuration, Error handling patterns, Cross-file semantic context.

Deliverables:
- `RagService` for embedding, hybrid retrieval, reranking, and context assembly.

Definition of Done:
- System chunks and embeds repository content, stores with rich metadata in Qdrant, and retrieves relevant context via hybrid retrieval with reranking and context assembly.

Tests:
- Integration tests with local Qdrant container for each retrieval source.
- Reranking effectiveness tests.

CI:
- Tests run against Testcontainers Qdrant.

---

## Phase 8 — RAG Evaluation

### [x] VXC-080 — Retrieval Benchmarking
Priority: P1
Phase: 8
Dependencies: VXC-070

Goal:
Prove the value of RAG through quantitative metrics and ablation study.

Tasks:
- Create a benchmark dataset of expected semantic queries vs ground-truth chunks.
- Implement an evaluation script measuring:
  - Recall@K
  - Precision@K
  - MRR (Mean Reciprocal Rank)
  - NDCG (Normalized Discounted Cumulative Gain)
  - Retrieval relevance
  - Retrieval latency
  - Context relevance
  - Token efficiency
  - Investigation success
- Set up an ablation study comparing:
  - Baseline: AST + Dependency Graph + Agent
  - Full: AST + Dependency Graph + RAG + Agent
- Demonstrate whether RAG actually improves repository investigation.

Deliverables:
- Benchmark execution suite with quantitative metrics.
- Ablation study results showing RAG impact.

Definition of Done:
- Running the benchmark outputs comprehensive metrics and ablation results demonstrating RAG's actual value.

Tests:
- RAG evaluation tests exist in a separate suite.

CI:
- Manual or scheduled execution in CI.

---

## Phase 9 — Agent Tools

### [x] VXC-090 — Core Agent Tools
Priority: P0
Phase: 9
Dependencies: VXC-060, VXC-070, VXC-080

Goal:
Expose deterministic capabilities as typed tools for the LLM.

Tasks:
- Build Spring AI Functions/Tools for read-only investigation:
  - `findClass`, `findMethod`, `findAnnotation` (from AST).
  - `findDependencies`, `impactAnalysis` (from Graph).
  - `searchSemanticContext` (from RAG).
  - `readFile`, `listFiles` (Repository).
- Enforce strict READ permissions for these tools. Investigation tools MUST NOT modify source code.
- Validate input/output schemas for the tools.

Deliverables:
- Spring AI `@Bean` functions for tools.

Definition of Done:
- The tools can be invoked independently, have strict input/output schemas, and handle errors gracefully.

Tests:
- Unit tests for tool wrappers, verifying schemas and error handling.

CI:
- Standard CI tests.

---

## Phase 10 — MCP Tool Interface

### [ ] VXC-095 — Focused MCP Server (OPTIONAL)
Priority: P2
Phase: 10
Dependencies: VXC-090

Goal:
Implement a focused MCP interface for tool interoperability. MCP is a tool interface, NOT a separate architecture. This is OPTIONAL and should only be implemented if time remains after core Agentic AI, RAG, and Verification capabilities are complete.

Tasks:
- Implement a focused MCP server exposing only useful VoxCode capabilities.
- Define MCP tools for:
  - Repository search
  - File inspection
  - AST analysis
  - Dependency analysis
  - Semantic retrieval
  - Build
  - Test
  - Verification
- Ensure MCP tools are bounded and safe.
- Do NOT create a large MCP ecosystem, dozens of servers, or MCP-based microservices.
- Use MCP only where it provides genuine architectural value for interoperability.

Deliverables:
- Focused MCP server with bounded tool interface.

Definition of Done:
- MCP server exposes defined tools safely and integrates with VoxCode's agent tools layer without creating architectural bloat.

Tests:
- Integration tests for MCP tool invocation.

CI:
- Included.

---

## Phase 11 — Adaptive Investigation Agent

### [x] VXC-100 — Core Investigation Loop
Priority: P0
Phase: 11
Dependencies: VXC-090

Goal:
Implement lightweight classification and the Request → Hypothesis → Tool → Observation → Evidence → Decision → Finding loop.

Tasks:
- Implement orchestrator using ONE primary Spring AI agent. Do NOT implement a multi-agent system.
- **Implement request classification** (`INVESTIGATE`, `REMEDIATE`, `INVESTIGATE_AND_REMEDIATE`, `OUT_OF_SCOPE`) as the very first step.
- Explicitly reject `OUT_OF_SCOPE` feature requests (stop). For `INVESTIGATE`, stop after finding. For `REMEDIATE`, validate and confirm finding even if discovery is skipped.
- Define structured agent state and decision traces in MySQL.
- Implement a loop that forces the LLM to output structured `InvestigationDecision` objects (action, tool, reason, confidence) without exposing hidden chain-of-thought.
- The agent must be able to determine that current evidence is insufficient and request additional retrieval/tool calls.
- The agent must genuinely adapt based on tool observations.
- Persist the investigation trace to the database for observability.

Deliverables:
- `InvestigationAgent` service capable of request classification and multi-step tool execution.

Definition of Done:
- Agent classifies requests, validates them, explicitly rejects out-of-scope requests, answers a repository question by calling tools sequentially, persists its structural trace, and stops when sufficient evidence is reached.

Tests:
- Mocked LLM tests to verify classification, loop, state transitions, trace generation, and out-of-scope rejection.

CI:
- Tests run in CI.

---

## Phase 12 — Agent Evaluation

### [x] VXC-110 — Agent Benchmark
Priority: P1
Phase: 12
Dependencies: VXC-100

Goal:
Measure the agent's investigation capabilities independently from remediation.

Tasks:
- Create real Java/Spring issue cases (e.g., missing @PreAuthorize).
- Run the agent against these cases and measure investigation metrics: Finding Precision, Finding Recall, F1, Evidence validity, Tool-call efficiency, Investigation success rate, Investigation trace quality.
- Verify that agent behavior adapts based on tool output.

Deliverables:
- Agent evaluation test suite.

Definition of Done:
- Benchmark suite produces measurable independent metrics for investigation success and efficiency.

Tests:
- End-to-end evaluation tests on fixture repositories.

CI:
- Manual/Scheduled execution.

---

## Phase 13 — Evidence Engine

### [x] VXC-120 — Structured Evidence Model
Priority: P0
Phase: 13
Dependencies: VXC-110

Goal:
Link LLM findings to deterministic repository artifacts.

Tasks:
- Create the `Finding` and `Evidence` entities (MySQL).
- Implement logic in the agent to attach AST/Graph/RAG tool results as formal evidence for its conclusions.
- Output findings in a strict JSON schema containing at minimum: repository, file path, line range, class/method, issue type, severity, evidence references, evidence source, and validation status.

Deliverables:
- `EvidenceEngine` service and schema validation.

Definition of Done:
- When the agent claims an issue exists, the resulting `Finding` object contains verifiable AST nodes, file paths, and line ranges.

Tests:
- Unit tests for evidence schema generation.

CI:
- Included.

---

## Phase 14 — Finding Validation

### [x] VXC-130 — Evidence Verification
Priority: P0
Phase: 14
Dependencies: VXC-120

Goal:
Reject hallucinations by validating evidence deterministically. Only CONFIRMED findings proceed to remediation.

Tasks:
- Implement a `FindingValidator` that independently verifies that cited repository artifacts (AST nodes, file paths, etc.) exist.
- Update finding statuses (`CONFIRMED`, `REJECTED`, `INSUFFICIENT_EVIDENCE`).

Deliverables:
- Validation layer for findings.

Definition of Done:
- A finding referencing a non-existent class or method is automatically marked as `REJECTED`. Only a `CONFIRMED` finding can progress.

Tests:
- Tests for positive and negative validation scenarios.

CI:
- Included.

---

## Phase 15 — Engineering Planning

### [x] VXC-140 — Plan Generation
Priority: P0
Phase: 15
Dependencies: VXC-130

Goal:
Create a human-readable, actionable remediation plan from a finding that explicitly connects investigation to remediation.

Tasks:
- Implement `PlanGenerationService`.
- Ensure the plan is ALWAYS generated from a `CONFIRMED` finding, NEVER directly from an arbitrary user request.
- The plan must contain: Finding, Root cause, Affected files, Affected classes/methods, Proposed change, Transformation strategy, Risk, Expected behavior, Verification strategy, Rollback strategy, and Scope boundaries (what is allowed to change).
- Persist the `EngineeringPlan` to the database.

Deliverables:
- Structured Engineering Plan generation and persistence.

Definition of Done:
- A confirmed finding is converted into a detailed plan stating exactly what files will change, the root cause, scope boundaries, and how to verify and rollback changes.

Tests:
- Unit tests for plan structure.

CI:
- Included.

---

## Phase 16 — Human Approval

### [x] VXC-150 — Approval Gate
Priority: P0
Phase: 16
Dependencies: VXC-140

Goal:
Prevent any code modification without explicit user consent.

Tasks:
- Implement API endpoints to review, approve, or reject an `EngineeringPlan`.
- Create the `Approval` entity linking the user, plan, and timestamp.
- The remediation engine MUST verify: plan exists, plan is valid, finding is CONFIRMED, approval exists, approval belongs to the current plan, repository/workspace matches the plan context.
- Block the remediation pipeline if ANY of these conditions fail.

Deliverables:
- Approval workflow API and state enforcement.

Definition of Done:
- Remediation attempts fail immediately with an authorization error if the linked plan is not explicitly approved, or if the repository context does not match.

Tests:
- Integration tests verifying the approval state machine and security checks.

CI:
- Included.

---

## Phase 17 — Bounded Remediation

### [x] VXC-160 — Safe Code Modification
Priority: P0
Phase: 17
Dependencies: VXC-150

Goal:
Apply approved fixes safely as a core, first-class product capability.

Tasks:
- Implement `RemediationService` capable of applying changes.
- Prioritize transformations in this exact order:
  1. Deterministic transformation using OpenRewrite when appropriate for Java/Spring transformations
  2. AST-aware transformation
  3. AI-generated targeted patch only when deterministic transformation is insufficient
- Never use unrestricted LLM text replacement as the default mechanism.
- Every modification MUST: operate only inside the approved workspace, remain within approved files/scope, produce a diff, preserve unrelated code, and be traceable to the plan.
- The remediation engine must detect unexpected modifications. If the actual diff contains files or changes outside the approved scope -> FAIL, do NOT accept, ROLLBACK.
- Persist the remediation trace (Finding → Plan → Approval → Transformation → Diff → Verification → Repair → Accept/Rollback).

Deliverables:
- Service that modifies repository files securely and outputs unified diffs.

Definition of Done:
- The service can successfully inject a `@PreAuthorize` annotation into a specific controller method using AST modification, outputting a clear diff. Scope violations are rejected and rolled back.

Tests:
- AST transformation tests; AI patch application tests; Scope violation tests.

CI:
- Included.

---

## Phase 18 — Docker Sandbox

### [x] VXC-170 — Isolated Workspace Execution
Priority: P0
Phase: 18
Dependencies: VXC-160

Goal:
Run the modified code in a secure, isolated environment before accepting it.

Tasks:
- Integrate Docker Engine API (e.g., via docker-java).
- Implement logic to spin up a temporary container (JDK + Maven), copy the modified workspace, and prepare for execution.
- Set strict resource limits (timeout, memory, CPU/resource limits, restricted filesystem access, controlled network access).

Deliverables:
- `ExecutionSandboxService`.

Definition of Done:
- VoxCode can spawn a container, safely copy the codebase, execute `java -version` inside it under strict resource limits, and destroy the container.

Tests:
- Sandbox integration tests checking limits and execution.

CI:
- Requires Docker-in-Docker or Testcontainers in CI.

---

## Phase 19 — Build/Test/Static Verification

### [x] VXC-180 — Executable Verification
Priority: P0
Phase: 19
Dependencies: VXC-170

Goal:
Verify that the remediation actually works. Remediation is only successful when ALL gates pass.

Tasks:
- Execute `mvn clean verify` (or compile + test) inside the Docker sandbox.
- Capture stdout, stderr, exit codes, and test result XMLs.
- Determine final status. Remediation succeeds ONLY if ALL required verification gates pass:
  1. Intended files were modified.
  2. No unauthorized files were modified.
  3. Actual diff matches the approved Engineering Plan.
  4. Project compilation/build succeeds.
  5. Tests pass.
  6. Required static analysis passes.
  7. No unexpected repository modifications exist.

Deliverables:
- Verification pipeline.

Definition of Done:
- Only passing all 7 gates results in ACCEPTED. A change that builds but modifies an unauthorized file results in FAILED.

Tests:
- End-to-end tests running against broken and fixed Java fixtures, including scope violation attempts.

CI:
- Included.

---

## Phase 20 — Limited Repair

### [x] VXC-190 — Bounded Repair Loop (COMPLETED)
Priority: P1
Phase: 20
Dependencies: VXC-180

Goal:
Attempt to fix small verification errors without infinite looping.

Tasks:
- Parse verification failures (compiler errors, failing tests). Classify the failure.
- Provide ONLY relevant failure evidence back to the Agent to propose a targeted fix.
- Apply the correction ONLY within the approved scope.
- Re-run verification.
- Enforce a strict retry budget (maximum 2 attempts).
- The repair mechanism MUST NOT expand the scope of the original Engineering Plan. If a repair requires a new file or scope expansion → STOP, ROLLBACK.

Deliverables:
- Repair loop logic in the execution orchestrator.

Definition of Done:
- The system attempts to fix a minor compilation error, succeeds on retry, and stops attempting if it requires a new file or fails 2 times.

Tests:
- Mocked LLM tests to verify the retry counter and scope-expansion rejection.

CI:
- Included.

**Implementation Summary:**
- Created RepairLoopService with bounded repair loop logic
- Implemented failure classification (COMPILER_ERROR, TEST_FAILURE, STATIC_ANALYSIS, SCOPE_VIOLATION, UNKNOWN)
- Enforced strict retry budget (MAX_RETRY_ATTEMPTS = 2)
- Implemented scope expansion detection and rejection
- Added rollback path for scope violations and budget exhaustion
- Created 47 unit tests covering all repair loop scenarios
- All tests passing successfully

---

## Phase 21 — Rollback

### [x] VXC-200 — State Preservation and Recovery (COMPLETED)
Priority: P0
Phase: 21
Dependencies: VXC-190

Goal:
Ensure failed remediations don't corrupt the repository.

Tasks:
- Take a Git snapshot or copy the workspace before remediation begins.
- Automatically restore the snapshot if ANY of the following occur:
  - Verification permanently fails
  - Repair budget is exhausted
  - Unauthorized modifications are detected
  - Remediation violates the approved scope
- Verify restoration.
- Record the rollback action in the execution history and mark remediation as `ROLLED_BACK`.

**Implementation Summary:**
- Created RollbackService with comprehensive state preservation and recovery
- Implemented workspace snapshot creation with SHA-256 checksums for all files
- Captured Git commit SHA and branch information when available
- Stored snapshots in configurable base directory (/tmp/voxcode/snapshots by default)
- Implemented rollback trigger detection for:
  - Unauthorized modifications (unauthorized files, unexpected modifications)
  - Scope violations (diff mismatch, remediation outside scope, scope expansion rejected)
  - Repair budget exceeded
  - Verification permanent failure (max retries reached or retry failed)
- Implemented workspace restoration with:
  - Deletion of rogue files created during failed remediation
  - Restoration of all original files from snapshot
  - SHA-256 checksum verification of restored files
  - Detection of unexpected files remaining after rollback
- Integrated with RemediationService to snapshot before remediation
- Integrated with RepairLoopService to trigger rollback on scope violation
- Record rollback in Remediation entity (status ROLLED_BACK, rollbackInfo JSON)
- Record rollback in Execution entity (status ROLLED_BACK, execution log entry)
- Snapshot cleanup on successful remediation completion
- 18 unit tests covering trigger detection, snapshot creation, rollback execution, restoration verification, and cleanup
- All tests passing successfully

Deliverables:
- `RollbackService`.

Definition of Done:
- A failed verification with no remaining retries results in the workspace being completely restored, verified, and recorded.

Tests:
- File system assertion tests verifying rollback triggers and execution.

CI:
- Included.

---

## Phase 22 — Engineering Report

### [x] VXC-210 — Report Generation
Priority: P0
Phase: 22
Dependencies: VXC-200

Goal:
Provide a comprehensive summary of the entire lifecycle covering BOTH traces.

Tasks:
- Aggregate the full Investigation trace (Request → ... → Finding) and the full Remediation trace (Finding → ... → Accept/Rollback).
- Generate a structured JSON (and markdown) Engineering Report containing both traces.

Deliverables:
- `ReportGenerationService`.

Definition of Done:
- The user can retrieve a single document containing the complete lifecycle traces from request to final verified status or rollback.

Tests:
- Unit tests verifying aggregation logic.

CI:
- Included.

**Implementation Summary:**
- Verified comprehensive ReportGenerationService implementation with full lifecycle aggregation
- Investigation Trace: Agent steps, findings, and supporting evidence aggregated from repositories
- Remediation Trace: Plans, approvals, remediations, executions, verifications, and rollbacks aggregated
- Overall lifecycle status determination based on verification results, rollback status, and remediation status
- Executive summary generation tailored to overall status (VERIFIED, ROLLED_BACK, VERIFICATION_FAILED, FINDINGS_CONFIRMED, etc.)
- Markdown report formatting with structured sections for investigation and remediation traces
- EngineeringReportDTO with nested DTOs for InvestigationTrace, RemediationTrace, AgentStep, Finding, Evidence, PlanLifecycle, Approval, Remediation, Execution, Verification, and RollbackSummary
- Fixed entity field mismatches (url vs remoteUrl, content vs codeSnippet/description, risk vs riskScore)
- 12 unit tests covering report generation scenarios
- All tests passing successfully

---

## Phase 23 — Observability

### [ ] VXC-220 — Telemetry and Metrics
Priority: P1
Phase: 23
Dependencies: VXC-210

Goal:
Make the agent and system inspectable and measurable.

Tasks:
- Add Micrometer/Prometheus metrics for tool latency, LLM latency, token usage, and build durations.
- Add structured logging events for every major state transition.
- Persist agent traces for later UI consumption.

Deliverables:
- Prometheus `/metrics` endpoint and rich logging.

Definition of Done:
- Observability tools can track the average time spent in the Verification phase.

Tests:
- N/A

CI:
- N/A

---

## Phase 24 — Security Hardening

### [ ] VXC-230 — System Security Hardening
Priority: P0
Phase: 24
Dependencies: VXC-001

Goal:
Protect the system and repositories. Treat repositories as untrusted input.

Tasks:
- Implement JWT/OAuth authentication for the API.
- Ensure strict authorization (users can only access their own repositories).
- Protect against path traversal, malicious repository content, prompt injection from repository files, secret leakage, unauthorized access/modification, and unsafe command execution.
- Implement secret redaction for context sent to the LLM (e.g., filter out API keys; never send secrets into the LLM context).

Deliverables:
- Hardened Spring Security configuration, sanitizers, and prompt-injection defense.

Definition of Done:
- An API request to read a file outside the workspace (`../../etc/passwd`) is blocked. LLM context strips `.env` secrets.

Tests:
- Security unit tests, prompt injection tests, and path traversal attempts.

CI:
- Included.

---

## Phase 25 — Frontend

### [ ] VXC-240 — UI Foundation and Repository Overview
Priority: P0
Phase: 25
Dependencies: VXC-030, VXC-060

Goal:
Build the React UI base and repository connection.

Tasks:
- Set up React with standard routing, dark/neon-green premium developer theme.
- Build Landing, Connect Repository, and Repository Overview screens.
- Connect to backend REST APIs for repository ingestion status.

### [ ] VXC-241 — Investigation and Trace UI
Priority: P0
Phase: 25
Dependencies: VXC-100, VXC-120

Goal:
Visualize the agent in action.

Tasks:
- Implement Chat/Q&A interface.
- Build the Investigation Workspace showing live SSE updates of the agent trace, hypotheses, tool calls, evidence, and findings.

### [ ] VXC-242 — Planning and Approval UI
Priority: P0
Phase: 25
Dependencies: VXC-150

Goal:
Implement the safety gate in the UI.

Tasks:
- Build the Engineering Plan view (affected files, risks).
- Add explicit Approve/Reject buttons connected to the API.

### [ ] VXC-243 — IDE-like Remediation & Verification UI
Priority: P0
Phase: 25
Dependencies: VXC-180, VXC-200

Goal:
Display execution results natively. Expose Remediation capabilities natively.

Tasks:
- Build IDE-like explorer for displaying engineering plans, approvals, proposed diffs, applied diffs, verification status, repair attempts, rollbacks, and the final engineering report.
- Visualize Repair and Rollback states securely via SSE.
- Do NOT turn VoxCode into a full IDE replacement (IntelliJ/VS Code). Keep it focused on the remediation flow.

---

## Phase 26 — End-to-End Integration

### [ ] VXC-250 — Complete E2E Flows
Priority: P0
Phase: 26
Dependencies: VXC-243

Goal:
Ensure all components work together seamlessly covering equal-priority flows.

Tasks:
- Test Flow A: Repository → Analysis → Q&A
- Test Flow B: Repository → Investigation → Finding
- Test Flow C: Finding → Plan → Approval → Remediation → Verification → Success
- Test Flow D: Remediation → Verification Failure → Limited Repair → Success
- Test Flow E: Remediation → Verification Failure → Repair Failure → Rollback
- Test Flow F: Developer Request → Investigation → Confirmed Finding → Plan → Approval → Remediation → Verification → Report
- Test Flow G: Out-of-Scope Feature Request → Classification → Explicit Rejection

Deliverables:
- Stable system capable of running all core workflows.

Definition of Done:
- A user can click through the entire UI to execute Flow F successfully, and OUT_OF_SCOPE triggers Flow G.

Tests:
- Cypress or Playwright E2E tests for the frontend.

CI:
- E2E tests run on release branches.

---

## Phase 27 — Final Evaluation

### [ ] VXC-260 — Final Benchmark Execution
Priority: P1
Phase: 27
Dependencies: VXC-250

Goal:
Produce final quality metrics for BOTH capabilities independently.

Tasks:
- Run the full evaluation suite.
- Measure Investigation metrics independently: Finding Precision, Finding Recall, F1, Evidence Validity, Tool-call efficiency, Investigation Success Rate, Trace Quality.
- Measure Remediation metrics independently: Fix Success Rate, Verification Pass Rate, Regression Rate, Repair Success Rate, Rollback Rate, Patch Correctness, Scope Compliance.
- Do NOT combine these into one vague metric. Document the results in the final architecture report.

Deliverables:
- Comprehensive Evaluation Report.

---

## Phase 28 — Release Engineering

### [ ] VXC-270 — Final Polish and Documentation
Priority: P0
Phase: 28
Dependencies: VXC-260

Goal:
Prepare the project for final presentation/release.

Tasks:
- Clean up `README.md` with setup instructions, architecture diagrams, and usage examples.
- Ensure all ADRs (Architecture Decision Records) are up to date.
- Verify Docker compose starts cleanly on a fresh machine.
- Prepare a Demo Repository with known issues to demonstrate VoxCode's investigation and bounded remediation capabilities.
- Record demo scenarios.

Deliverables:
- Final v1.0 release tag.

---

## Cross-Cutting Requirements

### Testing
- Maintain a clear separation between unit tests (fast, mocked LLM) and integration tests (Docker, DB, Qdrant, real LLM).
- Use real Java/Spring fixture repositories for intelligence and remediation testing.

### Security
- Repositories are untrusted input. Prevent prompt injection from repository documentation. Never send secrets to the LLM.
- Execution workspaces must be heavily sandboxed (Docker constraints on timeout, memory, CPU, FS, and network).

### Observability
- Keep agent traces persisted in the DB for debugging and visualization.
- Ensure the UI streams agent status via SSE so the user never sees a stalled "Working..." state.

### Documentation
- Maintain ADRs for major decisions (e.g., "Why we used JavaParser over regex", "Why we don't use GraphRAG").

### CI/CD
- GitHub Actions enforces quality gates (Unit, Integration, Static Analysis checks).
- Protect `main` and `develop` branches via branch protection rules.
- All PRs must pass required CI checks before merging.
- See "Cross-Cutting Requirements — Professional Git & GitHub Development Workflow" for complete workflow details.

### MCP Integration
- MCP is a focused tool interface for interoperability, NOT a separate architecture or ecosystem.
- Implement only useful VoxCode capabilities as MCP tools (repository search, file inspection, AST analysis, dependency analysis, semantic retrieval, build, test, verification).
- Do NOT create large MCP server ecosystems, dozens of servers, MCP microservices, or MCP-based microservices.
- Use MCP only where it provides genuine architectural value for tool interoperability.
- MCP is TIER 5 (OPTIONAL) - only implement if time remains after core Agentic AI, RAG, and Verification capabilities are complete.

### Technology Scope Protection
- **EXCLUDED:** Kafka, Kubernetes, Redis, Microservices, Distributed service architecture, Complex caching, Complex messaging infrastructure, Custom distributed workflow engines, Huge MCP ecosystem, Multi-agent swarm architecture, Foundation-model training, Custom LLM, Generic multi-language support, Full IDE replacement, Unlimited autonomous coding, Complex voice infrastructure, Arbitrary feature generation.
- **Database Constraint:** MySQL/JPA/Hibernate are supporting infrastructure for Agentic AI state persistence. No complex database optimization, advanced SQL engineering, database sharding, complicated caching architecture, or unnecessary database abstractions.
- **MCP Constraint:** MCP is a focused tool interface for interoperability. Do NOT create multiple MCP servers, MCP microservices, dozens of MCP tools, or an MCP ecosystem. Use MCP only where it provides genuine architectural value for tool interoperability.
- **Every technology must have a direct role in:** Retrieve → Investigate → Decide → Act → Verify → Evaluate.
- **Prioritize:** Completed + Reliable + Measurable + Defensible over Large + Feature-heavy + Unfinished.
- **Do not add a feature unless it strengthens:** Evidence → Decision → Action → Verification → Evaluation.

### Evaluation
- Continuous focus on proving the value of RAG and the Agent via ablation and benchmarking. Separate investigation metrics from remediation metrics.
- RAG evaluation must include quantitative metrics (Recall@K, Precision@K, MRR, NDCG, retrieval relevance, latency, context relevance, token efficiency, investigation success).
- Ablation study comparing AST+Dependency Graph+Agent vs AST+Dependency Graph+RAG+Agent to demonstrate RAG's actual value.

### Intelligence Boundaries
- **AST = Structural Truth**: JavaParser provides deterministic structural analysis (classes, methods, annotations, imports).
- **JGraphT = Relationship Truth**: Dependency graph provides deterministic relationship analysis (caller/callee, class dependencies, interfaces).
- **RAG = Semantic/Contextual Information**: Hybrid retrieval provides semantic context, documentation, similar implementations, cross-file relationships.
- **Tools = Operational Truth**: Agent tools provide operational capabilities (read files, execute builds, run tests).
- **Build/Tests = Behavioral Truth**: Execution verification provides behavioral validation.
- **Critical Boundary**: RAG must NEVER replace AST or dependency analysis for structural claims. Use the right tool for the right truth.

---

## Cross-Cutting Requirements — Professional Git & GitHub Development Workflow

### Branching Strategy

The project uses a GitFlow-inspired branching strategy:

- `main` → stable/release-ready code
- `develop` → integration branch for upcoming releases
- `feature/*` → new feature/task implementation branches
- `fix/*` → bug fix branches
- `hotfix/*` → critical fixes when required

**Rule:** Do not directly develop on `main` or `develop`. All development must occur on feature/fix branches.

### Standard Developer Workflow

For every VXC task, developers must follow this workflow:

1. Start from the latest `develop` branch.
2. Pull the latest remote changes: `git pull origin develop`.
3. Create a dedicated feature/fix branch: `git checkout -b feature/VXC-XXX-description` or `git checkout -b fix/VXC-XXX-description`.
4. Implement only the assigned VXC task and its required dependencies.
5. Write/update tests for the implementation.
6. Run relevant local validation (compilation, unit tests).
7. Run `mvn clean verify` or the appropriate project validation command.
8. Review `git status` to ensure no unrelated files are staged.
9. Review `git diff` to verify changes match the task scope.
10. Ensure no secrets, `.env` files, or build artifacts are included.
11. Commit using a meaningful commit message (see Commit Requirements).
12. Push the branch to GitHub: `git push origin feature/VXC-XXX-description`.
13. Create a Pull Request targeting `develop`.
14. Allow GitHub Actions to execute CI pipeline.
15. Fix any CI failures before proceeding.
16. Review the Pull Request (self-review or peer review).
17. Merge only after all required checks pass.
18. Delete the feature branch after successful merge.

### Commit Requirements

All commits must follow professional standards:

- Commits should represent logical units of work.
- Use meaningful, descriptive commit messages.
- Do not commit secrets, API keys, or credentials.
- Do not commit `.env` files containing sensitive configuration.
- Do not commit build artifacts (e.g., `target/`, `node_modules/`, `.class` files).
- Do not commit unrelated changes.
- Keep commits reasonably focused and atomic.

**Commit Message Format Examples:**

```
feat: implement repository ingestion service
fix: prevent workspace path traversal vulnerability
test: add AST annotation analysis unit tests
docs: add ADR for sandbox verification gates
refactor: extract evidence validation logic
chore: upgrade Spring Boot to 3.2.0
```

### Pull Request Requirements

Every Pull Request must contain:

- **VXC task ID(s)** in the PR title (e.g., `VXC-010: Configuration Management and Logging`)
- **Summary of implementation** describing what was done and why
- **Tests performed** documenting test coverage and results
- **CI status** confirming all checks pass
- **Architectural considerations** noting any design decisions or patterns used
- **Known limitations** if any exist
- **Screenshots/logs** when relevant (e.g., UI changes, error handling)

**PR Title Example:**
```
VXC-010: Configuration Management and Logging
```

**PR Description Template:**
```markdown
## VXC Task(s)
- VXC-010

## Summary
Implemented Spring profiles, structured logging, global exception handling, and actuator endpoints.

## Implementation
- Added Spring profiles (local, test, prod)
- Configured Logback with structured JSON logging
- Created global exception handler with standardized API responses
- Added health check and actuator endpoints

## Tests
- Unit tests for global exception handler
- Integration tests for logging output
- Manual verification of health endpoints

## CI Status
- [x] Build passes
- [x] Unit tests pass
- [x] Integration tests pass
- [x] Static analysis passes

## Architectural Considerations
Used standard Spring Boot patterns. No breaking changes to existing architecture.

## Known Limitations
None identified.
```

### Branch Protection

The following branch protection rules must be enforced:

- **Protect `main`**: No direct pushes allowed
- **Protect `develop`**: No direct pushes allowed
- **No direct pushes to protected branches**: All changes must go through PRs
- **PR required for merging**: Feature/fix branches must use Pull Requests
- **Required CI checks**: All required status checks must pass before merge
- **Do not merge failing code**: Never merge code with failing required checks

### GitHub Actions

GitHub Actions is the project's automated CI/CD system. It validates:

- **Compilation**: Maven build succeeds
- **Unit tests**: All unit tests pass
- **Integration tests**: Integration tests with Docker containers pass
- **Static analysis**: SpotBugs, Checkstyle, or other quality gates pass
- **Quality gates**: Code coverage, security scans, and other relevant checks

**Important:** GitHub Actions does NOT replace the developer's Git workflow. Developers must run local validation before pushing. CI is a safety net, not a substitute for local testing.

### Developer vs VoxCode Responsibilities

These are two separate workflows that must not be confused:

**Developer Development Workflow:**
```
Developer → Git branch → code implementation → tests → commit → push → Pull Request → CI → review → merge
```

**VoxCode Product Workflow:**
```
User Request → Classification → Investigation → Evidence → Confirmed Finding → Engineering Plan → Human Approval → Remediation → Verification → Repair/Rollback → Engineering Report
```

- The **Developer Workflow** is how humans build and maintain the VoxCode software itself.
- The **VoxCode Product Workflow** is what the VoxCode software does for its users (analyzing and fixing their code).

These workflows are entirely separate concepts. The developer workflow ensures VoxCode is built professionally. The VoxCode product workflow ensures VoxCode provides safe, evidence-driven code remediation to its users.

---

## Architecture Decision Records (ADRs) TO WRITE
- ADR 01: Modular Monolith vs Microservices
- ADR 02: JavaParser vs Language Server Protocol (LSP) for AST
- ADR 03: RAG Semantic Boundary and Hybrid Retrieval Strategy
- ADR 04: Structured Evidence Validation
- ADR 05: Verification via Docker Sandbox
- ADR 06: Deterministic AST Remediation vs LLM Direct Text Replace
- ADR 07: MCP as Focused Tool Interface vs Separate Architecture
- ADR 08: Intelligence Boundaries (AST vs Graph vs RAG vs Tools vs Build/Tests)

---

## Final Product Acceptance Checklist
- [ ] Adaptive investigation works on real Java/Spring repositories.
- [ ] Findings contain verifiable evidence independently verified by the FindingValidator.
- [ ] Bounded remediation can apply targeted fixes.
- [ ] Remediation is restricted to confirmed findings and approved plans.
- [ ] Human approval blocks unauthorized modifications.
- [ ] Every modification produces a diff and remains strictly within the approved scope.
- [ ] Modified code is strictly verified inside Docker across all 7 verification gates.
- [ ] Limited repair is bounded by a retry budget and does not expand scope.
- [ ] Failed remediation automatically rolls back under defined failure conditions.
- [ ] Investigation and remediation traces are fully persisted and distinctly separated.
- [ ] Engineering Report contains the complete lifecycle.
- [ ] Out-of-scope feature-generation requests are explicitly classified and rejected.
- [ ] RAG implements hybrid retrieval (vector + lexical + metadata/symbol + dependency-aware) with reranking and context assembly.
- [ ] RAG evaluation includes quantitative metrics (Recall@K, Precision@K, MRR, NDCG, retrieval relevance, latency, context relevance, token efficiency, investigation success).
- [ ] Ablation study demonstrates RAG's actual value (AST+Graph+Agent vs AST+Graph+RAG+Agent).
- [ ] RAG never replaces AST or dependency analysis for structural claims.
- [ ] One adaptive primary agent (not multi-agent swarm) implements the Request → Classification → Hypothesis → Tool → Observation → Evaluation → Next Action → Evidence → Decision → Finding loop.
- [ ] Agent can determine evidence is insufficient and request additional retrieval/tool calls.
- [ ] Agent does not expose hidden chain-of-thought; only structured decision traces are persisted.
- [ ] MCP is implemented as a focused tool interface, not a separate architecture or ecosystem.
- [ ] MCP exposes only useful VoxCode capabilities (repository search, file inspection, AST analysis, dependency analysis, semantic retrieval, build, test, verification).
- [ ] Intelligence boundaries are respected: AST = structural truth, JGraphT = relationship truth, RAG = semantic/contextual information, Tools = operational truth, Build/tests = behavioral truth.
- [ ] Project remains one unified VoxCode system with no Kafka/Kubernetes/Redis/microservices/GraphRAG/multi-agent creep.
- [ ] Git/GitHub professional workflow is explicit and followed (branches, commits, pushes, PRs, reviews, merges, branch protection).
- [ ] GitHub Actions validates compilation, unit tests, integration tests, and static analysis.
- [ ] TODO.md remains the single source of truth for VoxCode specification.
