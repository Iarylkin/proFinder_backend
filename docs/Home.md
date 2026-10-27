# ProFinder Backend — Documentation

> Landing page of the project [Wiki](https://github.com/Strogolsky/proFinder_backend/wiki).
> These pages live in [`docs/`](https://github.com/Strogolsky/proFinder_backend/tree/develop/docs)
> and are synced with the Wiki both ways — see
> [`.github/wiki-sync.md`](https://github.com/Strogolsky/proFinder_backend/blob/develop/.github/wiki-sync.md).
>
> Files 1–15 are the **target architecture**. Where the current code disagrees with
> them, these documents are the intended design.

## Documents

| # | Page | What it covers |
|---|---|---|
| 1 | [Description](1%20-%20Description.md) | One-page overview: what ProFinder is, who it serves, feature set |
| 2 | [Requirements](2%20-%20Requirements.md) | Functional / non-functional requirements, use cases |
| 3 | [System Design](3%20-%20System%20Design.md) | Architecture, services, cross-cutting concerns |
| 4 | [Business Logic](4%20-%20Business%20logic.md) | Domain rules, order lifecycle, moderation, tokens |
| 5 | [State Machines](5%20-%20State%20Machines.md) | Mermaid state diagrams (Order, Profile, Account, Report, …) |
| 6 | [Database Schema](6%20-%20Database%20Schema.md) | Tables, ER diagrams, enums, indexes |
| 7 | [Application Classes](7%20-%20Application%20Classes.md) | Entity / Repository / Service / Controller / DTO breakdown |
| 8 | [API Specification](8%20-%20API%20Specification.md) | Conventions, error catalog, all endpoints |
| 9 | [Event Catalog](9%20-%20Event%20Catalog.md) | Event envelope, queue topology, retry/DLQ, payloads |
| 10 | [Sequence Diagrams](10%20-%20Sequence%20Diagrams.md) | Mermaid `sequenceDiagram` flows |
| 11 | [Permission Matrix](11%20-%20Permission%20Matrix.md) | Role × action matrix, account-status gate, enforcement |
| 12 | [Elasticsearch Mapping](12%20-%20Elasticsearch%20Mapping.md) | Index mappings, analyzers, query shapes |
| 13 | [Validation Rules](13%20-%20Validation%20Rules.md) | Per-field constraint tables for every API input |
| 14 | [Category Taxonomy](14%20-%20Category%20Taxonomy.md) | Seed categories, migration inserts, rename/delete notes |
| 15 | [SDLC & Workflow](15%20-%20SDLC%20%26%20Workflow.md) | Repo layout, issue→prod workflow, environments, CI/CD, testing |

### Planning / internal

- [Documentation Roadmap](Documentation%20Roadmap.md) — what is still missing or undecided
- [tasks](tasks.md) — original design-task taxonomy (superseded, kept for reference)
