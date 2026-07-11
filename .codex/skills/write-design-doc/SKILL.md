---
name: write-design-doc
description: Use when planning piku-back work before implementation and creating or editing the corresponding design, implementation, migration, rollout, or task planning document; not for general documentation work unrelated to pre-implementation planning.
---

# Write Design Doc

Use this skill for `piku-back` planning documentation work before implementation begins.

Do not use this skill for general documentation work that is not part of pre-implementation design or planning, such as README updates, guides, meeting minutes, or routine documentation maintenance.

Do not use this skill for code review, document review, post-implementation review, or retrospective assessment.

## Core Rules

- Do not insert source code, pseudocode, configuration snippets, SQL, shell commands, or API implementation examples into the design document.
- Write design documents in Korean.
- If the design request is ambiguous, ask clarifying questions until the feature scope, architecture direction, and intended document outcome are clear enough to write.
- Focus on feature behavior, domain responsibilities, architecture flow, data ownership, API contract intent, and operational impact.
- Explain architecture with layers, ports, adapters, domain boundaries, and request or event flow rather than implementation details.
- For implementation, migration, rollout, or work plans, include explicit task sections with checkbox steps while keeping all steps prose-only.
- Respect DDD + hexagonal boundaries from `docs/architecture/ddd-hexagonal-architecture.md`.
- Place working design and planning documents under `docs/superpowers/plans`.
- Place official documents under the appropriate `docs/` category and update the matching index when creating a new official document.
- Treat documents with explicit implementation, migration, rollout, or work tasks as work documents. Include a `Commit Message` section in these documents only when they are under `docs/superpowers/`.
- Do not include a `Commit Message` section in planning-only documents without concrete work tasks.
- Never include a `Commit Message` section in files outside `docs/superpowers/`, including architecture or domain records, product specifications, handoffs, runbooks, incident retrospectives, and other official documentation.
- When a `Commit Message` section is included, write it in Korean and follow `docs/standards/engineering-workflow.md`: `<type>: <description>`, no scope, no file-name list, no internal phase labels.
- Base an included `Commit Message` on the intended product or engineering work described by the document, not on the act of creating or editing the document. Use `docs:` only when the actual work is documentation-only maintenance.

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
11. Commit Message, only for a work document under `docs/superpowers/`

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
- If the document is a work document under `docs/superpowers/`, a Korean `Commit Message` section is present.
- If the document is planning-only or outside `docs/superpowers/`, no `Commit Message` section is present.
- A working design or planning document is placed under `docs/superpowers/plans`; an official document is placed under the matching `docs/` category.
