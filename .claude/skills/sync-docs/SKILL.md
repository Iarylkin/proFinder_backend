---
name: sync-docs
description: Reconcile docs/ with the current branch's code changes - find real discrepancies (not just untouched files), skip already-documented gaps, get the user's rationale for genuine drift, and update the doc plus its changelog entry accordingly.
---

# Sync docs with code

Compares the current branch against `main`, finds every changed file, and for
each one checks whether `docs/` still accurately describes what the code now
does. Code is the source of truth during active rewrites - docs and their
changelog get updated to match, but only with the user's explanation of *why*
the implementation ended up this way, since that reasoning isn't inferable
from the diff alone.

## Step 1 - Enumerate every changed file, not a filtered subset

Do not diff against `main`/`origin/main` by default - on this repo `main` can
sit dozens of commits behind the current branch (stale integration branch),
so a `merge-base` diff against it drags in a pile of already-merged, already-
documented history that has nothing to do with the current branch's actual
work. Scope to the current branch's own commits instead:

```bash
BASE=$(git log --merges -1 --format=%H HEAD)   # nearest merge-commit ancestor
git diff --name-only "$BASE"...HEAD   # committed on this branch, since that merge
git diff --name-only                  # unstaged
git diff --cached --name-only         # staged
```

The nearest merge commit reachable from `HEAD` (typically the last "Merge
pull request" commit) marks where already-integrated history ends and this
branch's own new commits begin - use that as `BASE`. Only fall back to
`git merge-base main HEAD` (or `origin/main`) if `HEAD` has no merge-commit
ancestor at all (e.g. a very short-lived repo/branch). Always include the
unstaged and staged diffs regardless of which `BASE` was used - in-progress
work isn't committed yet but is still in scope.

If the resulting file list still looks implausibly large or unrelated to the
branch's apparent purpose (branch name, recent commit subjects), stop and
confirm the intended scope with the user before proceeding, rather than
silently reconciling docs against unrelated history.

Union all three lists. Drop only files that cannot possibly be described in
`docs/`: build output (`target/`), compiled classes, lock files, generated
resources. Everything else - Java code, tests, config, SQL, workflow YAML -
stays in scope. Do not pre-filter by "looks like domain code": that heuristic
misses files whose name doesn't map cleanly to a doc keyword.

Track the list as a checklist (e.g. a todo list) and work through it file by
file until every entry is marked reviewed. "Unrelated to any doc" is a valid
outcome, but it must be an explicit conclusion for that file, not a silent
skip because nothing obvious matched.

## Step 2 - For each file, find what docs/ should say about it

1. Derive a search keyword from the file (the leading CamelCase word of a
   class name, e.g. `OrderProposalService.java` -> `Order`).
2. `grep -li` that keyword across `docs/*.md` to find candidate sections -
   this is a starting point, not a filter.
3. Independently check the structural docs that enumerate things regardless
   of keyword match: `7 - Application Classes.md` (every class should be
   listed), `8 - API Specification.md` (every endpoint), `6 - Database Schema.md`
   (every entity/column), `5 - State Machines.md` (every status transition),
   `13 - Validation Rules.md` (every constraint). If a changed file plausibly
   belongs in one of these and isn't reflected there, that is a finding too.
4. Read the actual current doc section and the actual current code, not just
   the diff hunk - a diff can look fine in isolation while the method it
   lives in now contradicts the doc.

## Step 3 - Filter out already-known gaps

Check the discrepancy against the gap table in `.claude/CLAUDE.md` (deployment
unit, async/events, `Client`/`Specialist` vs `Customer`/`Professional`, schema
management). If it is exactly one of these documented transitional gaps, mark
the file reviewed with no action - it is already explained there.

## Step 4 - For every remaining discrepancy, ask for the rationale

Use `AskUserQuestion` (batch up to 4 at a time, one question per discrepancy):

> Docs say **[X]** (`docs/N - Title.md`), code now does **[Y]** (`Class.java:LINE`).
> Why, and what should happen?

Options:
- A best-guess rationale, drawn from the code, its comments, or the commit
  message
- A second best-guess rationale, if another plausible reason exists
- "This is a bug - fix the code, don't touch docs"
- (the user can always type a different explanation via "Other")

## Step 5 - Apply the answer

- **A rationale was given** (a drafted option or free text): edit the
  relevant `docs/N - Title.md` section to match current code behavior, then
  add a row to that file's `## Changelog` table following its own existing
  format - bump `Version`, use today's date (`date +%F`), and write the
  `Change` cell as what changed **and why**, matching the concrete,
  cross-referenced style already used in that file's changelog. Do not
  invent a metadata/changelog block for a doc that doesn't already have one.
- **"It's a bug"**: do not touch docs. Record it for the final summary
  instead - this skill never edits code on the user's behalf.
- Known gaps from Step 3: no action, already recorded as reviewed.

## Step 6 - Final summary

Report, grouped:
- Docs updated (file + changelog line added)
- Known gaps skipped (file + which CLAUDE.md gap explains it)
- Flagged as code bugs, not yet fixed (file + what's wrong)
