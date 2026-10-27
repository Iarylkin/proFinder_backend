# Wiki ⇄ docs/ synchronization

The [`docs/`](../docs) folder and the
[GitHub Wiki](https://github.com/Strogolsky/proFinder_backend/wiki) are kept in
sync **both ways** by [`.github/workflows/wiki-sync.yml`](workflows/wiki-sync.yml),
which uses [`newrelic/wiki-sync-action`](https://github.com/newrelic/wiki-sync-action).

| Trigger | Direction |
| --- | --- |
| Push to `develop` that touches `docs/**` | `docs/` → Wiki |
| Wiki page edited in the GitHub UI (`gollum` event) | Wiki → `docs/` (commit to `develop`) |

Edit either side — a pull request against `docs/`, or the Wiki pages in the
browser — and the other side updates within a minute.

> Only create/update propagates. A page **deleted** on the Wiki is not deleted in
> `docs/` (GitHub's `gollum` event does not carry deletions) — remove the file in
> `docs/` via a PR.

## File / page naming

The action copies files 1:1 — it does **not** rename files or rewrite links.

* `Home.md` is the Wiki landing page — keep it.
* `_Sidebar.md` / `_Footer.md` render as the Wiki sidebar / footer.
* `1 - Description.md` becomes the page **"1 - Description"**. Cross-links use the
  URL-encoded relative form `[Description](1%20-%20Description.md)` — literally
  `%20` for each space. Heading anchors (`#some-heading`) follow GitHub's slug
  rules and work on both sides.
* Keep `docs/` **flat** — subfolders get flattened into the page name.
* `docs/README.md` also becomes a Wiki page ("README"); keep it short.

## Content notes (migrated from Obsidian)

* File names kept as `N - Title.md` so the existing cross-links keep working. No
  Obsidian `[[wikilinks]]` or `![[embeds]]` are used.
* No YAML frontmatter.
* Fenced `mermaid` blocks (files 3, 4, 5, 6, 10, 15) render natively on GitHub in
  both the repo and the Wiki.
* Excalidraw files (`*.excalidraw.md`) are **excluded** — they only render inside
  Obsidian. Export needed diagrams to SVG/PNG under `docs/img/` and embed with a
  standard Markdown image tag.

## One-time setup

1. **Initialize the Wiki** — create any page in the GitHub Wiki UI once, so the
   `<repo>.wiki.git` repository exists.
2. **Add the `WIKI_ACTION_TOKEN` secret** — a Personal Access Token with write
   access to this repository (classic PAT with `repo` scope, or a fine-grained PAT
   with *Contents: read and write*). The default `GITHUB_TOKEN` is not used: pushes
   made with it do not trigger other workflows, and it cannot push wiki→repo on the
   `gollum` event.
3. The workflow file must be on the repository's **default branch** (`develop`) for
   the `gollum` trigger to fire.
