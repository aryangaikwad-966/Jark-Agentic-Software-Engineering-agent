# Jark — Product Requirements Document (PRD)

**Version:** 1.0
**Status:** 🔒 Final / Locked
**Product:** Jark
**Full Name:** **Jark — Agentic Java/Spring Code Review & Remediation Agent**
**Product Type:** Agentic AI Software Engineering Platform
**Primary Domain:** Java/Spring repositories
**Development Horizon:** 1 year
**Scope Philosophy:** Build one complete, production-oriented Agentic AI project within one year. Prioritize depth, reliability, evaluation, and engineering quality over unnecessary features.

---

# 1. Product Overview

## 1.1 Product Name

**Jark**

## 1.2 Full Name

> **Jark — Agentic Java/Spring Code Review & Remediation Agent**

The name communicates:

* **Agentic** → the system investigates, decides, and acts through tools.
* **Java/Spring** → specialized domain rather than a generic coding assistant.
* **Code Review** → investigation, evidence, and validation.
* **Remediation** → the agent can perform bounded fixes.
* **Agent** → emphasizes the decision-making and tool-use loop.

---

# 2. Product Definition

> **Jark is an evidence-driven Agentic AI software engineering agent that understands Java/Spring repositories, investigates issues using AST, dependency graphs and supporting RAG, produces evidence-backed findings, plans bounded remediation, obtains human approval, applies targeted fixes, verifies them in an isolated environment, and generates an engineering report.**

The complete product philosophy is:

> **Understand → Investigate → Prove → Plan → Approve → Act → Verify → Report**

The central principle is:

> **AI-generated decisions are not considered trustworthy merely because the LLM is confident. Jark must provide repository evidence and executable verification.**

---

# 3. Vision

Jark demonstrates a production-oriented Agentic AI system by combining:

* Java/Spring backend engineering
* Repository intelligence
* AST analysis
* Dependency analysis
* RAG
* Tool calling
* Adaptive agent behavior
* Evidence generation
* Finding validation
* Human-in-the-loop control
* Remediation
* Isolated verification
* Failure handling
* Evaluation
* Observability

Jark is **not** intended to become a generic Cursor, Devin, or OpenHands replacement.

Its specialization is:

> **Evidence-driven engineering intelligence and controlled remediation for Java/Spring repositories.**

---

# 4. Core Product Thesis

## Evidence → Decision → Action → Verification

A conventional LLM coding workflow:

```text
User
 ↓
Prompt
 ↓
LLM
 ↓
Answer / Code
```

Jark:

```text
User Request
      ↓
Repository Understanding
      ↓
Agent Investigation
      ↓
Evidence Collection
      ↓
Finding Validation
      ↓
Engineering Plan
      ↓
Human Approval
      ↓
Targeted Remediation
      ↓
Isolated Verification
      ↓
Accept / Repair / Rollback
      ↓
Engineering Report
```

The LLM is therefore only one component.

The system's trust model is based on:

**Repository evidence + agent decisions + executable verification.**

---

# 5. Problem Statement

Java/Spring repositories contain complex relationships between:

* Controllers
* Services
* Repositories
* Interfaces
* Implementations
* Configuration
* Security configuration
* Transactions
* Dependencies
* Tests
* Modules

An LLM working only with text can miss these relationships.

Jark addresses:

### 5.1 Repository Complexity

Developers need to understand unfamiliar codebases before making safe decisions.

### 5.2 AI Hallucination

An LLM can produce convincing conclusions without sufficient repository evidence.

### 5.3 False Positives

AI code-review systems can report issues that are actually valid behavior.

### 5.4 Context Retrieval

Relevant documentation, configuration, conventions, and repository context may not fit naturally into an LLM context window.

### 5.5 Unsafe Code Modification

AI-generated changes can introduce new failures.

### 5.6 Lack of Verification

Generated code should not automatically be considered correct.

---

# 6. Product Strategy

Jark has **two capability layers**.

## Primary Layer — Investigation + Evidence + Verification

This is the heart of Jark.

```text
AST
+
Dependency Graph
+
RAG
      ↓
Adaptive Agent
      ↓
Evidence
      ↓
Finding Validation
      ↓
Verification
      ↓
Measured Result
```

## Supporting Layer — Remediation

Remediation demonstrates that the agent can move from understanding and investigation into controlled engineering action.

```text
Validated Finding
       ↓
Engineering Plan
       ↓
Human Approval
       ↓
Targeted Fix
       ↓
Verification
       ↓
Accept / Repair / Rollback
```

### Scope rule

> **Remediation must never compromise the quality of the core investigation, evidence, evaluation, and verification system.**

Both capabilities are intended to be implemented in the final one-year product, but remediation remains deliberately bounded.

---

# 7. Target Users

## Primary

### Java/Spring Developer

A developer who needs to:

* understand an unfamiliar repository
* investigate issues
* review findings
* safely modify code
* verify modifications

## Secondary

* Backend developers
* AI engineers
* Software engineering teams
* Open-source contributors
* Developers maintaining Java/Spring systems
* Students building production-oriented AI engineering systems

---

# 8. Core User Journey

```text
Landing
   ↓
Connect Repository
   ↓
Repository Analysis
   ↓
Repository Overview
   ↓
Chat / Investigation
   ↓
Finding
   ↓
Evidence
   ↓
Engineering Plan
   ↓
Human Approval
   ↓
Live Agent Execution
   ↓
Remediation
   ↓
Verification
   ↓
Diff
   ↓
Engineering Report
   ↓
History
```

---

# 9. Core Use Cases

## UC-01 — Connect Repository

The user connects a GitHub repository or uploads repository content.

Jark:

1. Loads the repository.
2. Detects Java/Spring structure.
3. Indexes source files.
4. Parses Java source.
5. Builds structural information.
6. Builds dependency relationships.
7. Creates semantic retrieval data.

---

## UC-02 — Understand Repository

Example:

> "How does authentication work?"

Jark investigates relevant:

```text
Controller
 ↓
Security Configuration
 ↓
Service
 ↓
Authentication Components
```

The response must contain repository-grounded references.

---

## UC-03 — Investigate Issue

Example:

> "Find security issues in this repository."

The agent performs:

```text
Hypothesis
 ↓
Choose Tool
 ↓
Observe
 ↓
Evaluate Evidence
 ↓
Need More Evidence?
 ├── Yes → Continue
 └── No  → Validate Finding
```

---

## UC-04 — Request Remediation

Example:

> "Fix the missing authorization."

Jark:

```text
Finding
 ↓
Engineering Plan
 ↓
Human Approval
 ↓
Targeted Modification
 ↓
Verification
 ↓
Result
```

---

## UC-05 — Verify Modification

Jark executes the modified repository in an isolated environment.

It can perform:

* Build
* Unit tests
* Static analysis
* Relevant verification checks

The result determines whether the change is accepted, repaired, or rolled back.

---

# 10. Genuine Agentic Behavior

This is **non-negotiable**.

Jark must not simply execute:

```text
AST → RAG → LLM → Report
```

That is a fixed pipeline.

Instead:

```text
Hypothesis
     ↓
Choose Tool
     ↓
Observe
     ↓
Evaluate
     ↓
Choose Next Action
     ↓
Observe
     ↓
Evaluate
     ↓
Sufficient Evidence?
   ↙       ↘
 No        Yes
 ↓          ↓
Continue   Validate
```

The agent can select among:

* Repository search
* AST inspection
* Dependency graph traversal
* RAG retrieval
* Configuration inspection
* Test inspection
* Diagnostic tools

Every investigation must produce a visible decision trace:

```text
Hypothesis
 ↓
Tool Selected
 ↓
Tool Result
 ↓
Evidence
 ↓
Decision
 ↓
Next Action
 ↓
Final Conclusion
```

### Required Agent Capabilities

The agent must:

1. **Genuinely adapt based on tool observations** - Next action depends on what was observed
2. **Determine evidence insufficiency** - Agent can request additional retrieval/tool calls
3. **Follow the investigation loop** - Request → Classification → Hypothesis → Tool → Observation → Evaluation → Next Action → Evidence → Decision → Finding
4. **Not expose hidden chain-of-thought** - Only structured decision traces are persisted
5. **Handle request classification** - INVESTIGATE, REMEDIATE, INVESTIGATE_AND_REMEDIATE, OUT_OF_SCOPE

### Required proof of agentic behavior

The final system must demonstrate multiple traces where:

> **Evidence changes the agent's investigation path.**

This prevents the agent from being merely a fixed workflow with an LLM attached.

---

# 11. Repository Intelligence

Jark uses three complementary sources.

## 11.1 AST Intelligence

JavaParser provides deterministic structural understanding.

The system should identify:

* Classes
* Interfaces
* Methods
* Fields
* Imports
* Packages
* Annotations
* Method calls
* Inheritance
* Implementations

---

## 11.2 Dependency Graph

The dependency graph represents relationships such as:

```text
Controller
    ↓
Service
    ↓
Repository
```

and:

```text
Class A
 ↓
Class B
 ↓
Class C
```

It enables:

* Caller analysis
* Callee analysis
* Impact analysis
* Dependency tracing
* Relationship investigation

---

## 11.3 Hybrid Repository-Aware RAG

RAG is an important part of Jark, but it is **not the primary structural understanding mechanism**.

RAG implements deep hybrid retrieval for repository semantic context, NOT just chunk→embed→top-K.

### RAG Pipeline

```text
Repository Documents
       ↓
Chunking + Metadata
       ↓
Embeddings
       ↓
Qdrant
       ↓
Hybrid Retrieval
   ├── Vector retrieval (semantic similarity)
   ├── Lexical retrieval (keyword/BM25)
   ├── Metadata/symbol retrieval (exact matches)
   └── Dependency-aware retrieval
       ↓
Reranking
       ↓
Context Assembly
       ↓
Agent
```

### Retrieval Sources

RAG retrieves semantic/contextual information such as:

* README files
* Documentation
* Configuration explanations
* Architecture documentation
* Project conventions
* Repository text
* Historical issue/commit context where available
* Similar implementations
* Related classes/methods
* Tests
* Error handling patterns
* Cross-file semantic context

### Responsibility separation

> **AST answers structural questions.**

> **Dependency Graph answers relationship questions.**

> **RAG answers semantic/contextual questions.**

> **Agent decides which information it needs and when.**

### Important Boundary

**AST = Structural Truth**
**JGraphT = Relationship Truth**
**RAG = Semantic/Contextual Information**
**Tools = Operational Truth**
**Build/Tests = Behavioral Truth**

RAG must NEVER replace AST or dependency analysis for structural claims.

---

# 12. RAG Evaluation

RAG must demonstrate measurable value through comprehensive metrics and ablation study.

Jark performs an ablation study:

```text
Baseline: Agent + AST + Graph
        VS
Full: Agent + AST + Graph + RAG
```

Measure:

* Recall@K
* Precision@K
* MRR (Mean Reciprocal Rank)
* NDCG (Normalized Discounted Cumulative Gain)
* Retrieval relevance
* Retrieval latency
* Context relevance
* Token efficiency
* Investigation success

The purpose is to answer:

> **Does RAG actually improve repository investigation?**

If retrieval does not materially improve relevant tasks, that result should be documented honestly.

Do not add retrieval technologies merely for resume keywords. The evaluation must demonstrate whether RAG actually improves repository investigation.

---

# 13. Supported Issue Classes

Jark does not attempt to detect every possible software problem.

Initial focus:

## 13.1 Security

Examples:

* Missing authorization
* Missing `@PreAuthorize`
* Unsafe endpoint exposure
* Security configuration inconsistencies

## 13.2 Transaction Problems

Examples:

* Missing transaction boundaries
* Incorrect transactional placement
* Transaction propagation concerns

## 13.3 Correctness

Examples:

* Potential null-related defects
* Incorrect control flow
* Error-handling problems

The issue set may expand only when the core system is stable.

---

# 14. Evidence Engine

Every important finding must be evidence-backed.

Example:

```text
Finding
────────────────────
Missing authorization

Severity
────────────────────
HIGH

Affected Files
────────────────────
PaymentController.java
PaymentService.java
SecurityConfig.java

AST Evidence
────────────────────
Endpoint method lacks required authorization annotation.

Dependency Evidence
────────────────────
PaymentController
        ↓
PaymentService
        ↓
PaymentRepository

RAG Evidence
────────────────────
Repository security documentation

Validation
────────────────────
Security analysis confirms endpoint exposure.

Recommendation
────────────────────
Require appropriate authorization.
```

The objective is:

> **A developer must be able to understand why Jark reached its conclusion.**

---

# 15. Finding Validation

The system must distinguish:

```text
Potential Finding
       ↓
Investigation
       ↓
Evidence
       ↓
Validation
       ↓
Confirmed / Rejected / Uncertain
```

Possible statuses:

* **Confirmed**
* **Rejected**
* **Insufficient Evidence**

This directly helps control false positives.

---

# 16. Engineering Plan

For a validated issue, Jark generates:

* Objective
* Problem summary
* Affected files
* Required changes
* Dependencies
* Risk
* Expected result
* Verification strategy

Example:

```text
Objective:
Add authorization to payment endpoint.

Affected:
PaymentController.java
SecurityConfig.java

Risk:
Medium

Verification:
Security tests
Build
Unit tests
```

---

# 17. Human Approval

Human approval is mandatory before remediation.

The user sees:

* Finding
* Evidence
* Proposed plan
* Files affected
* Risk
* Expected changes
* Verification strategy

Actions:

```text
Reject
   OR
Approve
```

The agent must not silently modify the repository.

---

# 18. Bounded Remediation

Jark supports targeted remediation rather than unrestricted autonomous coding.

Supported fixes should remain within explicitly supported issue classes.

Examples:

```text
Missing @PreAuthorize
Missing @Transactional
Supported bounded correctness fixes
```

### Modification principle

Avoid uncontrolled raw text manipulation.

Prioritize transformations in this exact order:

1. **Deterministic transformation using OpenRewrite** when appropriate for Java/Spring transformations
2. **AST-aware transformation**
3. **AI-generated targeted patch** only when deterministic transformation is insufficient

Never use unrestricted LLM text replacement as the default mechanism.

Prefer:

* OpenRewrite for deterministic Java/Spring transformations where appropriate
* JavaParser for AST-aware transformation
* Carefully bounded file modifications
* AI-generated targeted patches only when deterministic transformation is insufficient

---

# 19. Live Agent Modification Experience

Jark should provide a **live agent workspace similar to a developer IDE experience**, while remaining specialized rather than becoming a complete IDE replacement.

During execution the user can see:

```text
Agent
 ↓
Investigating
 ↓
Planning
 ↓
Modifying
 ↓
Verifying
 ↓
Repairing if necessary
 ↓
Completed
```

The UI should visibly stream:

* Agent actions
* Tool calls
* Files being inspected
* Files being modified
* Evidence
* Build output
* Test output
* Verification status
* Diff changes

The user should never feel that the system disappeared behind a chat box.

---

# 20. Verification — Core Capability

Verification is one of Jark's strongest engineering capabilities.

After an approved modification:

```text
Modification
     ↓
Build
     ↓
Tests
     ↓
Static Analysis
     ↓
Result
```

Possible outcomes:

```text
SUCCESS
   ↓
Accept
```

or:

```text
FAILURE
   ↓
Analyze Failure
   ↓
Limited Repair
   ↓
Retry Verification
```

If verification still fails:

```text
Rollback
```

Jark must never claim:

> "Fix successful"

merely because the LLM generated code.

The change is successful only when executable verification provides supporting evidence.

---

# 21. Isolated Execution

Modified or untrusted repository code must not execute directly inside the main application environment.

The execution layer should provide appropriate isolation, including:

* CPU limits
* Memory limits
* Timeout
* Restricted filesystem access
* Controlled network access

The exact implementation is defined in the Technical Design Document.

---

# 22. Limited Repair Loop

When verification fails:

```text
Build/Test Failure
       ↓
Analyze Failure
       ↓
Determine Whether Repair Is Appropriate
       ↓
Targeted Repair
       ↓
Retry
```

The retry budget is strictly bounded.

Example:

```text
Maximum repair attempts = small fixed limit
```

If the limit is reached:

```text
Rollback
```

No infinite autonomous repair loops are permitted.

---

# 23. Rollback

Before remediation, Jark preserves the original state.

If the modification cannot be successfully verified:

```text
Modified Repository
       ↓
Verification Failure
       ↓
Repair Attempts
       ↓
Still Failed
       ↓
Rollback
       ↓
Original State
```

Rollback provides a safety boundary around remediation.

---

# 24. Engineering Report

Every investigation/remediation should produce an engineering report.

### Report contents

* User request
* Repository
* Investigation trace
* Findings
* Evidence
* Validation
* Engineering plan
* Approval
* Files affected
* Changes
* Build result
* Test result
* Static analysis
* Repair attempts
* Rollback status
* Final outcome

Example:

```text
JARK ENGINEERING REPORT

Repository:
payment-service

Finding:
Missing authorization

Evidence:
3 repository components

Plan:
Add authorization annotation

Approval:
Approved

Files changed:
2

Build:
SUCCESS

Tests:
412 / 412 PASSED

Rollback:
Not required

Final:
VERIFIED
```

---

# 25. MCP

MCP is included as a focused tool interface for interoperability, NOT a separate architecture.

Potential capabilities exposed through MCP tools:

* Repository search
* File inspection
* AST analysis
* Dependency analysis
* Semantic retrieval
* Build
* Test
* Verification

Architecture:

```text
Agent
  ↓
MCP Tool Interface
  ↓
Repository Capability
```

### MCP Requirements

* Implement a focused MCP server/interface
* Expose only useful Jark capabilities/tools
* MCP is an interoperability/tool interface, NOT a separate architecture
* Do NOT build a large MCP ecosystem
* Do NOT create dozens of unnecessary servers
* Do NOT create MCP-based microservices
* Use MCP only where it provides genuine architectural value

### MCP Rule

MCP should only be used where it improves:

* Tool separation
* Agent architecture
* Extensibility
* Interoperability
* Understanding of modern agent infrastructure

MCP is therefore an **architectural capability**, not a feature-count objective.

---

# 26. Evaluation System

Evaluation is a first-class requirement.

## 26.1 Finding Detection

Measure:

* Precision
* Recall
* F1
* False-positive rate
* False-negative rate

## 26.2 RAG

Measure:

* Recall@K
* Retrieval relevance
* Retrieval latency

## 26.3 Agent

Measure:

* Investigation success rate
* Tool-call efficiency
* Investigation time
* Unnecessary tool calls
* Investigation termination
* Decision-trace quality

## 26.4 Remediation

Measure:

* Fix success rate
* Build success rate
* Test success rate
* Regression rate
* Repair success rate
* Rollback rate

---

# 27. Benchmark

The benchmark should use real Java/Spring bugs and historical repository states where practical.

Each case should contain:

```text
Repository
Commit / Version
Bug Category
Expected Finding
Expected Evidence
Expected Outcome
```

Benchmark development begins early in the project rather than being postponed until the end.

---

# 28. Observability

Jark records:

* Agent decisions
* Tool calls
* Tool results
* LLM latency
* Token usage where available
* RAG latency
* Retrieved documents
* Investigation duration
* Build duration
* Test duration
* Repair attempts
* Failure reasons
* Final outcome

The objective is to make the agent inspectable rather than a black box.

---

# 29. Security Requirements

Jark must protect both repositories and execution environments.

Required areas:

* Authentication
* Authorization
* Repository isolation
* Tool authorization
* Path traversal protection
* Command restrictions
* Secret protection
* Prompt-injection awareness
* Sandbox restrictions

---

# 30. UI/UX Direction

The supplied Jark visual reference is the visual source of truth.

### Visual language

* Dark developer-focused interface
* Near-black background
* Bright green primary accent
* Subtle green borders/glows
* Minimal secondary colors
* Modern typography
* High information density
* Premium developer-tool aesthetic

### Core experience

```text
Landing
 ↓
Connect Repository
 ↓
Repository Overview
 ↓
Chat / Q&A
 ↓
Investigation
 ↓
Finding
 ↓
Plan
 ↓
Approval
 ↓
Live Agent
 ↓
Verification
 ↓
Diff
 ↓
Report
 ↓
History
```

The interface should emphasize **visible agent activity, evidence, diffs, and verification**, not just conversational responses.

---

# 31. Core Screens

## 31.1 Landing

* Connect GitHub
* Upload Repository
* Try Demo Repository

## 31.2 Repository Connection

* Repository
* Branch
* Connection status
* Analysis status

## 31.3 Repository Overview

* Repository statistics
* Modules
* Dependencies
* Architecture
* Analysis progress
* Supported issue categories

## 31.4 Chat / Q&A

User can:

* Ask repository questions
* Start investigation
* Ask about findings
* Request remediation

## 31.5 Investigation Workspace

Display:

* Current hypothesis
* Agent status
* Tool calls
* Evidence discovered
* Relevant files
* Investigation trace

## 31.6 Finding Details

Display:

* Severity
* Description
* Evidence
* Code locations
* AST evidence
* Dependency evidence
* RAG evidence
* Validation
* Recommendation

## 31.7 Engineering Plan

Display:

* Objective
* Tasks
* Files
* Risk
* Verification strategy

## 31.8 Approval

Actions:

```text
Reject
Approve
```

## 31.9 Live Agent Workspace

Display live:

```text
Analyzing...
Searching...
Inspecting AST...
Checking dependencies...
Retrieving context...
Planning...
Modifying...
Running tests...
Verifying...
```

Also show:

* Current file
* Agent action
* Tool call
* Code changes
* Diff
* Logs
* Status

## 31.10 Verification

Display:

* Build
* Tests
* Static analysis
* Current status
* Failure information
* Repair attempts
* Final verification result

## 31.11 Diff Preview

Display:

* Files changed
* Unified diff
* Split diff
* Added lines
* Removed lines

## 31.12 Engineering Report

Display:

* Finding
* Evidence
* Plan
* Changes
* Verification
* Final status

## 31.13 History

Display:

* Investigations
* Findings
* Remediations
* Reports
* Verification results

## 31.14 Settings

Only useful product settings should be included.

Avoid unnecessary configuration.

---

# 32. Technology Direction

## Backend

* Java 21
* Spring Boot
* Maven
* Spring Security

## AI

* Spring AI
* LLM provider
* Embeddings

## Repository Intelligence

* JavaParser
* Dependency graph
* OpenRewrite where useful

## RAG

* Qdrant
* Embeddings
* Semantic retrieval

## Database

* MySQL

## Execution

* Isolated execution environment
* Docker-based verification

## Testing

* JUnit 5
* Mockito

## Version Control

* Git
* GitHub

## Frontend

* React-based UI
* Jark visual reference

## Agent Tool Architecture

* Spring AI tool calling
* MCP where meaningful

---

# 33. Explicit Scope Boundaries

## Must Build

### Repository Intelligence

* Java/Spring repository analysis
* AST
* Dependency graph
* Repository indexing

### AI

* Supporting RAG
* RAG evaluation
* Adaptive investigation agent
* Tool calling

### Evidence

* Evidence engine
* Finding validation
* Decision traces

### Product

* Repository Q&A
* Engineering planning
* Human approval
* Live agent workspace
* Diff visualization
* Engineering reports
* History

### Verification

* Build
* Tests
* Static analysis
* Isolated execution
* Verification result

### Evaluation

* Benchmark
* Precision
* Recall
* F1
* RAG metrics
* Agent metrics
* Remediation metrics

### Remediation

* Targeted fixes
* Human approval
* Limited repair
* Rollback

### Infrastructure

* Observability
* Security
* MCP where justified

---

# 34. Do NOT Build

To protect the one-year scope:

❌ Multi-agent architecture
❌ LangGraph merely for using LangGraph
❌ General-purpose coding assistant
❌ Full Cursor replacement
❌ Full Devin replacement
❌ Generic multi-language support
❌ Foundation-model training
❌ Graph RAG
❌ Kubernetes
❌ Kafka
❌ Redis
❌ Microservices
❌ Distributed service architecture
❌ Complex caching
❌ Complex messaging infrastructure
❌ Custom distributed workflow engines
❌ Large MCP ecosystem
❌ Unrestricted autonomous coding
❌ Endless bug categories
❌ Unnecessary Python microservices
❌ Voice as a core engineering dependency
❌ Features that exist only for resume keyword collection

**Database Constraint:** MySQL/JPA/Hibernate are supporting infrastructure for Agentic AI state persistence. No complex database optimization, advanced SQL engineering, database sharding, complicated caching architecture, or unnecessary database abstractions.

**MCP Constraint:** MCP is a focused tool interface for interoperability. Do NOT create multiple MCP servers, MCP microservices, dozens of MCP tools, or an MCP ecosystem. Use MCP only where it provides genuine architectural value for tool interoperability. MCP is OPTIONAL (TIER 5) - only implement if time remains after core Agentic AI, RAG, and Verification capabilities are complete.

**Optional voice interaction may be added only if the core product is already complete and stable.**

---

# 35. One-Year Development Priority

## Priority 1 — Repository Intelligence

```text
AST
+
Dependency Graph
+
Repository Index
```

## Priority 2 — Agent

```text
Adaptive Investigation
+
Tool Calling
+
Decision Traces
```

## Priority 3 — RAG

```text
Chunking
→ Embeddings
→ Qdrant
→ Retrieval
→ Evaluation
```

## Priority 4 — Evidence

```text
Evidence Collection
→ Finding
→ Validation
```

## Priority 5 — Verification

```text
Build
→ Test
→ Static Analysis
→ Measured Result
```

## Priority 6 — Evaluation

Benchmark and metrics must be developed early, not during the final month.

## Priority 7 — Remediation

```text
Plan
→ Approval
→ Fix
→ Verify
→ Repair
→ Rollback
```

Remediation should be developed after the core investigation/evidence/verification foundations are stable.

---

# 36. Success Criteria

Jark is successful when it can:

## Repository

* Analyze Java/Spring repositories
* Build AST information
* Build dependency relationships
* Index repository content

## RAG

* Retrieve meaningful semantic context
* Measure retrieval quality
* Demonstrate the effect of RAG through ablation

## Agent

* Select appropriate tools
* Perform multi-step investigation
* Change investigation paths based on evidence
* Stop when sufficient evidence exists
* Produce visible decision traces

## Evidence

* Produce structured evidence-backed findings
* Reference exact repository components
* Distinguish confirmed, rejected, and uncertain findings

## Verification

* Execute changes in isolation
* Build the repository
* Run tests
* Analyze failures
* Produce an objective verification result

## Remediation

* Require human approval
* Apply bounded fixes
* Detect verification failures
* Perform limited repair
* Roll back unsuccessful modifications

## Evaluation

* Produce Precision/Recall/F1
* Measure RAG performance
* Measure agent effectiveness
* Measure remediation success and regression rates

---

# 37. Final Product Architecture

```text
                         JARK
                            │
                      User Request
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Repository Manager  │
                 └──────────┬──────────┘
                            │
                            ▼
              ┌──────────────────────────┐
              │ Repository Intelligence  │
              │                          │
              │ AST                      │
              │ Dependency Graph         │
              │ Repository Index         │
              └────────────┬─────────────┘
                           │
                           ├───────────────┐
                           │               │
                           ▼               ▼
                    Semantic RAG      Structural Tools
                           │               │
                           └───────┬───────┘
                                   ▼
                         ┌──────────────────┐
                         │ Investigation    │
                         │ Agent            │
                         │                  │
                         │ Hypothesize      │
                         │ Tool Call        │
                         │ Observe          │
                         │ Evaluate         │
                         │ Decide           │
                         └────────┬─────────┘
                                  │
                                  ▼
                         Evidence Engine
                                  │
                                  ▼
                         Finding Validation
                                  │
                         ┌────────┴────────┐
                         │                 │
                      Invalid            Valid
                         │                 │
                         ▼                 ▼
                       Report        Engineering Plan
                                           │
                                           ▼
                                    Human Approval
                                           │
                                           ▼
                                    Targeted Fix
                                           │
                                           ▼
                                  Isolated Execution
                                           │
                                           ▼
                                  Build / Test / Scan
                                           │
                              ┌────────────┴────────────┐
                              │                         │
                           Success                   Failure
                              │                         │
                              ▼                         ▼
                           Accept                Limited Repair
                                                        │
                                                        ▼
                                                     Retry
                                                        │
                                             ┌──────────┴─────────┐
                                             │                    │
                                          Success              Failure
                                             │                    │
                                             ▼                    ▼
                                           Accept              Rollback
                                             │                    │
                                             └──────────┬─────────┘
                                                        ▼
                                             Engineering Report
                                                        │
                                                        ▼
                                                   Evaluation
```

---

# 38. Final Product Identity

### Name

**Jark**

### Full Name

> **Jark — Agentic Java/Spring Code Review & Remediation Agent**

### Product Type

**Agentic AI Software Engineering Platform**

### Core Identity

> **An evidence-driven Agentic AI software engineering agent specialized in Java/Spring repositories.**

### Primary Focus

> **Agentic investigation + evidence + verification**

### Supporting Focus

> **Bounded, human-approved remediation**

### Technologies Demonstrated

**AI / Agentic**

* LLM integration
* Spring AI
* Tool calling
* Agentic investigation
* RAG
* Embeddings
* Qdrant
* AST reasoning
* Dependency graphs
* MCP
* Evaluation
* Observability

**Engineering**

* Java
* Spring Boot
* Spring Security
* REST APIs
* SQL
* Git
* Docker
* Testing
* Maven
* Build systems
* Reliability
* Security
* System design

---

# 39. Final One-Sentence Definition

> **Jark is an Agentic Java/Spring Code Review & Remediation Agent that combines AST-based repository intelligence, dependency graphs, supporting RAG, adaptive tool-using investigation, evidence-backed findings, human-approved remediation, and isolated verification to turn AI-generated software-engineering decisions into measurable, verifiable outcomes.**

---

# 🔒 FINAL LOCK

**Jark is a one-year complete project, not a multi-year roadmap.**

The final philosophy is:

> **Understand → Investigate → Prove → Plan → Approve → Act → Verify → Report**

The most important engineering rule is:

> **If you must choose between adding another feature and making the existing agent, RAG, evidence, evaluation, remediation, or verification significantly better — improve the existing system.**

The goal is **not the maximum number of features**.

The goal is to build **one exceptionally deep, measurable, reliable Agentic AI engineering system** that you can defend technically from architecture all the way down to an individual agent tool call, code change, test result, and verification decision.
