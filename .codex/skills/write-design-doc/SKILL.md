---
name: write-design-doc
description: Use when creating, editing, or reviewing piku-back design documents, architecture notes, feature design docs, handoff docs, migration plans, rollout plans, or task-based planning documents.
---

# Write Design Doc

Use this skill for `piku-back` design documentation work only.

## Core Rules

- Do not insert source code, pseudocode, configuration snippets, SQL, shell commands, or API implementation examples into the design document.
- Write design documents in Korean.
- If the design request is ambiguous, ask clarifying questions until the feature scope, architecture direction, and intended document outcome are clear enough to write.
- Focus on feature behavior, domain responsibilities, architecture flow, data ownership, API contract intent, and operational impact.
- Explain architecture with layers, ports, adapters, domain boundaries, and request or event flow rather than implementation details.
- For implementation, migration, rollout, or work plans, include explicit task sections with checkbox steps while keeping all steps prose-only.
- Respect DDD + hexagonal boundaries from `docs/architecture/ddd-hexagonal-architecture.md`.
- Place design documents under `docs/superpowers/plans`.
- Place official documents under the appropriate `docs/` category and update the matching index when creating a new official document.
- Include a required `Commit Message` section in every completed design document.
- Write the commit message in Korean and follow `docs/standards/engineering-workflow.md`: `<type>: <description>`, no scope, no file-name list, no internal phase labels.
- Base the `Commit Message` on the intended product or engineering work described by the document, not on the act of creating or editing the document. Use `docs:` only when the actual work is documentation-only maintenance.

## Document Shape

Use only sections that fit the request, but prefer this order:

1. `# Title`
2. Purpose and non-goals
3. Current behavior or context
4. Proposed behavior
5. Architecture flow
6. Domain and ownership boundaries
7. API or client contract intent, if relevant
8. Operational, migration, and compatibility notes
9. Task plan, if the document is an implementation, migration, rollout, or work plan
10. Risks and open questions
11. Commit Message

## Task Plan Guidance

When the document is an implementation plan, migration plan, rollout plan, or work plan, include explicit task sections.

Task sections should:

- Use headings like `### Task 1: ...`.
- Use checkbox steps with `- [ ]`.
- Name the intended files, layers, ports, adapters, tests, and documents to change.
- Describe the behavioral intent and verification goal of each task.
- Keep each task small enough to review independently.
- Identify the owning domain and any boundary crossed by the task.

Task sections must not include source code, pseudocode, SQL, shell commands, API payload examples, or implementation snippets. Describe what must change and what must be verified in prose.

## Architecture Flow Guidance

- Describe the flow as actors and responsibilities, for example client, controller, use case, domain model, port, adapter, external system.
- Name the owning domain and any cross-context dependency.
- Prefer intention-revealing names for capabilities and boundaries.
- For API errors, describe the Problem Details direction and compatibility constraints rather than creating custom response shapes.
- If a detailed sequence is needed, use prose or Mermaid diagrams without embedding code-like payloads.

## Final Check

Before presenting or saving the design document, verify:

- No code-like blocks or implementation snippets are present.
- The document explains feature and architecture flow clearly enough for implementation planning.
- DDD + hexagonal architecture boundaries are explicit where relevant.
- If the document is an implementation, migration, rollout, or work plan, explicit `Task` sections with checkbox steps are present.
- A Korean `Commit Message` section is present.
- The design document is placed under `docs/superpowers/plans`.
