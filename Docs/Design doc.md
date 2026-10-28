# Jark — Final UI/UX Design Document

**Version:** 1.0
**Status:** 🔒 Final / Locked
**Product:** Jark
**Full Name:** **Jark — Agentic Java/Spring Code Review & Remediation Agent**
**Design Status:** Final
**Development Horizon:** 1 year

> **Design Basis:** The supplied Jark visual reference is the primary visual inspiration. Functional behavior follows the final PRD.

---

# 1. Design Vision

Jark must feel like a **premium AI-native developer workspace**, not a generic chatbot.

The core experience is:

> **Understand → Investigate → Evidence → Plan → Approve → Act → Verify → Report**

The UI must make the agent's engineering process visible.

The developer should always know:

1. What Jark is doing.
2. Which tools it is using.
3. What evidence it discovered.
4. What it believes is wrong.
5. What it plans to change.
6. What will be modified.
7. Whether the modification was verified.
8. What happened if verification failed.

---

# 2. Product UX Identity

Jark should communicate:

> **"An engineering agent that investigates your Java/Spring repository, explains its evidence, makes controlled changes, and proves whether those changes actually work."**

It should **not** feel like:

* ChatGPT with a code editor
* A generic code-generation tool
* A simple static code scanner
* A generic Cursor clone

The central UI concept is:

```text
Developer
    ↕
Jark Agent
    ↕
Repository
```

---

# 3. Design Reference

The supplied Jark reference establishes the visual direction:

* Dark developer-tool interface
* Near-black background
* Neon-green identity
* Thin green borders
* Code-editor aesthetic
* Compact information hierarchy
* Technical dashboards
* Agent activity visualization
* Minimal rounded cards
* Premium developer-tool appearance

The reference should influence the **visual language**, while the final PRD determines the actual functionality.

---

# 4. Core Design Principles

## 4.1 Evidence First

Never show an important conclusion without making its evidence accessible.

Instead of:

> Security issue detected.

Show:

```text
HIGH
Missing Authorization

Location
PaymentController.java:42

Evidence
├── PaymentController.java
├── PaymentService.java
└── SecurityConfig.java

Validation
✓ Confirmed
```

---

## 4.2 Agent Transparency

The agent's activity must be visible.

```text
Hypothesis
    ↓
AST Search
    ↓
Dependency Analysis
    ↓
RAG Retrieval
    ↓
Evidence Evaluation
    ↓
More Evidence Required
    ↓
Validation
```

This is one of the most important UX elements in Jark.

---

## 4.3 Human Control

The user must remain in control of code modification.

Clearly distinguish:

```text
Investigating
        ↓
Planning
        ↓
Awaiting Approval
        ↓
Modifying
        ↓
Verifying
```

The agent must never make an approved-looking modification without an explicit approval state.

---

## 4.4 Developer Native

Use familiar developer interfaces:

* File explorer
* Code editor
* Git information
* Terminal
* Build logs
* Test results
* Diff viewer
* Dependency graph
* Repository structure

---

## 4.5 Verification Visibility

The UI must distinguish:

```text
Generated
Investigated
Evidence-backed
Approved
Modified
Verified
```

A generated fix is **not** a verified fix.

---

# 5. Color System

The visual identity is based on **near-black + neon green**.

## Primary Background

```text
#050705
```

## Surface

```text
#0A0F0B
```

Used for:

* Cards
* Panels
* Navigation
* Code areas

## Elevated Surface

```text
#101611
```

Used for:

* Modals
* Active panels
* Expanded cards

## Primary Green

```text
#00E676
```

Used for:

* Primary actions
* Success
* Active navigation
* Agent indicators
* Important highlights

## Secondary Green

```text
#00C853
```

Used for:

* Secondary accents
* Progress indicators
* Supporting highlights

## Border

```text
#1C3323
```

## Primary Text

```text
#F5F7F5
```

## Secondary Text

```text
#9AA39D
```

## Muted Text

```text
#66706A
```

---

# 6. Status Colors

Green is the primary brand color, but status colors must remain distinguishable.

### Success

Green.

Example:

```text
✓ VERIFIED
```

### Warning

Amber/yellow.

```text
⚠ MEDIUM RISK
```

### Error

Red.

Used only when necessary:

* Failed build
* Failed tests
* Critical security issue
* Rollback

### Information

Neutral gray/blue-gray.

Do not introduce unnecessary colors.

---

# 7. Typography

## UI Font

**Inter**

## Code Font

**JetBrains Mono**

Suggested hierarchy:

```text
Page Title       24–28px
Section Title    18–20px
Card Title       14–16px
Body             13–14px
Metadata         11–12px
Code             12–13px
```

The UI should remain information-dense without becoming cluttered.

---

# 8. Border & Radius

### Cards

```text
8–12px
```

### Buttons

```text
6–8px
```

### Inputs

```text
6–8px
```

### Pills

```text
999px
```

Use thin, subtle borders.

Avoid excessive glassmorphism or large shadows.

---

# 9. Iconography

Use consistent minimal line icons.

Important icons:

* Repository
* GitHub
* Search
* Code
* Graph
* Shield
* Agent
* Terminal
* Check
* Warning
* Error
* History
* Settings
* Voice
* File
* Git branch

Icons should support the interface rather than dominate it.

---

# 10. Branding

## Logo

```text
Jark
```

Brand treatment:

* **Vox** → white
* **Code** → neon green

Possible logo structure:

```text
[code / AI symbol] Jark
```

## Product Descriptor

> **Agentic Java/Spring Code Review & Remediation Agent**

## Primary Product Message

> **Understand. Investigate. Verify.**

---

# 11. Global Application Layout

After repository connection:

```text
┌──────────────────────────────────────────────────────────────┐
│ Jark       Repository / Branch                 User       │
├───────────────┬──────────────────────────────────────────────┤
│               │                                              │
│ Overview      │                                              │
│ Investigate   │                                              │
│ Findings      │              Main Workspace                  │
│ Plans         │                                              │
│ Execution     │                                              │
│ Verification  │                                              │
│ Reports       │                                              │
│               │                                              │
│ History       │                                              │
│ Settings      │                                              │
└───────────────┴──────────────────────────────────────────────┘
```

Desktop is the primary experience.

---

# 12. Primary Navigation

```text
Jark

Repository
├── Overview
├── Investigate
├── Findings
├── Plans
├── Execution
├── Verification
└── Reports

History

Settings
```

No unnecessary navigation items.

---

# 13. Landing Screen

## Goal

Immediately communicate the product.

```text
                 Jark

       Agentic Java/Spring Code
       Review & Remediation Agent

   Understand your repository.
   Investigate issues.
   Verify changes.

       [ Connect GitHub ]

       [ Upload Repository ]

          Try Demo →
```

### Primary CTA

**Connect GitHub**

### Secondary CTA

**Upload Repository**

### Tertiary CTA

**Try Demo Repository**

---

# 14. Repository Connection

```text
← Back

Connect Repository

        GitHub

Connect your GitHub repository

[ Connect GitHub ]

OR

Repository URL

[ https://github.com/... ]

Branch

[ main ▼ ]

[ Analyze Repository ]
```

Security message:

> Repository analysis and execution occur in an isolated environment.

---

# 15. Repository Analysis

Do not display only a spinner.

Show actual progress:

```text
Analyzing Repository

✓ Repository connected
✓ Files indexed
✓ Java files detected
✓ AST analysis
● Building dependency graph
○ Preparing RAG index
○ Preparing agent tools
```

The user should understand what is happening.

---

# 16. Repository Overview

The overview is the main repository dashboard.

### Header

```text
payment-service                         ● READY

Java · Spring Boot · Maven
main
```

### Metrics

```text
Modules       Java Files       Classes       Dependencies
36            1,296           125K LOC       98
```

### Architecture

Show the repository structure visually:

```text
Controller
     ↓
Service
     ↓
Repository
     ↓
Database
```

### Intelligence Status

```text
AST Analysis          ✓ READY
Dependency Graph      ✓ READY
RAG Index             ✓ READY
Agent                 ✓ READY
```

---

# 17. Repository Intelligence Visualization

The overview should expose the three intelligence layers:

```text
┌─────────────────────────────────────────────┐
│ Repository Intelligence                     │
│                                             │
│  AST             Dependency Graph     RAG   │
│  ✓ Ready         ✓ Ready              ✓     │
│                                             │
│  Structural      Relationships        Context│
└─────────────────────────────────────────────┘
```

The user should understand that Jark does not rely only on LLM text retrieval.

### Intelligence Boundaries

The UI should communicate the distinct roles of each intelligence layer:

```text
┌─────────────────────────────────────────────┐
│ Intelligence Boundaries                     │
│                                             │
│  AST               = Structural Truth      │
│  Dependency Graph    = Relationship Truth   │
│  RAG                = Semantic Context      │
│  Tools              = Operational Truth     │
│  Build/Tests        = Behavioral Truth      │
└─────────────────────────────────────────────┘
```

This helps users understand that Jark uses the right tool for the right truth.

---

# 18. Chat / Repository Q&A

The chat should feel like an engineering interface, not a generic chatbot.

### User

> Explain how authentication works in this repository.

### Jark

```text
Authentication Flow

AuthController
      ↓
SecurityFilterChain
      ↓
AuthenticationManager
      ↓
UserDetailsService

Evidence
• SecurityConfig.java
• AuthController.java
• CustomUserDetailsService.java
```

Evidence references should be clickable.

---

# 19. Investigation Workspace

This is one of the **most important screens in Jark**.

```text
┌──────────────────────────────────────────────────────────────┐
│ Investigation: Payment Authorization                        │
├──────────────────────┬───────────────────────────────────────┤
│ Agent Trace          │ Evidence                              │
│                      │                                       │
│ ● Hypothesis         │ PaymentController.java                │
│ ↓                    │                                       │
│ ✓ AST Search         │ @PostMapping("/payments")             │
│ ↓                    │                                       │
│ ✓ Dependency Graph   │ PaymentService.java                   │
│ ↓                    │                                       │
│ ✓ Hybrid RAG         │ SecurityConfig.java                   │
│   ├ Vector Retrieval │                                       │
│   ├ Lexical Search   │                                       │
│   ├ Metadata Filter  │                                       │
│   └ Reranking        │                                       │
│ ↓                    │                                       │
│ ● Validation         │                                       │
└──────────────────────┴───────────────────────────────────────┘
```

This screen should demonstrate genuine agent behavior.

---

# 20. Agent Trace

Every important tool action should be visible.

Example:

```text
10:31:04
Agent formed hypothesis

10:31:05
Tool → AST Search

10:31:06
Found PaymentController

10:31:07
Tool → Dependency Graph

10:31:08
Found PaymentService dependency

10:31:10
Tool → RAG Search

10:31:11
Retrieved security documentation

10:31:12
Agent → More evidence required

10:31:13
Tool → Security Configuration

10:31:15
Finding validated
```

The trace should communicate:

> **The agent is deciding what to investigate next.**

### Agent Adaptation Visualization

When evidence changes the investigation path:

```text
10:31:16
Evidence contradicts initial hypothesis.
Updating investigation strategy...

10:31:17
New hypothesis formed based on contradictory evidence.
```

### Evidence Insufficiency Detection

When the agent determines more information is needed:

```text
10:31:18
Current evidence insufficient for conclusion.
Requesting additional retrieval...

10:31:19
Tool → Hybrid RAG Search
  ├ Vector Retrieval
  ├ Lexical Search
  ├ Metadata Filter
  └ Reranking
```

---

# 21. Investigation Status

Use a consistent status model:

```text
○ Ready
● Investigating
● Running AST Analysis
● Searching Dependencies
● Retrieving Context
◐ Awaiting Approval
● Modifying
● Verifying
✓ Verified
✕ Failed
```

Always accompany color with text.

---

# 22. Findings Screen

Findings should be structured rather than buried in chat.

Example:

```text
┌──────────────────────────────────────────────┐
│ HIGH                                         │
│ Missing Authorization                        │
│                                              │
│ PaymentController.java:42                    │
│                                              │
│ Evidence: 4 components                       │
│ Validation: ✓ Confirmed                      │
│                                              │
│ [ View Evidence ] [ Investigate ]            │
└──────────────────────────────────────────────┘
```

---

# 23. Finding Details

Each finding should contain:

### Summary

What is wrong?

### Severity

How important is it?

### Location

Exact file and line.

### Evidence

What did Jark observe?

### Structural Evidence

AST + dependency graph.

### Semantic Evidence

RAG context.

### Validation

Why is the finding confirmed?

### Recommendation

What should be done?

---

# 24. Evidence Visualization

Evidence should be expandable.

```text
Evidence
────────────────────────

▾ AST Evidence

  PaymentController.java
  Method: processPayment()


▾ Dependency Evidence

  PaymentController
        ↓
  PaymentService
        ↓
  PaymentRepository


▾ RAG Evidence

  Hybrid Retrieval Results:
  ├ Vector Retrieval: 85% similarity
  ├ Lexical Search: "authorization endpoint"
  ├ Metadata Filter: @RestController methods
  ├ Dependency-Aware: Related security configs
  └ Reranking: Reordered by relevance

  security.md
  Section: Endpoint Authorization


▾ Validation

  Authorization analysis
  Result: ✓ Confirmed
```

This is a core Jark UX differentiator.

---

# 25. Engineering Plan

```text
Engineering Plan

Objective
Add authorization to payment endpoint.

Affected Files
✓ PaymentController.java
✓ SecurityConfig.java

Tasks
✓ Add authorization annotation
✓ Verify role configuration
✓ Update security test

Risk
● Medium

Verification
✓ Build
✓ Unit tests
✓ Security tests

[ Review & Approve ]
```

The plan should be understandable before approval.

---

# 26. Approval Screen

Approval is a **safety gate**.

```text
Review Plan

✓ Dependency Analysis
✓ Impact Analysis
✓ Code Change Analysis
✓ Verification Strategy
✓ Rollback Available

Files Affected
2

Risk
Medium

[ Cancel ]       [ Approve & Execute ]
```

The approval action must be explicit.

---

# 27. Agent-in-Action Workspace

This is the visual centerpiece.

The UI should show live agent activity:

```text
Agent is working...

✓ Analyzing target files
✓ Inspecting dependencies
● Applying modification
○ Running build
○ Running tests
○ Verification
```

The user should be able to observe the process rather than seeing only:

> "Working..."

---

# 28. IDE-Like Code Workspace

Because Jark supports **live agent modification**, the execution experience should resemble a lightweight developer IDE.

```text
┌─────────────┬──────────────────────────┬─────────────────────┐
│ Explorer    │ Code Editor              │ Agent Activity      │
│             │                          │                     │
│ src         │ PaymentController.java   │ Applying change...  │
│ ├ controller│                          │                     │
│ ├ service   │ @RestController          │ ✓ Analysis          │
│ └ repository│                          │ ● Modification      │
│             │                          │ ○ Verification      │
└─────────────┴──────────────────────────┴─────────────────────┘
```

The agent can modify files live, but the modification must remain within the approved plan.

---

# 29. Code Explorer

Example:

```text
EXPLORER

payment-service
├── src
│   ├── main
│   │   └── java
│   │       └── payments
│   │           ├── controller
│   │           ├── service
│   │           ├── repository
│   │           └── model
│   └── test
└── pom.xml
```

Modified files should be visibly marked.

---

# 30. Live Modification View

The user should see the modification as it occurs.

Example:

```text
PaymentController.java

- @PostMapping("/payments")
+ @PreAuthorize("hasRole('USER')")
+ @PostMapping("/payments")
```

Never replace this with:

> "Changes completed."

---

# 31. Diff Preview

Support:

```text
[ Unified ] [ Split ]
```

Example:

```text
PaymentController.java

  @RestController

- @PostMapping("/payments")
+ @PreAuthorize("hasRole('USER')")
+ @PostMapping("/payments")
```

Use green for additions and red for removals.

Keep surrounding code neutral.

---

# 32. Verification Screen

Verification should be one of the strongest visual states.

```text
VERIFICATION

Build
✓ SUCCESS

Tests
✓ 412 / 412 PASSED

Static Analysis
✓ PASSED

Regression
✓ NO DETECTED REGRESSION

Execution Time
18.4s


FINAL RESULT

✓ VERIFIED
```

The interface should make it impossible to confuse **generated** with **verified**.

---

# 33. Verification Failure

Example:

```text
Verification Failed

Build
✓ Passed

Tests
✕ 3 failed

Agent Analysis
● Investigating failure...

Repair Attempt
1 / 2

[ View Failure ]
```

If repair succeeds:

```text
✓ Repair successful
✓ Tests passed
✓ VERIFIED
```

If repair fails:

```text
✕ Verification failed

Rollback initiated...

✓ Repository restored

FINAL RESULT

ROLLBACK COMPLETED
```

---

# 34. Execution Report

```text
✓ SUCCESS

Files Changed       2
Tests Passed        412 / 412
Build               SUCCESS
Time                18.4s

────────────────────────

Finding
Missing authorization

Evidence
4 repository components

Changes
2 files modified

Verification
Build + Tests + Static Analysis

Final
VERIFIED
```

Actions:

```text
[ View Changes ]
[ Open Repository ]
```

---

# 35. Engineering Report

The final report should combine the complete engineering lifecycle.

```text
JARK ENGINEERING REPORT

Repository
payment-service

Request
Find and remediate authorization issue

Finding
Missing authorization

Evidence
4 components

Plan
Add endpoint authorization

Approval
✓ Approved

Changes
2 files

Build
✓ SUCCESS

Tests
✓ 412 / 412

Verification
✓ PASSED

Rollback
Not required

FINAL
✓ VERIFIED
```

---

# 36. History

History should show previous engineering sessions.

```text
Today

Add authorization
✓ Verified

Investigate payment flow
✓ Completed

Yesterday

Transaction analysis
✓ Completed

Authentication investigation
✓ Completed
```

Each item can show:

* Repository
* Request
* Timestamp
* Findings
* Status
* Verification result

---

# 37. Voice Interaction

Voice remains **optional and secondary**.

It should simply translate voice into the existing Jark workflow.

Example:

```text
Voice Chat

      🎙

Listening...

"Find authorization issues
in this repository."

[ Stop ]
```

Voice should **not** become a separate engineering architecture.

---

# 38. Settings

Keep settings minimal.

### Agent

```text
Model
Selected Model

Temperature
0.2
```

### Safety

```text
Require Approval
ON

Maximum Repair Attempts
2
```

### General

```text
Theme
Dark

Notifications
ON
```

Avoid unnecessary configuration.

---

# 39. Notifications

Notifications should be action-oriented.

```text
✓ Investigation completed
3 findings require review.
```

```text
⚠ Approval required
Jark prepared a remediation plan.
```

```text
✓ Verification completed
412 / 412 tests passed.
```

```text
✕ Verification failed
Rollback completed successfully.
```

---

# 40. Responsive Design

## Desktop — Primary

Full workspace:

* Sidebar
* Explorer
* Agent trace
* Evidence
* Code
* Diff
* Logs

## Tablet

Collapse secondary panels.

## Mobile

Only expose useful monitoring workflows:

* Status
* Findings
* Approval
* Reports
* History

Do not attempt to reproduce the complete IDE workspace on mobile.

---

# 41. Accessibility

Required:

* Keyboard navigation
* Visible focus states
* Good contrast
* Semantic buttons
* Screen-reader labels
* Tooltips
* Text-based status indicators

Never communicate state only through color.

Use:

```text
✓ VERIFIED
```

rather than only a green dot.

---

# 42. Animation

Animation should communicate system activity.

Good uses:

* Agent progress
* Tool execution
* Build progress
* Test progress
* Status transitions
* Diff updates

Avoid:

* Decorative particles
* Excessive glow
* Constant movement
* Large transitions
* Marketing animations

The product should feel **fast and technical**.

---

# 43. Loading States

Never show a blank screen.

Example:

```text
Building Repository Intelligence

✓ Reading repository
✓ Parsing Java files
● Building dependency graph
○ Preparing RAG index

Preparing Agent...
```

---

# 44. Empty States

Example:

```text
No investigations yet.

Ask Jark about your repository
or start an issue investigation.

[ Start Investigation ]
```

---

# 45. Error UX

Every error should answer:

1. What happened?
2. Why?
3. What did Jark do?
4. What can the user do next?

Example:

```text
Build Failed

Maven compilation failed in
PaymentService.java.

Jark did not accept the change.

[ View Build Log ]
[ View Diff ]
[ Retry Repair ]
[ Rollback ]
```

---

# 46. Trust UX Rules

These are mandatory.

### Rule 1

Never represent generated code as verified code.

### Rule 2

Never hide failed verification.

### Rule 3

Never hide important agent tool activity.

### Rule 4

Never silently modify code.

### Rule 5

Provide evidence for important findings.

### Rule 6

Clearly distinguish:

```text
Generated
    ↓
Validated
    ↓
Verified
```

---

# 47. Core UX Flow

```text
┌───────────────┐
│    Landing    │
└───────┬───────┘
        ↓
┌──────────────────┐
│ Connect Repository│
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Repository       │
│ Overview         │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Chat / Q&A       │
└────────┬─────────┘
         ↓
┌────────────────────────┐
│ Investigation Workspace│
│ AST + Graph + RAG      │
│ Agent Trace             │
└────────┬───────────────┘
         ↓
┌──────────────────┐
│ Evidence / Finding│
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Engineering Plan │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Human Approval   │
└────────┬─────────┘
         ↓
┌────────────────────────┐
│ IDE-like Agent Action  │
│ Live Modification      │
└────────┬───────────────┘
         ↓
┌──────────────────┐
│ Build / Tests    │
│ Verification     │
└────────┬─────────┘
         ↓
    ┌────┴────┐
    ↓         ↓
 Success    Failure
    ↓         ↓
 Report    Repair
              ↓
           Retry
              ↓
       ┌──────┴──────┐
       ↓             ↓
    Success       Failure
       ↓             ↓
     Report       Rollback
       └──────┬──────┘
              ↓
          History
```

---

# 48. Screen Priority for the One-Year Build

Because Jark is a **one-year complete product**, implementation effort should be prioritized.

## 🔴 Tier 1 — Perfect These

1. **Repository Overview**
2. **Investigation Workspace**
3. **Agent Trace**
4. **Finding / Evidence**
5. **Engineering Plan**
6. **Approval**
7. **IDE-like Agent Modification**
8. **Verification**
9. **Diff**
10. **Engineering Report**

These screens demonstrate the actual product thesis.

## 🟡 Tier 2 — Build Normally

* Landing
* Repository Connection
* Repository Q&A
* History
* Settings

## 🟢 Tier 3 — Keep Lightweight

* Voice
* Mobile
* Advanced notifications

Do **not** sacrifice the core agent experience to over-design secondary features.

---

# 49. What Makes Jark UX Different

A normal AI coding interface:

```text
User
 ↓
Chat
 ↓
AI
 ↓
Code
```

Jark:

```text
User
 ↓
Repository
 ↓
Agent Investigation
 ↓
AST + Dependency Graph + RAG
 ↓
Evidence
 ↓
Finding Validation
 ↓
Engineering Plan
 ↓
Human Approval
 ↓
Live Modification
 ↓
Build + Tests
 ↓
Verification
 ↓
Report
```

The UI should make this difference immediately obvious.

---

# 50. Final Visual Identity

Jark should feel:

> **Dark + Technical + Premium + Minimal + Neon Green + Developer-native + Transparent + Trustworthy**

It should resemble a serious engineering environment rather than a consumer AI application.

---

# 51. Final Design Statement

The final Jark UX should communicate:

> **"Watch an engineering agent understand your Java/Spring repository, investigate an issue using structural and semantic evidence, propose a bounded change, let you approve it, modify the code live, and prove whether the resulting system actually works."**

That is the core experience.

---

# 🔒 FINAL UI/UX LOCK

**Product:** Jark
**Full Name:** **Jark — Agentic Java/Spring Code Review & Remediation Agent**

### Visual Identity

**Near-black + Neon Green**

### Primary Workflow

**Understand → Investigate → Evidence → Plan → Approve → Act → Verify → Report**

### Primary Experience

**Agent Investigation + IDE-like Live Modification + Verification**

### Primary Trust Mechanisms

**Visible evidence + visible agent trace + human approval + executable verification**

### Primary Design Goal

> **Make Jark's engineering process visible, understandable, controllable, and verifiable without overwhelming the developer.**

This is the **final design document to lock alongside your PRD and Technical Design Document**.
