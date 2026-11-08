# Issue tracker: GitHub

Issues and specs live in GitHub Issues for `DavidHLP/BossAnalyze`.
Use the `gh` CLI with `--repo DavidHLP/BossAnalyze`.

## Operations

- Create: `gh issue create --repo DavidHLP/BossAnalyze --title "..." --body-file <file>`
- Read: `gh issue view <number> --repo DavidHLP/BossAnalyze --comments`
- List: `gh issue list --repo DavidHLP/BossAnalyze --state open --json number,title,body,labels,comments`
- Comment: `gh issue comment <number> --repo DavidHLP/BossAnalyze --body-file <file>`
- Label: `gh issue edit <number> --repo DavidHLP/BossAnalyze --add-label "..."` or `--remove-label "..."`
- Close: `gh issue close <number> --repo DavidHLP/BossAnalyze --comment "..."`

Use temporary UTF-8 files for multiline bodies. Check for existing
related issues before creating another.

When a skill says "publish to the issue tracker", create a GitHub issue.
When it says "fetch the relevant ticket", read the issue and comments.

## Pull requests as a triage surface

**PRs as a request surface: no.**

GitHub issues and PRs share a number space. If the object type is unclear,
resolve it before applying issue operations.

## Wayfinding operations

Use one issue labelled `wayfinder:map` for Notes, Decisions-so-far, and Fog.
Create child issues labelled `wayfinder:<type>` where type is `research`,
`prototype`, `grilling`, or `task`.

Link children as GitHub sub-issues. If unavailable, use a task list on
the map and `Part of #<map>` in each child.

Represent blockers with native issue dependencies, using numeric database
IDs rather than issue numbers. If unavailable, write `Blocked by: #<n>`
in the child. A ticket is available only when all blockers are closed.

Select the first open, unassigned, unblocked child in map order. Claim
it by assigning yourself before other ticket writes. Resolve it with an
answer comment, close it, and append the result and a context link to
the map's Decisions-so-far.
