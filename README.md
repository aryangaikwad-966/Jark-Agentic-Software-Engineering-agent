# Jark — Agentic Java RAG Intelligence

Jark is an Agentic AI system for Java/Spring repository intelligence that uses AST analysis, dependency analysis, and RAG to investigate engineering problems, gather and validate evidence, create engineering plans, perform human-approved bounded remediation, and verify or roll back changes.

**Core Principle:**
Understand → Retrieve → Investigate → Prove → Plan → Act → Verify

**Core Thesis:**
"Understand before changing, prove before acting, and verify after acting."

## Architecture

Jark follows a unified architecture:

```
Repository → Repository Intelligence (AST + Dependency Graph + Repository Index) → Hybrid Repository-Aware RAG → One Adaptive Primary Agent → Evidence Engine → Finding Validation → Engineering Plan → Human Approval → Bounded Remediation → MCP/Tool Interface → Isolated Docker Workspace → Build + Tests + Static Analysis + Diff/Scope Validation → PASS (Accept) / FAIL (Bounded Repair) / FAIL after retry budget (Rollback) → Engineering Report → Evaluation
```

## Technology Stack

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
- **MCP:** Focused MCP server/interface exposing only useful Jark capabilities/tools (repository inspection, AST analysis, dependency analysis, retrieval, build, test, verification) - NOT a separate architecture or ecosystem

**EXCLUDED TECHNOLOGIES**
- Redis, Kafka, Kubernetes, Microservices, Distributed service architecture, Complex caching, Complex messaging infrastructure, Custom distributed workflow engines, Huge MCP ecosystem, Multi-agent swarm architecture, Foundation-model training, Custom LLM, Generic multi-language support, Full IDE replacement, Unlimited autonomous coding, Complex voice infrastructure, Arbitrary feature generation

## Intelligence Boundaries

- **AST = Structural Truth**: JavaParser provides deterministic structural analysis
- **JGraphT = Relationship Truth**: Dependency graph provides deterministic relationship analysis
- **RAG = Semantic/Contextual Information**: Hybrid retrieval provides semantic context
- **Tools = Operational Truth**: Agent tools provide operational capabilities
- **Build/Tests = Behavioral Truth**: Execution verification provides behavioral validation

## Local Setup

### Prerequisites
- Docker & Docker Compose
- Java 21 (install via Homebrew: `brew install openjdk@21`)
- Maven 3.9+
- Node.js 20+

### Java 21 Setup
If you have multiple Java versions installed, set Java 21 as the default:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

Or use the provided setup script:
```bash
source setup-env.sh
```

### Starting Infrastructure
1. Make sure Docker is running.
2. In the root directory, run:
   ```bash
   docker-compose up -d
   ```
   This starts the MySQL (port 3306) and Qdrant (port 6333) containers.

### Backend Development
1. Navigate to `Backend/`
2. Set up Java 21 environment:
   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk@21
   export PATH="$JAVA_HOME/bin:$PATH"
   ```
3. Verify Java version:
   ```bash
   java -version
   ```
4. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

### Frontend Development
1. Navigate to `Frontend/`
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the dev server:
   ```bash
   npm run dev
   ```

## Project Scope

Jark is a one-year solo project focused on building a deep, measurable, and defensible Agentic AI system for Java/Spring repositories.

**Primary Capabilities:**
- Investigation: Repository intelligence and evidence-backed findings
- Remediation: Bounded, human-approved code modifications

**Supported Remediation Scope:**
- Localized bug fixes
- Security fixes using existing infrastructure
- Incorrect annotations/configuration
- Deprecated Java/Spring API migrations
- Incorrect method calls
- Transaction-boundary fixes
- Dependency/configuration corrections
- Other small, single-concern, few-file fixes

**Unsupported Scope:**
- Entire application feature generation
- Authentication infrastructure from scratch
- New payment systems
- Large modules
- Arbitrary application generation
- Unlimited autonomous coding

## Development Workflow

Jark follows a professional Git workflow:

**Branching Strategy:**
- `main` → stable/release-ready code
- `develop` → integration branch for upcoming releases
- `feature/*` → new feature/task implementation branches
- `fix/*` → bug fix branches
- `hotfix/*` → critical fixes

**Developer Workflow:**
1. Start from latest `develop`
2. Pull latest remote changes
3. Create feature/fix branch
4. Implement only assigned task and dependencies
5. Write/update tests
6. Run local validation
7. Run Maven validation
8. Review git status and diff
9. Commit logical changes
10. Push branch
11. Open PR against `develop`
12. Run GitHub Actions
13. Fix CI failures
14. Review PR
15. Merge only after required checks pass
16. Delete branch after successful merge

## Documentation

- **TODO.md** - Single source of truth for Jark specification and implementation tasks
- **Docs/PRD.md** - Product Requirements Document (locked)
- **Docs/Design doc.md** - UI/UX Design Document (locked)
- **Docs/Tech doc.md** - Technical Design Document (locked)

## Technology Protection

**EXCLUDED TECHNOLOGIES:**
- Redis, Kafka, Kubernetes, Microservices, Distributed service architecture, Complex caching, Complex messaging infrastructure, Custom distributed workflow engines, Huge MCP ecosystem, Multi-agent swarm architecture, Foundation-model training, Custom LLM, Generic multi-language support, Full IDE replacement, Unlimited autonomous coding, Complex voice infrastructure, Arbitrary feature generation

**Database Constraint:** MySQL/JPA/Hibernate are supporting infrastructure for Agentic AI state persistence. No complex database optimization, advanced SQL engineering, database sharding, complicated caching architecture, or unnecessary database abstractions.

**MCP Constraint:** MCP is a focused tool interface for interoperability. Do NOT create multiple MCP servers, MCP microservices, dozens of MCP tools, or an MCP ecosystem. Use MCP only where it provides genuine architectural value for tool interoperability.

**Technology Priority Rule:** Every technology must have a clear role in Retrieve → Investigate → Decide → Act → Verify → Evaluate. If a technology does not strengthen this loop, it should NOT be added.
