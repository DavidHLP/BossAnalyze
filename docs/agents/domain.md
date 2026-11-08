# Domain Docs

## Layout

Use a single-context layout:

- `CONTEXT.md` at the repository root: domain vocabulary and context.
- `docs/adr/`: architecture decision records.

## Before exploring

Read `CONTEXT.md` and ADRs relevant to the work.

If these files do not exist, proceed silently. Do not propose creating
them upfront. The domain-modeling skill creates them when domain terms
or decisions are resolved.

If a root `CONTEXT-MAP.md` is introduced later, follow its links to the
contexts relevant to the task.

## Vocabulary

Use the terms defined in `CONTEXT.md` in issue titles, proposals,
hypotheses, and tests. Avoid synonyms the glossary explicitly rejects.
Reconsider unfamiliar terminology or note genuine gaps for domain-modeling.

## Decision conflicts

Explicitly identify any proposal that contradicts an ADR. Name the ADR
and explain why the decision should be reconsidered.
